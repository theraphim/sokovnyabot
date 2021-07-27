import cats.instances.future._
import cats.syntax.functor._
import com.softwaremill.sttp._
import com.bot4s.telegram.future.Polling
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.methods.ParseMode
import com.bot4s.telegram.models.ChatType.Supergroup
import com.bot4s.telegram.models.{Chat, Message, User}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class Body(message: String)

import com.bot4s.telegram.api.AkkaTelegramBot
import com.bot4s.telegram.clients.AkkaHttpClient
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

abstract class AkkaExampleBot(val token: String) extends AkkaTelegramBot {
  override val client = new AkkaHttpClient(token)
}

object NameTables {
  val alesyaEng = Seq("lesya", "lesja", "lesia", "lesa", "lyesya", "lyesja", "lyesia", "lyesa", "ljesya", "ljesja", "ljesia", "ljesa").flatMap { s =>
    Seq(s"a$s", s"o$s")
  }

  val alesyaRu = Seq("олеся", "алеся", "леся")

  val alesya: Set[String] = (alesyaEng ++ alesyaRu).toSet
}


class SokovnyaBot(token: String, russianChats: Set[Long], covidChats: Set[Long]) extends AkkaExampleBot(token)
  with Polling
  with Commands[Future] {

  LoggerConfig.factory = PrintLoggerFactory()
  // set log level, e.g. to TRACE
  LoggerConfig.level = LogLevel.TRACE

  private[this] case class ChatProperties(isRussian: Boolean, isCovid: Boolean)

  private[this] case class UserGreetingPart(text: String, needCaps: Boolean)

  private[this] def getUserGreetingPart(user: User, chatProperties: ChatProperties): UserGreetingPart = {
    val suffix = user.lastName.map(x => s" $x").getOrElse("")
    val userMD = s"[${user.firstName}${suffix}](tg://user?id=${user.id})"
    val isAlesya = NameTables.alesya.contains(user.firstName.toLowerCase)

    chatProperties match {
      case ChatProperties(true, _) if isAlesya =>
        UserGreetingPart(s"Здравствуйте $userMD, теперь Вы здеся!", true)
      case ChatProperties(true, _) =>
        UserGreetingPart(s"Здравствуйте $userMD,", false)
      case _ =>
        UserGreetingPart(s"Greetings $userMD,", false)
    }
  }

  private[this] def getUserReplyText(user: User, chatProperties: ChatProperties): String = {
    val userGreetingPart = getUserGreetingPart(user, chatProperties)

    val tailPart = chatProperties match {
      case ChatProperties(true, false) =>
        "при входе надо надеть маску, представиться, рассказать о себе. Ковидня"
      case ChatProperties(true, true) =>
        "пожалуйста представьтесь, расскажите сколько вам лет, чем вы занимаетесь, и какую вам сделали прививку, а я всё аккуратно запишу. Ковидня"
      case ChatProperties(false, false) =>
        "please state your name, age, and occupation."
      case ChatProperties(false, true) =>
        "please state your name, age, occupation, and the name of the vaccine you have received."
    }

    val maybeCapitalized = if (userGreetingPart.needCaps) {
      tailPart.substring(0,1).toUpperCase() + tailPart.substring(1)
    } else tailPart

    userGreetingPart.text + " " + tailPart
  }

  private[this] def getChannelProperties(c: Chat) = {
    val sg = c.`type` == Supergroup
    ChatProperties(sg && russianChats.contains(c.id), sg && covidChats.contains(c.id))
  }

  private[this] def greetUser(user: User, cp: ChatProperties)(implicit msg: Message) = {
    reply(
      getUserReplyText(user, cp),
      parseMode = Some(ParseMode.Markdown),
      replyToMessageId = Some(msg.messageId),
    )
  }

  onCommand("preved") { implicit msg =>
    using(_.from) { user =>
      greetUser(user, ChatProperties(false, false)).map(_ => ())
    }
  }

  onMessage { implicit msg =>
    val cp = getChannelProperties(msg.chat)
    if (!cp.isRussian) {
      logger.info(s"non-russian chat: ${msg.chat}, text: ${msg.text}")
    }
    using(_.newChatMembers) { newChatMembers =>
      Future.sequence(newChatMembers.map(user => greetUser(user, cp)).toSeq).map(_ => ())
    }
  }

}

case class ServiceConfig(token: String, russianChats: List[Long], covidChats: List[Long])

import pureconfig._
import pureconfig.generic.auto._

object SokovnyaMain extends App {
  val conf = ConfigSource.default.load[ServiceConfig]
  conf match {
    case Left(value) =>
      println(value)
    case Right(config) =>
    val bot = new SokovnyaBot(config.token, config.russianChats.toSet, config.covidChats.toSet)
    val eol = bot.run()
    println("Press [ENTER] to shutdown the bot, it may take a few seconds...")
    scala.io.StdIn.readLine()
    println("Calling shutdown.")
    bot.shutdown() // initiate shutdown
    // Wait for the bot end-of-life
    println("Waiting for shut down.")
    Await.result(eol, Duration.Inf)
    println("Exiting.")
  }
}
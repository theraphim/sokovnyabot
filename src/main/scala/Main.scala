import cats.instances.future._
import cats.syntax.functor._
import com.softwaremill.sttp._
import com.bot4s.telegram.future.Polling
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.methods.ParseMode
import com.bot4s.telegram.models.ChatType.Supergroup
import com.bot4s.telegram.models.{Message, User}

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

class SokovnyaBot(token: String, russianChats: Set[Long]) extends AkkaExampleBot(token)
  with Polling
  with Commands[Future] {

  LoggerConfig.factory = PrintLoggerFactory()
  // set log level, e.g. to TRACE
  LoggerConfig.level = LogLevel.TRACE

  private[this] def greetUser(user: User, isRussian: Boolean)(implicit msg: Message) = {
    val suffix = user.lastName.map(x => s" $x").getOrElse("")
    val userMD = s"[${user.firstName}${suffix}](tg://user?id=${user.id})"
    val replyText = if (isRussian) {
      if (NameTables.alesya.contains(user.firstName.toLowerCase)) {
        s"Здравствуйте $userMD, теперь Вы здеся! При входе надо надеть маску, представиться, рассказать о себе, не отходить от чята дальше чем на 20км. Соковня"
      } else {
        s"Здравствуйте $userMD, при входе надо надеть маску, представиться, рассказать о себе, не отходить от чята дальше чем на 20км. Соковня"
      }
    } else {
      s"Greetings $userMD, please state your name, age, and occupation."
    }
    reply(
      replyText,
      parseMode = Some(ParseMode.Markdown),
      replyToMessageId = Some(msg.messageId),
    )
  }

  onCommand("preved") { implicit msg =>
    using(_.from) { user =>
      greetUser(user, false).map(_ => ())
    }
  }

  onMessage { implicit msg =>
    val isCyrillic = (msg.chat.`type` == Supergroup) && russianChats.contains(msg.chat.id)
    if (!isCyrillic) {
      logger.info(s"non-russian chat: ${msg.chat}, text: ${msg.text}")
    }
    using(_.newChatMembers) { newChatMembers =>
      Future.sequence(newChatMembers.map(user => greetUser(user, isCyrillic)).toSeq).map(_ => ())
    }
  }

}

case class ServiceConfig(token: String, russianChats: List[Long])

import pureconfig._
import pureconfig.generic.auto._

object SokovnyaMain extends App {
  ConfigSource.default.load[ServiceConfig].foreach { config =>
    val bot = new SokovnyaBot(config.token, config.russianChats.toSet)
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
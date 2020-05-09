import cats.instances.future._
import cats.syntax.functor._
import com.softwaremill.sttp._
import com.bot4s.telegram.future.Polling
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.methods.ParseMode

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class Body(message: String)

import com.bot4s.telegram.api.AkkaTelegramBot
import com.bot4s.telegram.clients.AkkaHttpClient
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

abstract class AkkaExampleBot(val token: String) extends AkkaTelegramBot {
  LoggerConfig.factory = PrintLoggerFactory()
  // set log level, e.g. to TRACE
  LoggerConfig.level = LogLevel.TRACE

  override val client = new AkkaHttpClient(token)
}

object NameTables {
  val alesyaEng = Seq("lesya", "lesja", "lesia", "lesa", "lyesya", "lyesja", "lyesia", "lyesa", "ljesya", "ljesja", "ljesia", "ljesa").flatMap { s =>
    Seq(s"a$s", s"o$s")
  }

  val alesyaRu = Seq("олеся", "алеся")

  val alesya: Set[String] = (alesyaEng ++ alesyaRu).toSet
}

class SokovnyaBot(token: String) extends AkkaExampleBot(token)
  with Polling
  with Commands[Future] {

  onMessage { implicit msg =>
    using(_.newChatMembers) { newChatMembers =>
      for (user <- newChatMembers) {
        val suffix = user.lastName.map(x => s" $x").getOrElse("")
        val replyText = if (NameTables.alesya.contains(user.firstName.toLowerCase)) {
          s"Здравствуйте [${user.firstName}${suffix}](tg://user?id=${user.id}), теперь Вы здеся! При входе надо представиться и рассказать о себе. Соковня"
        } else {
          s"Здравствуйте [${user.firstName}${suffix}](tg://user?id=${user.id}), при входе надо представиться и рассказать о себе. Соковня"
        }
        reply(
          replyText,
          parseMode = Some(ParseMode.Markdown),
          replyToMessageId = Some(msg.messageId),
        )
      }
      Future.unit
    }
  }
}

case class ServiceConfig(token: String)

import pureconfig._
import pureconfig.generic.auto._

object SokovnyaMain extends App {
  ConfigSource.default.load[ServiceConfig].foreach { config =>
    val bot = new SokovnyaBot(config.token)
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
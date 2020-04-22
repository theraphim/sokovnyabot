import cats.instances.future._
import cats.syntax.functor._
import com.softwaremill.sttp._
import com.bot4s.telegram.future.Polling
import com.bot4s.telegram.api.declarative.Commands

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

class SokovnyaBot(token: String) extends AkkaExampleBot(token)
  with Polling
  with Commands[Future] {

  onMessage { implicit msg =>
    using(_.newChatMembers) { newChatMembers =>
      for (user <- newChatMembers) {
        val suffix = user.lastName.map(x => s" $x").getOrElse("")
        reply(s"Здравствуйте ${user.firstName}${suffix}, при входе надо представиться и рассказать о себе. Соковня")
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
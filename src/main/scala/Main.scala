import eventsrc.{BlackBoxPath, EventFromBlackBox}
import zio.*
import zio.logging.*

object Main extends App:
  val live = Logging.console()

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    (for {
      _ <- console.putStrLn("ZivergeTechBoxer - spinning up .. ")
      _ <- EventFromBlackBox.spinUpBuffer(BlackBoxPath(args.headOption.getOrElse("/Users/andersbohn/Downloads/blackbox.macosx")))// FIXME replace with some nice zio-/zlayer-config?! 

    } yield ()).provideSomeLayer[ZEnv](live).exitCode

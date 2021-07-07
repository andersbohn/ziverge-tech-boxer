import zio.*
import zio.logging.*

object Main extends App:
  val live = Logging.console()

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    (for {
      _ <- console.putStrLn("ZivergeTechBoxer - spinning up .. ")

    } yield ()).provideSomeLayer[ZEnv](live).exitCode

import eventsrc.{BlackBoxPath, RawEventInputStream, EventFromBlackBox, EventsFromInputStreamImpl, Eventsrc}
import rest.Endpoints
import zio.*
import zio.logging.*
import zio.console.*
import zio.blocking.*
import zhttp.http.*
import zhttp.service.Server

object Main extends App:
  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    val execFileName = args.headOption.getOrElse("/Users/andersbohn/Downloads/blackbox.macosx")
    val port         = args.tails.flatMap(_.headOption.flatMap(_.headOption.map(_.toInt))).nextOption().getOrElse(8080)
    val needed       = Logging.console() ++ Blocking.live ++ Console.live
    val layer  = (needed ++ ZLayer.succeed(BlackBoxPath(execFileName))) >>>  Eventsrc.liveBbox >>> Eventsrc.liveZstream
    val layers       =  needed ++ layer
    (for {
      _ <- console.putStrLn("ZivergeTechBoxer - spinning up .. ")
      fork <- eventsrc.streamEm.fork
      _ <- console.putStrLn(s"stats $fork")
      _ <- Server.start(port, Endpoints.routes)
    } yield ()).provideSomeLayer[ZEnv](layers).exitCode

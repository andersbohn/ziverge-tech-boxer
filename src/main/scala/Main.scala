import domain.Stats
import eventsrc.{ BlackBoxPath, EventFromBlackBox, EventsFromInputStreamImpl, Eventsrc, RawEventInputStream }
import rest.Endpoints
import zio.*
import zio.logging.*
import zio.console.*
import zio.blocking.*
import zhttp.http.*
import zhttp.service.Server
import zio.stm.*

object Main extends App:
  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    val pa      = CmdArgs.parseArgs(args)
    val needed  = Logging.console() ++ Blocking.live ++ Console.live
    val isLayer = pa.execFileStreamOrInput match {
      case Left(execFileName) =>
        (needed ++ ZLayer.succeed(BlackBoxPath(execFileName))) >>> Eventsrc.liveBbox >>> Eventsrc.liveZstream
      case Right(doit)        =>
        needed ++ Eventsrc.consoleIn >>> Eventsrc.liveZstream
    }
    val layers  = needed ++ isLayer
    (for {
      _        <- console.putStrLn("ZivergeTechBoxer - spinning up .. ")
      statCnts <- STM.atomically(TRef.make(Stats.zero))
      fiber    <- eventsrc.streamEm(statCnts).fork
      _        <- console.putStrLn(s"stats $fiber")
      _        <- Server.start(pa.port, Endpoints.routes(statCnts))
    } yield ()).provideSomeLayer[ZEnv](layers).exitCode

case class CmdArgs(execFileStreamOrInput: Either[String, Unit], port: Int)
object CmdArgs {
  def empty = new CmdArgs(Right(()), 8080)
  val Help  = """use -i <path-to-exe-file> or --port to override port 8080 as target"""

  def parseArgs(args: List[String]) = args.sliding(2, 2).foldLeft(CmdArgs.empty) { case (accArgs, arg) =>
    arg match {
      case "-i" :: execFile :: Nil       => accArgs.copy(execFileStreamOrInput = Left(execFile))
      case "--port" :: aPort :: Nil      => accArgs.copy(port = aPort.toInt)
      case "-h" :: Nil | "--help" :: Nil => println(CmdArgs.Help); sys.exit(1)
      case unknownArg                    => println(s"unknown arg $unknownArg -> ${CmdArgs.Help} "); sys.exit(1)
    }
  }
}

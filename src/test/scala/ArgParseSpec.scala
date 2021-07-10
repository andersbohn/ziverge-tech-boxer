import domain.*
import eventsrc.{ BlackBoxPath, EventsFromInputStreamImpl, Eventsrc, RawEventInputStream }
import zio.*
import zio.console.*
import zio.duration.*
import zio.json.*
import zio.logging.*
import zio.stm.*
import zio.test.*
import zio.test.Assertion.{ isGreaterThanEqualTo, * }
import zio.test.environment.TestClock

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ArgParseSpec extends DefaultRunnableSpec {

  val Inp1 = List(
    "-i",
    "/Users/andersbohn/Downloads/blackbox.macosx",
    "--windowSeconds",
    "10",
    "--port",
    "8090",
    "--windowCount",
    "100"
  )

  override def spec =
    suite("domain.MiniSpec")(testM(" check simple json parser ") {
      for {
        args1 <- ZIO.effect(CmdArgs.parseArgs(Inp1))
      } yield assert(args1.cfgs.windowSeconds)(equalTo(10)) &&
        assert(args1.cfgs.windowCount)(equalTo(100))
    })
}

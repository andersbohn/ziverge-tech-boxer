package domain

import zio.*
import zio.blocking.Blocking
import zio.test.*
import zio.test.Assertion.*
import zio.logging.*
import zio.console.*
import zio.json.*
import zio.stm.*
import zio.clock.*
import domain.*
import java.util.concurrent.TimeUnit
import zio.clock.currentTime
import zio.duration.*
import zio.test.Assertion.isGreaterThanEqualTo
import zio.test.*
import zio.test.environment.TestClock
import eventsrc.{ BlackBoxPath, EventsFromInputStreamImpl, Eventsrc, RawEventInputStream }

import java.time.LocalDateTime

object MiniSpec extends DefaultRunnableSpec {

  val OneRaw = """{ "event_type": "bar", "data": "dolor", "timestamp": 1625674980 }"""

  val port   = 8080
  val layers =
    ZLayer.succeed(RawEventInputStream(getClass.getResourceAsStream("/sample1.json"))) >>> Eventsrc.liveZstream

  import zio.json.JsonCodec.apply

  override def spec =
    suite("domain.MiniSpec")(
      testM(" check simple json parser ") {
        (for {
          raw   <- EventRaw.parseJson(OneRaw)
          event <- Task.fromEither(Event.fromRaw(raw))
        } yield assert("bar")(equalTo(event.eventType)) &&
          assert("dolor")(equalTo(event.data)) &&
          assert(LocalDateTime.of(2021, 7, 7, 16, 23))(equalTo(event.timestamp)))
      },
      testM(" validate update stats fun ") {
        (for {
          stats1 <- Task.succeed(Stats(1, 2, Map("a" -> 3, "b" -> 4)))
          stats3  = stats1.updateWith(Stats.one(Right(Event("a", "tehadata", LocalDateTime.now))))
          stats2  = stats3.updateWith(Stats.one(Left(new RuntimeException("bad!"))))
        } yield assert(stats2.wordCount)(equalTo(Map("a" -> 4, "b" -> 4))) &&
          assert(3)(equalTo(stats2.errorCount)))
      },
      testM(" read a json file stream ") {
        (for {
          statCnts     <- STM.atomically(TRef.make(Stats.zero))
          cnt          <- eventsrc.streamEm(statCnts)
          _            <- TestClock.adjust(120.seconds) // TODO not sure why testtime hasn't made things be processed by the strem
          statsUpdated <- STM.atomically(statCnts.get)
        } yield assert(1)(equalTo(cnt)) &&
          assert(0)(equalTo(statsUpdated.eventCount)) &&
          assert(0)(equalTo(statsUpdated.errorCount)))
      }.provideCustomLayer(layers)
    )
}

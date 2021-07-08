package domain

import zio.*
import zio.blocking.Blocking
import zio.test.*
import zio.test.Assertion.*
import zio.console.*
import zio.json.*
import domain.*
import eventsrc.{ EventsFromInputStreamImpl, Eventsrc, RawEventInputStream }

import java.time.LocalDateTime

object MiniSpec extends DefaultRunnableSpec {

  val OneRaw = """{ "event_type": "bar", "data": "dolor", "timestamp": 1625674980 }"""

//  val layers = Blocking.live ++ ZLayer.succeed(EventFile(ZInputStream.fromInputStream(getClass.getResourceAsStream("/sample1.json")))) >>> Eventsrc.liveZstream
  val layers = Blocking.live

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
      }
//      ,
//      testM(" read a json file stream ") {
//        (for {
//          fork <- eventsrc.streamEm.fork
//          fromFile   <- EventsFromInputStreamImpl.eventFileService(RawEventInputStream(getClass.getResourceAsStream("/sample1.json"))).eventStream.take(5).runCount
//        } yield assert(5)(equalTo(fromFile)))
//      }.provideSomeLayer[ZEnv](layers)
    )
}

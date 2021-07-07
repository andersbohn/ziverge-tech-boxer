package domain

import zio.*
import zio.blocking.Blocking
import zio.test.*
import zio.test.Assertion.*
import zio.json.*
import domain.*
import eventsrc.{EventFile, Eventsrc}

import java.time.LocalDateTime

object MiniSpec extends DefaultRunnableSpec {

  val OneRaw = """{ "event_type": "bar", "data": "dolor", "timestamp": 1625674980 }"""

  val layers = Blocking.live ++ ZLayer.succeed(EventFile(ZInputStream.fromInputStream(getClass.getResourceAsStream("/sample1.json")))) >>> Eventsrc.liveZstream

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
      testM(" read a json file stream ") {
        (for {
          fromFile   <- eventsrc.eventStream
        } yield assert("bar")(equalTo(fromFile.head.eventType))
          )
      }.provideSomeLayer[ZEnv](layers)
    )
}

package domain

import zio.*
import zio.blocking.Blocking
import zio.test.*
import zio.test.Assertion.*
import zio.json.*
import domain.*

import java.time.LocalDateTime

object MiniSpec extends DefaultRunnableSpec {

  val OneRaw = """{ "event_type": "bar", "data": "dolor", "timestamp": 1625674980 }"""

  import zio.json.JsonCodec.apply

  override def spec =
    suite("domain.MiniSpec") {
      testM(" check simple json parser ") {
        (for {
          raw   <- EventRaw.parseJson(OneRaw)
          event <- Task.fromEither(Event.fromRaw(raw))
        } yield assert("bar")(equalTo(event.eventType)) &&
          assert("dolor")(equalTo(event.data)) &&
          assert(LocalDateTime.of(2021, 7, 7, 16, 23))(equalTo(event.timestamp)))
      } //.provideSomeLayer(layers)
    }
}

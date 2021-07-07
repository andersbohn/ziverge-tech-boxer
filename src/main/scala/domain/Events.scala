package domain

import java.time.LocalDateTime
import java.time.*
import scala.util.Try
import zio.*
import zio.json.*

case class EventRaw(event_type: String, data: String, timestamp: Long)

object EventRaw {
  implicit val decoder: JsonDecoder[EventRaw] = DeriveJsonDecoder.gen[EventRaw]

  def parseJson(jsonStr: String): Task[EventRaw] =
    Task.fromEither(jsonStr.fromJson[EventRaw].left.map(s => new RuntimeException(s"parser error '$s''")))

}

case class Event(eventType: String, data: String, timestamp: LocalDateTime)

object Event:
  def fromRaw(eventRaw: EventRaw): Either[Throwable, Event] = Try(
    Event(eventRaw.event_type, eventRaw.data, LocalDateTime.ofEpochSecond(eventRaw.timestamp, 0, ZoneOffset.UTC))
  ).toEither

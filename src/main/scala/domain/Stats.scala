package domain

import zio.*
import zio.json.*

import java.time.{ LocalDateTime, * }
import scala.util.Try

case class Stats(eventCount: Long, errorCount: Long, wordCount: Map[String, Long]) {
  def updateWith(stats: Stats): Stats =
    Stats(
      eventCount + stats.eventCount,
      errorCount + stats.errorCount,
      wordCount ++ stats.wordCount
    ) // FIXME merge-add-maps correct
}
object Stats                                                                       {
  def zero                                 =
    Stats(0L, 0L, Map.empty)
  def one(event: Either[Throwable, Event]) =
    event.fold(t => Stats(0, 1, Map.empty), event => Stats(1, 0, Map(event.eventType -> 1)))

  implicit val encoder: JsonEncoder[Stats] = DeriveJsonEncoder.gen[Stats]
  implicit val decoder: JsonDecoder[Stats] = DeriveJsonDecoder.gen[Stats]
}

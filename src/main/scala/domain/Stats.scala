package domain

import zio.*
import zio.json.*

import java.time.{LocalDateTime, *}
import scala.util.Try

case class Stats(eventCount: Long, errorCount: Long, wordCount: Map[String, Long])
object Stats {
  implicit val encoder: JsonEncoder[Stats] = DeriveJsonEncoder.gen[Stats]
  implicit val decoder: JsonDecoder[Stats] = DeriveJsonDecoder.gen[Stats]
}

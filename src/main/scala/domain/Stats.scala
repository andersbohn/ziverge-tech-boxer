package domain

import zio.*
import zio.json.*

import java.time.{ LocalDateTime, * }
import scala.util.Try

case class Stats(eventCount: Long, errorCount: Long, wordCount: Map[String, Long]) {

  // .. hacked from stackoverflow - as am missing spark-sql :) ..
  def merge[A, B](a: Map[A, B], b: Map[A, B])(mergef: (B, Option[B]) => B): Map[A, B] = {
    val (big, small) = if (a.size > b.size) (a, b) else (b, a)
    small.foldLeft(big) { case (z, (k, v)) => z + (k -> mergef(v, z.get(k))) }
  }

  def mergeSum[A](a: Map[A, Long], b: Map[A, Long]): Map[A, Long] =
    merge(a, b)((v1, v2) => v2.map(_ + v1).getOrElse(v1))

  def updateWith(stats: Stats): Stats =
    Stats(eventCount + stats.eventCount, errorCount + stats.errorCount, mergeSum(wordCount, stats.wordCount))
}
object Stats {
  def zero                                 =
    Stats(0L, 0L, Map.empty)
  def one(event: Either[Throwable, Event]) =
    event.fold(t => Stats(0, 1, Map.empty), event => Stats(1, 0, Map(event.eventType -> 1)))

  implicit val encoder: JsonEncoder[Stats] = DeriveJsonEncoder.gen[Stats]
  implicit val decoder: JsonDecoder[Stats] = DeriveJsonDecoder.gen[Stats]
}

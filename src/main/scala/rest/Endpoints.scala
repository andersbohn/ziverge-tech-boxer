package rest

import domain.Stats
import eventsrc.EventsrcService
import zhttp.http.*
import zio.*
import zio.json.*

import java.time.{ZonedDateTime, *}

object Endpoints {

  def jsonize(stats: Stats): String =
    "" + stats.toJson

  val routes: HttpApp[EventsrcService, Throwable] = Http.collectM[Request] {
    case Method.GET -> Root / "stats"                                                 =>
      eventsrc.stats.map(jsonize(_)).map(Response.text)
  }

}

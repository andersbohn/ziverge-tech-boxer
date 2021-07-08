package rest

import domain.Stats
import eventsrc.Eventsrc.EventsrcEnv
import eventsrc.EventsrcService
import zhttp.http.*
import zio.*
import zio.json.*
import zio.stm.*

import java.time.{ ZonedDateTime, * }

object Endpoints {

  def jsonize(stats: Stats): String =
    "" + stats.toJson

  def routes(statsStm: TRef[Stats]): HttpApp[EventsrcEnv, Throwable] = Http.collectM[Request] {
    case Method.GET -> Root / "health" =>
      Task.succeed(Response.text("""{"ok": "isgood")"""))
    case Method.GET -> Root / "stats"  =>
      eventsrc.stats(statsStm).map(jsonize(_)).map(Response.text)
  }

}

import domain.{Event, Stats}
import eventsrc.Eventsrc.EventsrcEnv
import zio.*
import zio.blocking.*
import zio.stream.*

import java.io.IOException



package object eventsrc {
  type EventsrcService = Has[Eventsrc.Service]

  def stats: RIO[EventsrcService, Stats] =
    ZIO.accessM(_.get.stats)

  def streamEm: RIO[EventsrcEnv, Long] =
    ZIO.accessM(_.get[Eventsrc.Service].streamEm)
}

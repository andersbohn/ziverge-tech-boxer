import domain.{Event, Stats}
import zio.*
import zio.blocking.*
import zio.stream.*

import java.io.IOException



package object eventsrc {
  type EventsrcService = Has[Eventsrc.Service]

  def stats: RIO[EventsrcService, Stats] =
    ZIO.accessM(_.get.stats)
}

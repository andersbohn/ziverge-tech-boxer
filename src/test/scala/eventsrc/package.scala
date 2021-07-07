import domain.Event
import zio.*
import zio.blocking.*
import zio.stream.*



package object eventsrc {
  type EventsrcService = Has[Eventsrc.Service]

  def eventStream: RIO[Blocking with EventsrcService, List[Event]] =
    ZIO.accessM(_.get[Eventsrc.Service].eventStream)
}

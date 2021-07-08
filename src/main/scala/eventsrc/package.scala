import domain.Event
import zio.*
import zio.blocking.*
import zio.stream.*

import java.io.IOException



package object eventsrc {
  type EventsrcService = Has[Eventsrc.Service]
  
}

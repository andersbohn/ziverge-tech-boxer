package eventsrc

import zio.*
import zio.blocking.*
import domain.Event

import java.nio.file.Path
import java.time.LocalDateTime

case class EventFile(inputStream: ZInputStream)

object Eventsrc {

  trait Service {
    def eventStream: RIO[Blocking, List[Event]]
  }

  val liveZstream: ZLayer[Has[EventFile], Nothing, EventsrcService] =
    ZLayer.fromService[EventFile, Eventsrc.Service](EventFromFileImpl.eventFileService)
}

case object EventFromFileImpl {

  def eventFileService(file: EventFile) =
    new Eventsrc.Service {
      override def eventStream: RIO[Blocking, List[Event]] =
        (for {
          fileBytes <- file.inputStream.readAll(10000).mapError(_.getOrElse(new RuntimeException("None in ioexception?!? ")))
          fileStr <- Task.effect(fileBytes.map(_.toChar).mkString)
//          _        <- Task.effect(println(fileStr))
        } yield List(Event("bar", "", LocalDateTime.now())))
    }

}

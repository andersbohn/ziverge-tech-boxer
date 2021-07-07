package eventsrc

import zio.*
import zio.console.*
import zio.blocking.*
import zio.stream.*
import domain.Event

import java.io.{BufferedReader, InputStreamReader}
import java.nio.file.Path
import java.time.LocalDateTime

case class BlackBoxPath(localFile: String)
case class EventFile(inputStream: ZInputStream)

object Eventsrc {

  trait Service {
    def eventStream: Stream[Blocking, Event]
    def eventList: RIO[Blocking, List[Event]]
  }

  val liveBlackBoxStream: ZLayer[Has[BlackBoxPath], Nothing, EventsrcService] =
    ZLayer.fromService[BlackBoxPath, Eventsrc.Service](EventFromBlackBox.spinUpAndStream)

  val liveZstream: ZLayer[Has[EventFile], Nothing, EventsrcService] =
    ZLayer.fromService[EventFile, Eventsrc.Service](EventFromFileImpl.eventFileService)
}

case object EventFromBlackBox {

  def spinUpBuffer(file: BlackBoxPath): ZIO[Console, Throwable, Unit] = {
    def acquire(file: String) = ZIO.effect {
      val prc = sys.runtime.exec(file)
      val br = new BufferedReader(new InputStreamReader(prc.getInputStream, "UTF-8"))
      prc -> br
    }

    def release(prc:Process, reader: BufferedReader) = ZIO.effectTotal {
      prc.destroy()
      reader.close()
    }

    ZManaged.make(acquire(file.localFile))(release).use { (prc, reader) =>
      Task.effect{
        val str = reader.readLine()
        println(str) // FIXME aaarg
        str
      }.forever
    }
  }

  def spinUpAndStream(blackBox:BlackBoxPath) =
    new Eventsrc.Service {
      override def eventStream: Stream[Blocking, Event] =
        ???

      override def eventList: RIO[Blocking, List[Event]] =
        ???
    }

}

case object EventFromFileImpl {

  def eventFileService(file: EventFile) =
    new Eventsrc.Service {

      override def eventStream: Stream[Blocking, Event] = ???

      override def eventList: RIO[Blocking, List[Event]] =
        (for {
          fileBytes <- file.inputStream.readAll(10000).mapError(_.getOrElse(new RuntimeException("None in ioexception?!? ")))
          fileStr <- Task.effect(fileBytes.map(_.toChar).mkString)
//          _        <- Task.effect(println(fileStr))
        } yield List(Event("bar", "", LocalDateTime.now())))
    }

}

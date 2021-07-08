package eventsrc

import zio.*
import zio.console.*
import zio.blocking.*
import zio.stream.*
import domain.{ Event, EventRaw }

import java.io.{ BufferedReader, IOException, InputStream, InputStreamReader }
import java.nio.file.Path
import java.time.LocalDateTime

case class BlackBoxPath(localFile: String)
case class EventFile(inputStream: InputStream)

object Eventsrc {

  trait Service {
    def eventStream: ZStream[Blocking, Throwable, Either[Throwable,Event]]
  }

  val liveZstream: ZLayer[Has[EventFile], Nothing, EventsrcService] =
    ZLayer.fromService[EventFile, Eventsrc.Service](EventFromFileImpl.eventFileService)
}

case object EventFromBlackBox {

  def spinUpBuffer(file: BlackBoxPath): ZIO[Console with Blocking, Throwable, Unit] = {
    def acquire(file: String) = ZIO.effect {
      val prc = sys.runtime.exec(file)
      prc -> EventFile(prc.getInputStream)
    }

    def release(prc: Process, reader: EventFile) = ZIO.effectTotal {
      prc.destroy()
      reader.inputStream.close()
    }

    ZManaged.make(acquire(file.localFile))(release).use { (prc, reader) =>
      (for {
        event <- EventFromFileImpl.eventFileService(reader).eventStream
          .either
          .tap(eOrE => console.putStrLn(eOrE.toString))
          .runDrain
      } yield ()).forever
    }
  }


}

case object EventFromFileImpl {

  def eventFileService(file: EventFile) =
    new Eventsrc.Service {

      def parseEvent(s: String): ZIO[Blocking, Throwable, Event] =
        for {
          raw   <- EventRaw.parseJson(s)
          event <- Task.fromEither(Event.fromRaw(raw))
        } yield event

      override def eventStream: ZStream[Blocking, Throwable, Either[Throwable, Event]] =
        ZStream
          .fromInputStream(file.inputStream)
          .chunkN(1)
          .aggregate(ZTransducer.utf8Decode)
          .aggregate(ZTransducer.splitLines)
          .mapChunks(identity)
          .mapM(parseEvent(_).either)

    }

}

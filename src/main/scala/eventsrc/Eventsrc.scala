package eventsrc

import zio.*
import zio.console.*
import zio.blocking.*
import zio.stream.*
import domain.{Event, EventRaw}

import java.io.{BufferedReader, IOException, InputStream, InputStreamReader}
import java.nio.file.Path
import java.time.LocalDateTime

case class BlackBoxPath(localFile: String)
case class EventFile(inputStream: InputStream)

object Eventsrc {

  trait Service {
    def eventStream: ZStream[Blocking, Throwable, Event]
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
      val br  = new BufferedReader(new InputStreamReader(prc.getInputStream, "UTF-8"))
      prc -> br
    }

    def release(prc: Process, reader: BufferedReader) = ZIO.effectTotal {
      prc.destroy()
      reader.close()
    }

    ZManaged.make(acquire(file.localFile))(release).use { (prc, reader) =>
      (for {
        str  <- Task.effect(reader.readLine())
        raw  <- EventRaw.parseJson(str).either
        event = raw.flatMap(Event.fromRaw)
        _    <- console.putStrLn(s"> $event") // FIXME aaarg
      } yield ()).forever
    }
  }

  def spinUpAndStream(blackBox: BlackBoxPath) =
    new Eventsrc.Service {
      override def eventStream: ZStream[Blocking, IOException, Event] =
        ???

    }

}

case object EventFromFileImpl {

  def eventFileService(file: EventFile) =
    new Eventsrc.Service {

      def parseEvent(s:String): ZIO[Blocking, Throwable, Event] =
        for {
          raw <- EventRaw.parseJson(s)
          event <- Task.fromEither(Event.fromRaw(raw))
        } yield event

      override def eventStream: ZStream[Blocking, Throwable, Event] =
        ZStream.fromInputStream(file.inputStream)
          .chunkN(1)
          .aggregate(ZTransducer.utf8Decode)
          .aggregate(ZTransducer.splitLines)
          .mapChunks(identity)
          .mapM(parseEvent)

    }

}

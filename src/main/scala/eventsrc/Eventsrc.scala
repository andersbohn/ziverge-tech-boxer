package eventsrc

import zio.*
import zio.console.*
import zio.blocking.*
import zio.stream.*
import zio.stm.*
import domain.{ Event, EventRaw, Stats }
import eventsrc.Eventsrc.EventsrcEnv

import java.io.{ BufferedReader, IOException, InputStream, InputStreamReader }
import java.nio.file.Path
import java.time.LocalDateTime

case class BlackBoxPath(localFile: String)
case class RawEventInputStream(inputStream: InputStream)

object Eventsrc {

  type EventsrcEnv = Console & Blocking & EventsrcService

  trait Service {
    def eventStream: ZStream[EventsrcEnv, Throwable, Either[Throwable, Event]]
    def stats(statsStm: TRef[Stats]): STM[Throwable, Stats]
    def streamEm(statsStm: TRef[Stats]): ZIO[EventsrcEnv, Throwable, Long]
    def updateStats(statsStm: TRef[Stats], updCounts: Stats): STM[String, Stats]
  }

  val liveZstream: ZLayer[Has[RawEventInputStream], Nothing, EventsrcService] =
    ZLayer.fromService[RawEventInputStream, Eventsrc.Service](EventsFromInputStreamImpl.eventFileService)

  val liveBbox: ZLayer[Console & Blocking & Has[BlackBoxPath], Nothing, Has[RawEventInputStream]] =
    ZLayer.fromService[BlackBoxPath, RawEventInputStream](EventFromBlackBox.spinUnmanaged)

}

case object EventFromBlackBox {

  def spinUnmanaged(file: BlackBoxPath): RawEventInputStream = {
    val prc  = sys.runtime.exec(file.localFile)
    val reis = RawEventInputStream(prc.getInputStream)
    reis
  }

  def spinManaged(file: BlackBoxPath): ZLayer[Console with Blocking, Throwable, Has[(Process, RawEventInputStream)]] = {
    def acquire                                            = ZIO.effect {
      val prc = sys.runtime.exec(file.localFile)
      prc -> RawEventInputStream(prc.getInputStream)
    }
    def release(prc: Process, reader: RawEventInputStream) = ZIO.effectTotal {
      prc.destroy()
      reader.inputStream.close()
    }

    ZLayer.fromAcquireRelease(acquire)(release)
  }

}

case object EventsFromInputStreamImpl {

  def eventFileService(file: RawEventInputStream) =
    new Eventsrc.Service {

      def parseEvent(s: String): ZIO[Blocking, Throwable, Event] =
        for {
          raw   <- EventRaw.parseJson(s)
          event <- Task.fromEither(Event.fromRaw(raw))
        } yield event

      override def streamEm(statsStm: TRef[Stats]): ZIO[EventsrcEnv, Throwable, Long] =
        val value: ZIO[EventsrcEnv, Throwable, Long] =
          eventStream.map(x => updateStats(statsStm, Stats.one(x))).runCount
        value

      override def eventStream: ZStream[EventsrcEnv, Throwable, Either[Throwable, Event]] =
        ZStream
          .fromInputStream(file.inputStream)
          .chunkN(1)
          .aggregate(ZTransducer.utf8Decode)
          .aggregate(ZTransducer.splitLines)
          .mapChunks(identity)
          .mapM(parseEvent(_).either)
          .tap(eOrE => console.putStrLn(s"eOrE: $eOrE"))

      override def stats(statsStm: TRef[Stats]): STM[Throwable, Stats] =
        statsStm.get

      override def updateStats(statsStm: TRef[Stats], updCounts: Stats): STM[String, Stats] =
        for {
          _        <- statsStm.update(oldStats => oldStats.updateWith(updCounts))
          resStats <- statsStm.get
        } yield resStats

    }

}

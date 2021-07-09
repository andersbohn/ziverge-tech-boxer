package eventsrc

import zio.*
import zio.console.*
import zio.blocking.*
import zio.clock.Clock
import zio.stream.*
import zio.stm.*
import zio.duration.*
import domain.{ Event, EventRaw, Stats }
import eventsrc.Eventsrc.EventsrcEnv

import java.io.{ BufferedReader, IOException, InputStream, InputStreamReader }
import java.nio.file.Path
import java.time.LocalDateTime

case class BlackBoxPath(localFile: String)
case class RawEventInputStream(inputStream: InputStream)

object Eventsrc {

  type EventsrcEnv = Clock & Console & Blocking & EventsrcService

  trait Service {
    def eventStream: ZStream[EventsrcEnv, Throwable, Either[Throwable, Event]]
    def stats(statsStm: TRef[Stats]): Task[Stats]
    def streamEm(statsStm: TRef[Stats]): ZIO[EventsrcEnv, Throwable, Long]
    def updateStats(statsStm: TRef[Stats], updCounts: Stats): ZIO[EventsrcEnv, Nothing, Stats]
  }

  val liveZstream: ZLayer[Has[RawEventInputStream], Nothing, EventsrcService] =
    ZLayer.fromService[RawEventInputStream, Eventsrc.Service](EventsFromInputStreamImpl.eventFileService)

  val consoleIn: ZLayer[Any, Nothing, Has[RawEventInputStream]] =
    ZLayer.succeed(RawEventInputStream(java.lang.System.in))

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
        eventStream
          .mapM(x => updateStats(statsStm, Stats.one(x)))
          .groupedWithin(30, 10.seconds)
          .mapM(x => STM.atomically(statsStm.update(_ => Stats.zero).as(x)))
          .runCount

      override def eventStream: ZStream[EventsrcEnv, Throwable, Either[Throwable, Event]] =
        ZStream
          .fromInputStream(file.inputStream)
          .chunkN(1)
          .aggregate(ZTransducer.utf8Decode)
          .aggregate(ZTransducer.splitLines)
          .mapChunks(identity)
          .mapM(parseEvent(_).either)
          .tap(eOrE => console.putStrLn(s"eOrE: $eOrE"))

      override def stats(statsStm: TRef[Stats]): Task[Stats] =
        STM.atomically(statsStm.get)

      override def updateStats(statsStm: TRef[Stats], updCounts: Stats): ZIO[EventsrcEnv, Nothing, Stats] =
        STM.atomically(for {
          x        <- statsStm.update(oldStats => oldStats.updateWith(updCounts))
          resStats <- statsStm.get
        } yield resStats)

    }

}

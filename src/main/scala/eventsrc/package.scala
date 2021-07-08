import domain.{ Event, Stats }
import eventsrc.Eventsrc.EventsrcEnv
import zio.*
import zio.blocking.*
import zio.stream.*
import zio.stm.*

import java.io.IOException

package object eventsrc {
  type EventsrcService = Has[Eventsrc.Service]

  def stats(statsStm: TRef[Stats]): ZSTM[EventsrcEnv, Throwable, Stats] =
    ZSTM.accessM(_.get[Eventsrc.Service].stats(statsStm))

  def streamEm(statsStm: TRef[Stats]): RIO[EventsrcEnv, Long] =
    ZIO.accessM(_.get[Eventsrc.Service].streamEm(statsStm))

  def updateStats(statsStm: TRef[Stats], updCounts: Stats): ZSTM[EventsrcEnv, String, Stats] =
    ZSTM.accessM(_.get[Eventsrc.Service].updateStats(statsStm, updCounts))
}

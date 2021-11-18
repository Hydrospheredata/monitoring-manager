package io.hydrosphere.monitoring.manager.util

import zio.clock.Clock

import scala.concurrent.duration.{Deadline, FiniteDuration, NANOSECONDS}
import zio.{clock, ZIO}

case class ZDeadline private (underlying: Deadline) extends Ordered[ZDeadline] {
  def +(other: FiniteDuration): ZDeadline           = ZDeadline(underlying + other)
  def -(other: FiniteDuration): ZDeadline           = ZDeadline(underlying - other)
  def -(other: ZDeadline): FiniteDuration           = underlying - other.underlying
  def timeLeft: ZIO[Clock, Nothing, FiniteDuration] = ZDeadline.now.map(now => this - now)
  def isOverdue: ZIO[Clock, Nothing, Boolean]       = timeLeft.map(_.toNanos < 0)

  override def compare(that: ZDeadline): Int = underlying.compare(that.underlying)
}

object ZDeadline {
  def now: ZIO[Clock, Nothing, ZDeadline] =
    clock.nanoTime.map(now => ZDeadline(Deadline(FiniteDuration.apply(now, NANOSECONDS))))

  implicit object ZDeadlineIsOrdered extends Ordering[ZDeadline] {
    def compare(a: ZDeadline, b: ZDeadline): Int = a compare b
  }
}

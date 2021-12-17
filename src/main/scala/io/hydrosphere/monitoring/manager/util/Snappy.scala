package io.hydrosphere.monitoring.manager.util

import org.xerial.snappy.{Snappy => JSnappy}
import zio.{Task, ZIO}

object Snappy {
  def compress(data: Array[Byte]): Task[Array[Byte]] = ZIO.effect(JSnappy.compress(data))
}

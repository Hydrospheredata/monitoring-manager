package io.hydrosphere.monitoring.manager.util

import zio.ZIO

object Error {
  final val notImplementedZ = ZIO.fail(new NotImplementedError())
}

import zio._
import zio.console._
import zio.stream._

import java.time.Duration

val runtime = Runtime.default

val x = ZStream(1)

val effect = for {
  fbr <- x.tap(n => putStrLn(n.toString))
    .repeat(Schedule.spaced(Duration.ofSeconds(1)))
    .ensuring(ZIO.effectTotal(println("Hey")))
    .runCollect
    .fork
  _ <- ZIO.sleep(Duration.ofSeconds(3))
  _ <- fbr.interrupt
} yield ()
runtime.unsafeRun(effect)
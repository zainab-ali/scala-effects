package ioeval

import cats.effect._

object E7 extends IOApp.Simple {

  val printThread: IO[Unit] = IO.delay(println(s"Is the current thread [${Thread.currentThread.getName}]?"))

  def run: IO[Unit] = printThread
}

package ioeval

import cats.effect._

object E8 extends IOApp.Simple {

  val printThread: IO[Unit] = IO.blocking(println(s"The current thread is [${Thread.currentThread.getName}]."))

  def run: IO[Unit] = printThread >> run
}

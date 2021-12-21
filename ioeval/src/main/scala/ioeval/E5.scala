package ioeval

import cats.effect._

object E5 extends IOApp.Simple {


  val name = s"Is the current thread [${Thread.currentThread.getName}]?"
  val printThread: IO[Unit] = IO.println(name)

  def run: IO[Unit] = printThread
}

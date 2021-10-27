package ioeval

import cats.effect._

object E6 extends IOApp.Simple {

  val printThread: IO[Unit] =
    IO.delay(Thread.currentThread.getName).flatMap { name =>
      // Could we have switched threads?
      val otherName = Thread.currentThread.getName
      // Lazy 
      IO.blocking(println(s"Is the current thread [$name], [$otherName] or [${Thread.currentThread.getName}]?"))
    }

  def run: IO[Unit] = printThread >> run
}

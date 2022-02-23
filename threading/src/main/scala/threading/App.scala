package threading

import cats.effect.*
import cats.effect.unsafe.IORuntimeConfig
import cats.effect.unsafe.IORuntime
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import scala.concurrent.duration.*
import cats.effect.implicits.*
import cats.implicits.*
import cats.effect.std.Random

/** Take a look at the following two computations. 
    What are the differences? 
  - How long does each take to run?
  - Are they run on the same pools? How can you tell?
*/
object Work {

  def writeToTheDatabase: IO[Unit] = {
    IO(Thread.sleep(5000L))
  }

  def calculateHash: IO[Unit] = {
    import java.security.MessageDigest
    val stringLength = 100000000
    for {
      random <- Random.scalaUtilRandom[IO]
      str <- random.nextString(stringLength)
      digest = MessageDigest.getInstance("SHA-256")
      _ <- IO(digest.digest(str.getBytes))
    } yield ()
  }

  def factorial: IO[Unit] = {
    @scala.annotation.tailrec
    def go(n: Long, total: Long): Long = {
      if (n > 1) {
        go(n - 1, total * n)
      } else total
    }

    printThread(() => go(2000000000L, 1))
  }

  /** Do a lot of work in parallel. */
  def doLotsOf(work: IO[Unit]): IO[Unit] =
    List.fill(20)(work).parSequence.void

  /** Time the work and print out the time once complete. */
  def time(work: IO[Unit]): IO[Unit] =
    work.timed.flatMap {
      case (t, _) => IO.println(s"The work took ${t.toSeconds} seconds.")
    }

  def printThread(work: () => Unit): IO[Unit] = {
    IO {
      val name = Thread.currentThread.getName
      val result = work()
      println(s"Running on thread $name")
    }
  }
}

object App extends IOApp.Simple {

  /** We'll play around with different numbers of threads */
  override def runtime: unsafe.IORuntime =
    Setup.createBasicRuntime(Setup.bounded("global", 1))

  def run: IO[Unit] = {
    /** We'll also do different kinds of work */
    Work.time(Work.doLotsOf(Work.calculateHash))
  }
}

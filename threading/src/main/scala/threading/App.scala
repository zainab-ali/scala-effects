package threading

import scala.concurrent._
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
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor
import doobie.*
import doobie.hikari.*
import doobie.implicits.*
import com.zaxxer.hikari.*

object Work {

  def transactor(ec: ExecutionContext): HikariTransactor[IO] = {
    val config = new HikariConfig()
    config.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres")
    config.setUsername("postgres")
    config.setPassword("postgres")
    config.setMaximumPoolSize(3)
    val dataSource = new HikariDataSource(config)
    HikariTransactor[IO](dataSource, ec)
  }

  def writeToTheDatabase(xa: HikariTransactor[IO]): IO[Unit] = {
    sql"select pg_sleep(10)".query[Unit].unique.transact(xa)
  }

  def snooze: IO[Unit] = IO.blocking(Thread.sleep(100000L))

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

  def sendKafkaMessage: Future[Int] = {
    // We need a thread pool
    import scala.concurrent.ExecutionContext.Implicits.global
    Future(println("Hey!")).as(1)
  }

  def handleError(work: IO[Unit]): IO[Unit] =
    work.handleErrorWith(e => IO.println(s"Caught error: $e"))
}

object App extends IOApp.Simple {

  /** We'll play around with different numbers of threads */
  val ecResource: Resource[IO, ExecutionContext] = ExecutionContexts.fixedThreadPool[IO](1)

  override val runtime: IORuntime = Setup.createRuntime(
    compute = Setup.bounded("io-compute", 4),
    blocking = Setup.unbounded("io-blocking")
  )

  def run: IO[Unit] = {
    // ecResource.use { ec =>
    //   val transactor = Work.transactor(ec)
    //   Work.time(
    //     Work.doLotsOf(Work.writeToTheDatabase(transactor))
    //   )
    // }
    Work.doLotsOf(Work.time(Work.factorial) >> Work.snooze)
  }
}

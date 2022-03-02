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
    config.setMaximumPoolSize(10)
    val dataSource = new HikariDataSource(config)
    HikariTransactor[IO](dataSource, ec)
  }

  def writeToTheDatabase(xa: HikariTransactor[IO]): IO[Unit] = {
    sql"select pg_sleep(5)".query[Unit].unique.transact(xa)
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
  val ec = ExecutionContexts.fixedThreadPool[IO](5)

  def run: IO[Unit] = {
    ec.use { ec =>
      val transactor = Work.transactor(ec)
      Work.time(Work.writeToTheDatabase(transactor))
    }
  }
}

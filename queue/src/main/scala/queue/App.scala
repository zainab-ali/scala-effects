package queue

import cats.effect.*
import cats.implicits.*
import java.util.UUID
import fs2.*

import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor
import doobie.*
import doobie.hikari.*
import doobie.implicits.*
import scala.concurrent.ExecutionContext
import com.zaxxer.hikari.*
import cats.effect.std.*
import org.postgresql.util.PSQLException

object Work {

  def transactor(ec: ExecutionContext): HikariTransactor[IO] = {
    val config = new HikariConfig()
    config.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres")
    config.setUsername("postgres")
    config.setPassword("postgres")
    config.setMaximumPoolSize(3)
    config.setConnectionTimeout(2000L)
    val dataSource = new HikariDataSource(config)
    HikariTransactor[IO](dataSource, ec)
  }

}

object App extends IOApp.Simple {

  type TaskId = Int
  type Offset = Int

  case class Message(taskId: Int, offset: Offset, time: Int)

  def takeMessage(ref: Ref[IO, TaskId], random: Random[IO]): IO[Message] = {
    (ref.getAndUpdate(_ + 1), random.betweenInt(-10, 3)).mapN((id, time) =>
      Message(id, id, time)
    )
  }

  def processTask(xa: HikariTransactor[IO])(message: Message): IO[Unit] =
    IO.println(s"Processing ${message.taskId} with time ${message.time}") >>
      sql"select pg_sleep(${message.time})"
        .query[Unit]
        .unique
        .transact(xa)

  def recordTaskSuccess(taskId: TaskId): IO[Unit] =
    IO.println(s"Finished task $taskId")

  def recordTaskFailure(taskId: TaskId): IO[Unit] =
    IO.println(s"Failed task $taskId")

  def commitOffset(offset: Offset): IO[Unit] =
    IO.println(s"Committed offset $offset")

  def checkTaskIsPending(taskId: TaskId): IO[Boolean] = IO(true)

  def processMessages(
      ref: Ref[IO, TaskId],
      random: Random[IO],
      xa: HikariTransactor[IO]
  ): IO[Unit] =
    Stream
      .repeatEval(takeMessage(ref, random))
      .parEvalMap(3)(message =>
        checkTaskIsPending(message.taskId).flatMap { isPending =>
          if (isPending) processTask(xa)(message).as(message)
          else IO(message)
        }
      )
      .evalMap(message => recordTaskSuccess(message.taskId).as(message))
      .evalMap(message => commitOffset(message.offset))
      .compile
      .drain

  val ecResource: Resource[IO, ExecutionContext] =
    ExecutionContexts.fixedThreadPool[IO](1)

  def run: IO[Unit] = {
    ecResource.use { ec =>
      val transactor = Work.transactor(ec)
      for {
        ref <- Ref.of[IO, TaskId](1)
        random <- Random.scalaUtilRandom[IO]
        _ <- processMessages(ref, random, transactor)
      } yield ()
    }
  }
}

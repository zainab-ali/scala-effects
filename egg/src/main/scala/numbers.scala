package egg

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.std.Queue
import cats.implicits.*
import cats.effect.implicits.*
import fs2.*
import scala.util.control.NoStackTrace

object DownloadFailed extends Exception with NoStackTrace
object ParsingFailed extends Exception with NoStackTrace
object ConnectionFailed extends Exception with NoStackTrace
object InsertFailed extends Exception with NoStackTrace

case class Message(
    downloadFailure: Boolean,
    connectionFailure: Boolean,
    file: List[String]
)

final class Processor(messageQueue: Queue[IO, Message]) {

  val takeMessage: IO[Message] = messageQueue.take
    .flatTap(_ => IO.println("Took message"))


  def process(message: Message): Stream[IO, Unit] = {

    val downloadFile: IO[Stream[IO, String]] =
      IO.raiseWhen(message.downloadFailure)(DownloadFailed)
        .as(Stream.emits(message.file))
        .flatTap(_ => IO.println("Downloaded file."))

    def decodeRow(row: String): IO[Int] =
      IO(Integer.parseInt(row)).adaptError(_ => ParsingFailed)
        .flatTap(_ => IO.println(s"Decoded row $row."))


    def writeToPostgres(userId: Int): IO[Unit] =
      IO.raiseWhen(message.connectionFailure)(ConnectionFailed)
        .flatMap(_ => IO.raiseWhen(userId < 0)(InsertFailed))
        .flatTap(_ => IO.println(s"Written user id $userId to postgres."))

    val commitOffset: IO[Unit] = IO.println("Committing offset.")

    Stream
      .eval(downloadFile)
      .flatten
      .evalMap(decodeRow)
      .evalMap(writeToPostgres)
      .evalMap(_ => commitOffset)
  }

  val run: Stream[IO, Unit] =
    Stream
      .repeatEval(takeMessage)
      .flatMap(process)
}

object Numbers extends IOApp.Simple {

  val messages: List[Message] = List(
    Message(
      downloadFailure = false,
      connectionFailure = false,
      file = List("1", "2", "3")
    )
  )

  val messageQueueIO: IO[Queue[IO, Message]] = {
    Queue.unbounded[IO, Message].flatMap { queue =>
      Stream.emits(messages).enqueueUnterminated(queue).compile.drain.as(queue)
    }
  }

  val run: IO[Unit] =
    Stream
      .eval(messageQueueIO)
      .flatMap(queue => (new Processor(queue)).run)
      .compile
      .drain

}

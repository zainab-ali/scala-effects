package numbers

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.std.Queue
import cats.implicits.*
import cats.effect.implicits.*
import fs2.*
import scala.util.control.NoStackTrace

object DownloadFailed extends Exception("The file was not present in S3") with NoStackTrace
object ParsingFailed extends Exception with NoStackTrace
object ConnectionFailed extends Exception with NoStackTrace
object InsertFailed extends Exception with NoStackTrace

case class Message(
    fileIsAbsent: Boolean,
    connectionFailure: Boolean,
    file: List[String]
)

object MessageProcessor {

   def process(message: Message): Stream[IO, Unit] = {

    val downloadFile: IO[Stream[IO, String]] =
      IO.raiseWhen(message.fileIsAbsent)(DownloadFailed)
        .as(Stream.emits(message.file))
        .flatTap(_ => IO.println("Downloaded file."))

    def decodeRow(row: String): IO[Int] =
      IO(Integer.parseInt(row)).adaptError(_ => ParsingFailed)
        .flatTap(_ => IO.println(s"Decoded row $row."))


    def writeToPostgres(number: Int): IO[Unit] =
      IO.raiseWhen(message.connectionFailure)(ConnectionFailed)
        .flatMap(_ => IO.raiseWhen(number < 0)(InsertFailed))
        .flatTap(_ => IO.println(s"Written number $number to postgres."))

    val commitOffset: IO[Unit] = IO.println("Committing offset.")

    Stream
      .eval(downloadFile)
      .flatten
      .evalMap(decodeRow)
      .evalMap(writeToPostgres)
      .evalMap(_ => commitOffset)
  }
}

final class Processor(messageQueue: Queue[IO, Message]) {

  val takeMessage: IO[Message] = messageQueue.take
    .flatTap(_ => IO.println("Took message"))

  val run: Stream[IO, Unit] =
    Stream
      .repeatEval(takeMessage)
      .flatMap(MessageProcessor.process)
}

object Numbers extends IOApp.Simple {

  val messages: List[Message] = List(
    Message(
      fileIsAbsent = false,
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

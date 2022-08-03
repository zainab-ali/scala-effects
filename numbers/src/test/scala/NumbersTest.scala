import weaver.*
import cats.effect.*
import cats.effect.std.Queue

import fs2.Stream
import numbers.*

object NumbersSuite extends SimpleIOSuite {
  test("when a file is not present in S3 the processor should ???") {
    val message: Message = Message(
      fileIsAbsent = true,
      connectionFailure = false,
      file = List("1", "2", "3")
    )
    process(message).as(success)
  }

  test("when a single row fails to parse, the processor should ???") {
    val message: Message = Message(
      fileIsAbsent = false,
      connectionFailure = false,
      file = List("1", "not-a-number", "3")
    )
    process(message).as(success)
  }

  test("when a single row fails to be inserted, the processor should ???") {
    val message: Message = Message(
      fileIsAbsent = false,
      connectionFailure = false,
      file = List("1", "-42", "3")
    )
    process(message).as(success)
  }

  test("when the processor fails to connect to the database, it should ???") {
    val message: Message = Message(
      fileIsAbsent = false,
      connectionFailure = true,
      file = List("1", "2", "3")
    )
    process(message).as(success)
  }

  private def process(message: Message): IO[Unit] =
    MessageProcessor.process(message).compile.drain
}


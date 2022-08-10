import weaver.*
import cats.effect.*
import cats.effect.std.Queue

import fs2.Stream
import numbers.*

object NumbersSuite extends SimpleIOSuite {
  test("when a file is not present in S3 the processor should fail with a NotFound error") {
    val message: Message = Message(
      fileIsAbsent = true,
      connectionFailure = false,
      lines = List("1", "2", "3")
    )
    process(message).as(failure("unimplemented"))
  }

  test("when a single row fails to parse, the processor should ignore the row") {
    val message: Message = Message(
      fileIsAbsent = false,
      connectionFailure = false,
      lines = List("1", "not-a-number", "3")
    )
    process(message).as(failure("unimplemented"))
  }

  test("when a single row fails to be inserted, the processor should ignore the row") {
    val message: Message = Message(
      fileIsAbsent = false,
      connectionFailure = false,
      lines = List("1", "-42", "3")
    )
    process(message).as(failure("unimplemented"))
  }

  test("when the processor fails to connect to the database, it should ???") {
    val message: Message = Message(
      fileIsAbsent = false,
      connectionFailure = true,
      lines = List("1", "2", "3")
    )
    process(message).as(failure("unimplemented"))
  }

  private def process(message: Message): IO[Unit] =
    MessageProcessor.process(message).compile.drain
}


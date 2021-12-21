package streameval

import fs2._
import cats.effect._

object Effects2 extends IOApp.Simple {

  case class Cat(name: String)

  def run: IO[Unit] = Stream
    .eval(IO.println("Mao").as("Maru"))
    .evalMap(maru => IO.println(maru))
    .repeat
    .take(3)
    .compile
    .drain
}

package streameval

import fs2._
import cats.effect._

object Effects extends IOApp.Simple {

  case class Cat(name: String)

  def run: IO[Unit] = Stream
    .eval(IO.println("Mao"))
    .repeat
    .take(3)
    .compile
    .drain
}

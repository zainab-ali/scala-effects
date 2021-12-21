package streameval

import fs2._
import cats.effect._

object Performance1 extends IOApp.Simple {

  case class Cat(name: String)

  val randomCat: IO[Cat] = {
    val random = scala.util.Random(123)
    IO {
      if (random.nextBoolean) Cat("Mao") else Cat("Maru")
    }
  }

  // What is the memory composition?
  def run: IO[Unit] = Stream
    .eval(randomCat)
    .evalMap(cat => IO.println(cat))
    .repeat
    .compile
    .drain
}

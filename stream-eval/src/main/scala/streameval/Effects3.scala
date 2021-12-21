package streameval

import fs2._
import cats.effect._

object Effects3 extends IOApp.Simple {

  case class Cat(name: String)

  val random = scala.util.Random(123)

  val randomCat: IO[Cat] = {
    IO.delay {
      if (random.nextBoolean) Cat("Mao") else Cat("Maru")
    }
  }

  def run: IO[Unit] = Stream
    .eval(randomCat)
    .repeat
    .take(10)
    .evalMap(IO.println)
    .compile
    .drain
}

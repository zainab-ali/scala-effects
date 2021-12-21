package streameval

import fs2._
import cats.effect._

object Push extends IOApp.Simple {

  case class Cat(name: String)

  class RandomCatSource(handler: Cat => Unit) {
    val random = scala.util.Random(123)

    final def makeCats(): Unit = {
      val cat = if (random.nextBoolean) Cat("Mao") else Cat("Maru")
      handler(cat)
      makeCats()
    }
  }

  // What happens when we run this?
  def run: IO[Unit] = IO {
    new RandomCatSource(println).makeCats()
  }
}

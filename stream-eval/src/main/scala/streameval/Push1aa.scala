package streameval

import fs2._
import cats.effect._

object Push1aa extends IOApp.Simple {

  case class Cat(name: String)

  class RandomCatSource(handler: Cat => Unit) {
    val random = scala.util.Random(123)

    final def makeCats(): Unit = {
      val cat = if (random.nextBoolean) Cat("Mao") else Cat("Maru")
      handler(cat)
      makeCats()
    }
  }

  def randomCats: Stream[IO, Cat] = {
    val randomCatSource = new RandomCatSource(???)
    ???
  }

  // What happens when we run this?
  def run: IO[Unit] = randomCats.take(3).compile.drain
}

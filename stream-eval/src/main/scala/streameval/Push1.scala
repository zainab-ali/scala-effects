package streameval

import fs2._
import cats.effect._

object Push1 extends IOApp.Simple {

  import cats.effect.unsafe.implicits.global

  case class Cat(name: String)

  class RandomCatSource(handler: Cat => Unit) {
    val random = scala.util.Random(123)

    @annotation.tailrec
    final def makeCats(): Unit = {
      val cat = if (random.nextBoolean) Cat("Mao") else Cat("Maru")
      handler(cat)
      makeCats()
    }
  }


  def run: IO[Unit] = IO {
  // Is it possible for me to write a pure functional version?
    new RandomCatSource(cat => IO.println(cat).unsafeRunSync()).makeCats()
  }
}

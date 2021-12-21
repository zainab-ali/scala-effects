package streameval

import fs2._
import cats.effect._
import cats.effect.std.Dispatcher

object Push1a extends IOApp.Simple {

  import cats.effect.unsafe.implicits.global

  case class Cat(name: String)

  class RandomCatSource(handler: Cat => Unit) {
    val random = scala.util.Random(123)

    final def makeCats(): Unit = {
      val cat = if (random.nextBoolean) Cat("Mao") else Cat("Maru")
      handler(cat)
      // Thread.sleep(2000)
      makeCats()
    }
  }

  import scala.concurrent.duration._

  def run: IO[Unit] = Dispatcher[IO].use { dispatcher =>
    IO.delay {
      // Is it possible for me to write a pure functional version?
      new RandomCatSource(cat => println(cat)).makeCats()
    }.timeout(1.millisecond)
  }
}

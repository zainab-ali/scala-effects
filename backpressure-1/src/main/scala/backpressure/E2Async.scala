package backpressure

import fs2._
import cats.effect._

object E2Async extends IOApp.Simple {

  case class Cat(name: String)

  class RandomCatSource(handler: Cat => Unit) {
    val random = scala.util.Random(123)

    final def makeCats(): Unit = {
      val cat = if (random.nextBoolean) Cat("Mao") else Cat("Maru")
      handler(cat)
      // makeCats()
    }
  }

  import scala.concurrent.duration._

  def oneCat: IO[Cat] = IO.async_[Cat]{ handler =>
    new RandomCatSource(cat => handler(Right(cat))).makeCats()
  }

  // What happens when we run this?
  def run: IO[Unit] = oneCat.flatMap(IO.println)
}

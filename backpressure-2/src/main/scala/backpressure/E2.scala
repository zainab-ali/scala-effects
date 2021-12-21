package backpressure

import fs2._
import cats.effect._

object E2 extends IOApp.Simple {

  case class Cat(name: String)

  class RandomCatSource(handler: Cat => Unit) {
    val random = scala.util.Random(123)

    final def makeCats(): Unit = {
      val cat = if (random.nextBoolean) Cat("Mao") else Cat("Maru")
      handler(cat)
      // makeCats()
    }
  }

  // Can we write this?
  def oneCat: IO[Cat] = ???
  //   Deferred[IO, Cat].flatMap { deferred =>
  //         IO.delay {
  //           // We run unsafe stuff at the boundary
  //           val source = new RandomCatSource(cat => deferred.set(cat).unsafeRunSync()).makeCats()
  // } >> deferred.get
  //   }

  def run: IO[Unit] = oneCat.flatMap(IO.println)
}

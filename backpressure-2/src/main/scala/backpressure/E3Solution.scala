package backpressure

import fs2._
import cats.effect._
import cats.effect.std._

object E3Solution extends IOApp.Simple {

  case class Cat(name: String)

  class RandomCatSource(handler: Cat => Unit) {
    val random = scala.util.Random(123)

    final def makeCats(): Unit = {
      val cat = if (random.nextBoolean) Cat("Mao") else Cat("Maru")
      handler(cat)
      makeCats()
    }
  }

  // Can we write this?
  def manyCats: IO[Stream[IO, Cat]] = for {
    q <- Queue.unbounded[IO, Cat]
    _ <- IO.delay {
      // This isn't a great idea
      import cats.effect.unsafe.implicits.global
      new RandomCatSource(cat => q.offer(cat).unsafeRunSync()).makeCats()
    }.start
  } yield Stream.fromQueueUnterminated(q)


  def run: IO[Unit] = manyCats.flatMap { stream =>
    stream
    .evalMap(IO.println)
    .take(2)
    .compile.drain
  }
}

// Streams as program descriptions, not as data
// "codata"
// Streams are not mutable
// Quick look some datastryctyres for working with utation - ref, deferred, queue
// 

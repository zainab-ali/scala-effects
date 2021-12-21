package backpressure

import fs2._
import cats.effect._
import cats.effect.std._
import scala.concurrent.duration._

object E6 extends IOApp.Simple {

  case class Cat(name: String)

  class RandomCatSource(handler: Cat => Unit) {
    val random = scala.util.Random(123)

    final def makeCats(): Unit = {
      val cat = if (random.nextBoolean) Cat("Mao") else Cat("Maru")
      handler(cat)
      makeCats()
    }
  }

  def manyCats: IO[Stream[IO, Cat]] = for {
    // We're now bounding the queue
    q <- Queue.bounded[IO, Cat](100)
    _ <- IO.delay {
      // This isn't a great idea
      import cats.effect.unsafe.implicits.global
      // What happens here?
      new RandomCatSource(cat => q.offer(cat).unsafeRunSync()).makeCats()
    }.start
  } yield Stream.fromQueueUnterminated(q)


  // What happens to our memory composition?
  def run: IO[Unit] = manyCats.flatMap { stream =>
    stream
    .evalMap(IO.println)
    .metered(1.second)
    .compile.drain
  } >> IO.never
}

package combinators

import fs2._
import cats.effect._
import cats.effect.std._
import scala.concurrent.duration._

object E0 extends IOApp.Simple {

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
    q <- Queue.bounded[IO, Cat](10)
    _ <- IO.delay {
      // This isn't a great idea
      import cats.effect.unsafe.implicits.global
      new RandomCatSource(cat => q.offer(cat).unsafeRunSync()).makeCats()
    }.start
  } yield Stream.fromQueueUnterminated(q)


  // How often are the cats printed?
  // What happens to our memory composition?
  def run: IO[Unit] = manyCats.flatMap { stream =>
    stream
    .metered(1.millisecond)
    .evalMap((cat: Cat) => IO.println(cat))
    .metered(1000.second)
    .compile.drain
  } >> IO.never
}

package combinators

import fs2._
import cats.effect._
import cats.effect.std._
import scala.concurrent.duration._

object E3 extends IOApp.Simple {

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
    q <- Queue.bounded[IO, Cat](3)
    _ <- IO.delay {
      // This isn't a great idea
      import cats.effect.unsafe.implicits.global
      new RandomCatSource(cat =>
        (IO.println(s"Generated a cat $cat") >> q.offer(cat)
        ).unsafeRunSync()).makeCats()
    }.start
  } yield Stream.fromQueueUnterminated(q)


  // How often are the cats printed?
  // What happens to our memory composition?
  def run: IO[Unit] = manyCats.flatMap { stream =>
    val s1 = stream.metered(1.second)
    val s2 = stream.metered(1.millisecond).evalMap { cat =>
      IO.println(s"Got a fast cat $cat").as(cat)
    }
    s1.parZip(s2)
    .evalMap((cs: (Cat, Cat)) => IO.println(cs))
    .compile.drain
  } >> IO.never
}

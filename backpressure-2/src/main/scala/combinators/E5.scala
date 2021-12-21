package combinators

import fs2._
import cats.effect._
import cats.effect.std._
import scala.concurrent.duration._

object E5 extends IOApp.Simple {

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
  } yield take(q)

  def slowCats: Pipe[IO, Cat, Cat] = _.metered(1.second)
    .evalTap(cat => IO.println(s"Slow $cat"))
    .map(cat => Cat(s"GINGER-$cat"))
  def fastCats: Pipe[IO, Cat, Cat] = _.metered(1.millisecond)
    .evalTap(cat => IO.println(s"Fast $cat"))

  def take(q: Queue[IO, Cat]): Stream[IO, Cat] = {
    Stream.eval(IO.println("Taking a cat...") >> q.take) ++ take(q)
  }


  // How often are the cats printed?
  // What happens to our memory composition?
  def run: IO[Unit] = manyCats.flatMap { stream =>
    stream.broadcastThrough(slowCats, fastCats)
    .evalTap((cat: Cat) => IO.println(s"Downstream $cat"))
    .compile.drain
  } >> IO.never
}

package combinators

import fs2._
import cats.effect._
import cats.effect.std._
import scala.concurrent.duration._

object E1 extends IOApp.Simple {

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
      new RandomCatSource(cat => (
        IO.println(s"Generated cat $cat") >> q.offer(cat)
      ).unsafeRunSync()).makeCats()
    }.start
  } yield take(q)

  def take(q: Queue[IO, Cat]): Stream[IO, Cat] = {
    Stream.eval(IO.println("Taking a cat...") >> q.take) ++ take(q)
  }


  // import cats.effect.unsafe.implicits.global
  // import cats.effect.implicits._
  // import cats.implicits._
  // val printValue = IO.println("value")
  // val result: IO[Unit] = (printValue, printValue).mapN {
  //   (_, _) => ()
  // }
  // result.unsafeRunSync()

  // How often are the cats printed?
  // What happens to our memory composition?
  def run: IO[Unit] = manyCats.flatMap { stream =>
    // // What gets printed? (a tuple of the same cat)?
    // // How often?
    stream.zip(stream)
    .evalMap((cs: (Cat, Cat)) => IO.println(cs))
    .metered(1.second)
    .compile.drain
  } >> IO.never
}

package backpressure

import fs2._
import cats.effect._
import cats.effect.std._

object E4 extends IOApp.Simple {

  case class Cat(name: String)

  class RandomCatSource(handler: Cat => Unit) {
    val random = scala.util.Random(123)

    final def makeCats(): Unit = {
      val cat = if (random.nextBoolean) Cat("Mao") else Cat("Maru")
      handler(cat)
      makeCats()
    }
  }

  def fromQueue(q: Queue[IO, Cat]): Stream[IO, Cat] = {
    Stream.eval(q.take).repeat
    Stream.eval(q.take) ++ Stream.eval(q.take)
  }

  def manyCats: IO[Stream[IO, Cat]] = for {
    q <- Queue.unbounded[IO, Cat]
    _ <- IO.delay {
      import cats.effect.unsafe.implicits.global
      new RandomCatSource(cat => q.offer(cat).unsafeRunSync()).makeCats()
    }.start
  } yield Stream.eval(q.take)



  // What happens when we run this?
  // What is our memory composition?
  def run: IO[Unit] = manyCats.flatMap { stream =>
    stream
    .repeat
    .evalMap(IO.println)
    .compile.drain
  } >> IO.never
}

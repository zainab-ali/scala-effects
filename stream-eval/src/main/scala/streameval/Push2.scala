package streameval

import fs2._
import cats.effect._
import cats.effect.std._

object Push2 extends IOApp.Simple {

  case class Cat(name: String)

  class RandomCatSource(handler: Cat => Unit) {
    val random = scala.util.Random(123)

    @annotation.tailrec
    final def makeCats(): Unit = {
      val cat = if (random.nextBoolean) Cat("Mao") else Cat("Maru")
      handler(cat)
      makeCats()
    }
  }


  def catStream(q: Queue[IO, Cat]): Stream[IO, Cat] = Stream.fromQueueUnterminated(q)

  import cats.effect.unsafe.implicits.global

  def run: IO[Unit] = for {
    q <- Queue.unbounded[IO, Cat]
    source = new RandomCatSource(cat => q.offer(cat).unsafeRunSync())
    fiber <- IO.delay(source.makeCats()).start
    _ <- catStream(q)
    .evalMap(IO.println)
    .take(3)
    .compile
    .drain
  } yield ()
}

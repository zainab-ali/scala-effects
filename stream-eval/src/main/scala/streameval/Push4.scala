package streameval

import fs2._
import cats.effect._
import cats.effect.std._
import scala.concurrent.duration._

object Push4 extends IOApp.Simple {

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

  def herdCats: Pipe[IO, Cat, Unit] = _.evalMap(IO.println)

  import cats.effect.unsafe.implicits.global

  def run: IO[Unit] = for {
    q <- Queue.bounded[IO, Cat](10)
    source = new RandomCatSource(cat => q.offer(cat).unsafeRunSync())
    fiber <- IO.delay(source.makeCats()).start
    _ <- catStream(q).through(herdCats).compile.drain
    // .evalMap(IO.println)
    // .metered(1.second)
    // .compile
    // .drain
  } yield ()
}

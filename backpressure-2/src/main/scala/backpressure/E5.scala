package backpressure

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
      println(s"Making a $cat")
      makeCats()
    }
  }

  def manyCats: IO[Stream[IO, Cat]] = for {
    q <- Queue.bounded[IO, Cat](1)
    _ <- IO.delay {
      // This isn't a great idea
      import cats.effect.unsafe.implicits.global
      new RandomCatSource(cat => q.offer(cat).unsafeRunSync()).makeCats()
    }.start
  } yield Stream.fromQueueUnterminated(q)


  def run: IO[Unit] = manyCats.flatMap { stream =>
    stream
    .evalMap(IO.println)
    // How about if we process elements slowly?
    // What happens to our memory composition?
    .metered(1.second)
    .compile.drain
  } >> IO.never
}

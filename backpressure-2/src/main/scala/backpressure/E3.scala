package backpressure

import fs2._
import cats.effect._
import cats.effect.std._

object E3 extends IOApp.Simple {

  case class Cat(name: String)

  class RandomCatSource(handler: Cat => Unit) {
    val random = scala.util.Random(123)

    final def makeCats(n: Int): Unit = {
      val cat = if (random.nextBoolean) Cat(s"Mao-$n") else Cat(s"Maru-$n")
      handler(cat)
      makeCats(n + 1)
    }
  }

  import cats.effect.unsafe.implicits.global
  import scala.concurrent.duration._

  // Queue[IO, Cat] would be Ref[IO, Vector[Cat]]
  // 

  // Ref[IO, Stream[IO, Cat]]
  // Can we write this?
  def manyCats: IO[(Stream[IO, Cat], Ref[IO, Stream[IO, Cat]])] =
    Ref.of[IO, Stream[IO, Cat]](Stream.empty[IO]).flatMap { ref =>
      val source = new RandomCatSource({ cat =>
          ref.update(in => in ++ Stream(cat)).unsafeRunSync()
      })
      IO.delay {
        source.makeCats(0)
      }.start >> IO.sleep(1.milli) >> ref.get.map(stream => (stream, ref))
    }


  def run: IO[Unit] =
    manyCats.flatMap { (stream, ref) =>
      stream.evalMap(IO.println).compile.drain >>
      IO.sleep(1.milli) >> IO.println("Are there more cats?") >>
      ref.get.flatMap(_.evalMap(IO.println).compile.drain) >> IO.never
    }
}

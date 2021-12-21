package combinators.zip

import fs2._
import cats.effect._
import cats.effect.std._
import scala.concurrent.duration._

object E1 extends IOApp.Simple {

  case class Cat(number: Int)

  val catSource: Stream[Pure, Cat] = Stream.iterate(1)(_ + 1).map(Cat(_))
  val catQueue: IO[Queue[IO, Cat]] = {
    for {
      queue <- Queue.bounded[IO, Cat](1)
      _ <- catSource.evalMap(queue.offer).compile.drain.start
    } yield queue
  }

  def takeFrom(q: Queue[IO, Cat]): Stream[IO, Cat] = {
    Stream.eval(q.take).repeat
  }

  def run: IO[Unit] = catQueue.flatMap { (q: Queue[IO, Cat]) =>
    val s1 = takeFrom(q)
    s1.zip(s1)
    // Is this deterministic?
    // Can we predict the order of the cats?
    .evalMap((cs: (Cat, Cat)) => IO.println(s"Feeding cats $cs"))
    .metered(3.second)
    .compile.drain
  }
}

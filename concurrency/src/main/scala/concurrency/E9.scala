package combinators

import fs2._
import cats.effect._
import cats.effect.std._
import scala.concurrent.duration._

// A high number - this illustrates the problem of why we see so many messages at the beginning
object E9 extends IOApp.Simple {

  case class Cat(number: Int)

  val catSource: Stream[Pure, Cat] = Stream.iterate(0)(_ + 1).map(Cat(_))
  val catQueue: IO[Queue[IO, Cat]] = {
    for {
      queue <- Queue.bounded[IO, Cat](1)
      _ <- catSource.evalMap(cat => IO.println(s"Creating $cat") >> queue.offer(cat)).compile.drain.start
    } yield queue
  }

  def takeFrom(q: Queue[IO, Cat]): Stream[IO, Cat] = {
    Stream.eval(IO.println(s"Taking a cat") >> q.take).repeat
  }

  def run: IO[Unit] = catQueue.flatMap { (q: Queue[IO, Cat]) =>
    val s1 = takeFrom(q)
    s1.evalMap((cat: Cat) => IO.println(s"Feeding cat $cat"))
    .metered(3.second)
    .compile.drain
  }
}

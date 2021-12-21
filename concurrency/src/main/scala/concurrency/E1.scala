package combinators

import fs2._
import cats.effect._
import cats.effect.std._
import scala.concurrent.duration._


object E1 extends IOApp.Simple {

  case class Shopper(number: Int)

  // Shopper "generator"
  val shopperSource: Stream[Pure, Shopper] = Stream.iterate(1)(_ + 1).map(Shopper(_))
  val shopperQueue: IO[Queue[IO, Shopper]] = {
    for {
      queue <- Queue.bounded[IO, Shopper](1)
      // What's happened to this fiber?
      _ <- shopperSource.evalMap(queue.offer).compile.drain.start
    } yield queue
  }

  def takeFrom(q: Queue[IO, Shopper]): Stream[IO, Shopper] = {
    Stream.eval(q.take).repeat
  }
  //

  def run: IO[Unit] = shopperQueue.flatMap { (q: Queue[IO, Shopper]) =>
    val s1 = takeFrom(q)
    s1
    // Is this deterministic?
    // How many shoppers pay?
    .evalMap((shopper: Shopper) => IO.println(s"$shopper is paying").as(shopper))
    .evalMap((shopper: Shopper) => IO.println(s"$shopper has paid"))
    .metered(2.second)
    .take(5)
    .compile.drain
  }
}

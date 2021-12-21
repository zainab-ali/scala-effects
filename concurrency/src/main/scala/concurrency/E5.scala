package combinators

import fs2._
import cats.effect._
import cats.effect.std._
import scala.concurrent.duration._

object E5 extends IOApp.Simple {

  sealed trait Speed
  case object Slow extends Speed
  case object Fast extends Speed

  case class Shopper(number: Int, speed: Speed)

  // Every other shopper is slow
  val shopperSource: Stream[Pure, Shopper] = Stream.iterate(1)(_ + 1)
    .map(n => Shopper(n, if (n % 2 == 0) Slow else Fast))

  val shopperQueue: IO[Queue[IO, Shopper]] = {
    for {
      queue <- Queue.bounded[IO, Shopper](1)
      _ <- shopperSource.evalMap(queue.offer).compile.drain.start
    } yield queue
  }

  def takeFrom(q: Queue[IO, Shopper]): Stream[IO, Shopper] = {
    Stream.eval(q.take).repeat
  }

  def run: IO[Unit] = shopperQueue.flatMap { (q: Queue[IO, Shopper]) =>
    val s1 = takeFrom(q)
    s1
    // What happens to the fast shoppers?
      .parEvalMapUnordered(2)((shopper: Shopper) =>
        IO.println(s"$shopper is paying") >> IO.sleep(shopper.speed match {
          case Slow => 5.seconds
          case Fast => 0.seconds
        }).as(shopper))
    .evalMap((shopper: Shopper) => IO.println(s"$shopper has paid"))
    // .metered(2.second)
    .take(30)
    .compile.drain
  }
}

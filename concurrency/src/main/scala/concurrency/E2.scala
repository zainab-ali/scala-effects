package combinators

import fs2._
import cats.effect._
import cats.effect.std._
import scala.concurrent.duration._

object E2 extends IOApp.Simple {

  case class Shopper(number: Int)

  val shopperSource: Stream[Pure, Shopper] = Stream.iterate(1)(_ + 1).map(Shopper(_))
  val shopperQueue: IO[Queue[IO, Shopper]] = {
    for {
      queue <- Queue.bounded[IO, Shopper](1)
      // - when you take one shopper ...
      _ <- shopperSource.evalMap(queue.offer).compile.drain.start
    } yield queue
  }

  def takeFrom(q: Queue[IO, Shopper]): Stream[IO, Shopper] = {
    Stream.eval(q.take).repeat
    // Stream.fromQueueUnterminated(q)
  }

  def run: IO[Unit] = shopperQueue.flatMap { (q: Queue[IO, Shopper]) =>
    // 1 shopper in the queue at a time
    val s1 = takeFrom(q)
    s1
    // Is this still deterministic? - can we predict the order of the shoppers?
    // How many shoppers pay?
    // Not deterministic - "interruption"
    // TODO: Will this break down chunks?
    // - 
    // cassandra
      .parEvalMap(2)((shopper: Shopper) =>
        // increment a counter
        IO.println(s"$shopper is paying").as(shopper)
          // decrement
          // - how do we make sure that all paying shoppers get out of the shop?
      )
    // 
    // commit
      .evalMap((shopper: Shopper) =>
        // 
        IO.println(s"$shopper has paid"))
    .metered(2.second)
    .take(5)
    .compile.drain
  }
}

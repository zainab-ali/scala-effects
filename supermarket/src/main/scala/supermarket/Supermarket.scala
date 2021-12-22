package supermarket

import fs2._
import cats.effect._
import cats.effect.std._
import scala.concurrent.duration._

// Two checkouts
// One processing fast shoppers
// One processing slow shoppers
// TODO: What should we do once we've processed the fast ones?
object Supermarket extends IOApp.Simple {

  sealed trait Speed
  case object Slow extends Speed
  case object Fast extends Speed

  case class Shopper(number: Int, speed: Speed)

  // Every other shopper is slow
  val shopperSource: Stream[Pure, Shopper] = Stream
    .iterate(1)(_ + 1)
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

  def splitStreams(
      q: Queue[IO, Shopper]
  ): IO[(Stream[IO, Shopper], Stream[IO, Shopper])] = {
    val fastQueue = Queue.unbounded[IO, Shopper]
    val slowQueue = Queue.unbounded[IO, Shopper]
    val all = Stream.eval(q.take).repeat
    val fast = all.filter(_.speed == Fast)
    val slow = all.filter(_.speed == Slow)

    for {
      fastQueue <- Queue.unbounded[IO, Shopper]
      slowQueue <- Queue.unbounded[IO, Shopper]
      shopper <- q.take
      _ <-
        if (shopper.speed == Fast) fastQueue.offer(shopper)
        else slowQueue.offer(shopper)
    } yield (
      Stream.eval(fastQueue.take).repeat,
      Stream.eval(slowQueue.take).repeat
    )
    //val fast = all.split(_.speed == Fast).flatMap(Stream.chunk)
    //val slow = all.split(_.speed == Slow).flatMap(Stream.chunk)
    //Stream(1, 3, 5).map(Shopper(_, Fas t)).covary[IO]
    //val slow = Stream(2, 4, 6).map(Shopper(_, Slow)).covary[IO]
    // Can we implement this properly?
//    (fast, slow)
  }

  def processFastShopper(shopper: Shopper): IO[Unit] = {
    IO.println(s"$shopper is paying")
  }

  def processSlowShopper(shopper: Shopper): IO[Unit] = {
    IO.println(s"$shopper is paying") >> IO.sleep(5.seconds)
  }

  def havingPaid(stream: Stream[IO, Shopper]): Stream[IO, Shopper] = {
    stream.evalMap((shopper: Shopper) =>
      IO.println(s"$shopper has paid").as(shopper)
    )
  }

  def mao(q: Queue[IO, Shopper]): IO[Stream[IO, Shopper]] = {

    splitStreams(q).map { case (fast, slow) =>
      fast.merge(slow).through(havingPaid)
    }
    // How do we actually process things?

    // What exactly is merge doing?
//    finalStream.through(havingPaid)
  }

  def run: IO[Unit] = shopperQueue.flatMap { (q: Queue[IO, Shopper]) =>
    Stream
      .eval(mao(q))
      .flatten
      .take(30)
      .compile
      .drain
  }
}

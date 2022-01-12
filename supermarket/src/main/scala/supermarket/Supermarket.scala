package supermarket

import fs2._
import cats.effect._
import cats.effect.std._
import scala.concurrent.duration._
import supermarket.Supermarket.Speed
import supermarket.Supermarket.Shopper

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
  val shopperSource: Stream[IO, Shopper] = Stream
    .iterate(1)(_ + 1)
    .map(n => Shopper(n, if (n % 2 == 0) Slow else Fast))
    .covary[IO]

  final case class Queues(fast: Queue[IO, Shopper], slow: Queue[IO, Shopper])

  def createQueues: IO[Queues] = for {
    fastQueue <- Queue.unbounded[IO, Shopper]
    slowQueue <- Queue.unbounded[IO, Shopper]
  } yield Queues(fastQueue, slowQueue)

  def waitInQueue(
      queues: Queues
  )(in: Stream[IO, Shopper]): Stream[IO, Unit] = {
    def putShopperInQueue(shopper: Shopper): IO[Unit] = shopper.speed match {
      case Slow => queues.slow.offer(shopper)
      case Fast => queues.fast.offer(shopper)
    }

    in.evalMap(putShopperInQueue)
  }

  def fastCheckout(queues: Queues): Stream[IO, Shopper] = {
    val fastStream: Stream[IO, Shopper] = Stream.eval(queues.fast.take).repeat

    def checkout(shopper: Shopper): IO[Unit] = {
      IO.println(s"$shopper is paying")
    }
    fastStream.evalTap(checkout)
  }

  def slowCheckout(queues: Queues): Stream[IO, Shopper] = {
    val slowStream: Stream[IO, Shopper] = Stream.eval(queues.slow.take).repeat

    def checkout(shopper: Shopper): IO[Unit] = {
      IO.println(s"$shopper is paying") >> IO.sleep(5.seconds)
    }

    slowStream.evalTap(checkout)
  }

  def leaveCheckouts(
      fastStream: Stream[IO, Shopper],
      slowStream: Stream[IO, Shopper]
  ): Stream[IO, Shopper] = {

    def leave(shopper: Shopper): IO[Unit] = IO.println(s"$shopper has paid")

    fastStream
      .merge(slowStream)
      .evalTap(leave)
  }

  def runCheckouts(
      entering: Stream[IO, Nothing],
      leaving: Stream[IO, Shopper]
  ): Stream[IO, Shopper] = ???

  def run(in: Stream[IO, Shopper]): IO[Unit] = {
    createQueues.flatMap { queues =>
      println("Created the queues")
      val entering = waitInQueue(queues)(in)
      val fastShoppers = fastCheckout(queues)
      val slowShoppers = slowCheckout(queues)
      val leaving = leaveCheckouts(fastShoppers, slowShoppers)
      (entering.take(4).debug(_ => "Got an element") ++ leaving.take(4))
        .compile.drain
      //runCheckouts(entering, leaving)
        //.take(30).compile.drain
    }
  }

  def run: IO[Unit] = run(shopperSource)
}

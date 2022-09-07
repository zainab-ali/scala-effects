package egg

import cats.effect.IO
import cats.effect.Ref
import cats.effect.std.Queue
import cats.effect.IOApp
import cats.implicits.*
import fs2.*
import retry.*

import scala.util.control.NoStackTrace

sealed trait RawEgg
object RawEgg {
  case class FreshEgg(yolkIsFragile: Boolean, isSmall: Boolean) extends RawEgg
  case object RottenEgg extends RawEgg
}

sealed trait CookedEgg
object CookedEgg {
  case object Fried extends CookedEgg
  case object Scrambled extends CookedEgg
}

object RottenEggError extends Exception("The egg was rotten.") with NoStackTrace
object YolkIsBroken
    extends Exception("The yolk broke during frying.")
    with NoStackTrace
object Overcooked extends Exception("The egg is overcooked.") with NoStackTrace
object PowerCut extends Exception("There is a power cut.") with NoStackTrace

object FryCook {

  import scala.concurrent.duration._

  def crack(eggBox: Queue[IO, RawEgg]): IO[RawEgg.FreshEgg] = {
    eggBox.take.flatMap {
      case re @ RawEgg.RottenEgg => IO.raiseError(RottenEggError)
      case egg: RawEgg.FreshEgg  => IO.pure(egg)
    }
  }

  def crackAndRetry(eggBox: Queue[IO, RawEgg]): IO[RawEgg.FreshEgg] = {
    val policy = RetryPolicies.constantDelay[IO](2.seconds)

    def onFailure(failedValue: RawEgg, details: RetryDetails): IO[Unit] = {
      IO(println(s"Retrying on $failedValue: $details"))
    }

    def isSuccessful(value: RawEgg): IO[Boolean] =
      value match {
        case RawEgg.FreshEgg(yolkIsFragile, isSmall) => IO.pure(true)
        case RawEgg.RottenEgg                        => IO.pure(false)
      }

    def isIOException(e: Throwable): IO[Boolean] = e match {
      case RottenEggError => IO.pure(true)
      case _              => IO.pure(false)
    }

    def onError(err: Throwable, details: RetryDetails): IO[Unit] = {
      IO(println(s"Retrying on ${err.getMessage}: $details"))
    }

    val action: IO[RawEgg.FreshEgg] = crack(eggBox)

//    val rv: IO[RawEgg] =
//      retryingOnFailures(policy, isSuccessful, onFailure)(action)

    /** def retryingOnFailures[M[_]: Monad: Sleep, A](policy: RetryPolicy[M],
      *                                              wasSuccessful: A => M[Boolean],
      *                                              onFailure: (A, RetryDetails) => M[Unit])
      *                                              (action: => M[A]): M[A]
      *
      * def retryingOnSomeErrors[M[_]: Sleep, A, E](policy: RetryPolicy[M],
      *                                            isWorthRetrying: E => M[Boolean],
      *                                            onError: (E, RetryDetails) => M[Unit])
      *                                           (action: => M[A])
      *                                           (implicit ME: MonadError[M, E]): M[A]
      */

    retryingOnSomeErrors(policy, isWorthRetrying = isIOException, onError)(
      action
    )
  }

  def cook(power: Ref[IO, Boolean])(rawEgg: RawEgg.FreshEgg): IO[CookedEgg] = {
    power.get
      .flatMap { hasPower =>
        if (!hasPower) IO.raiseError(PowerCut)
        else IO.unit
      }
      .flatMap { _ => IO(cookWithPower(rawEgg)).rethrow }
  }

  def cookWithPower(rawEgg: RawEgg.FreshEgg): Either[Exception, CookedEgg] = {
    if (rawEgg.yolkIsFragile) Left(YolkIsBroken)
    else if (rawEgg.isSmall) Left(Overcooked)
    else Right(CookedEgg.Fried)
  }

  // Task 1: What happens if the RawEgg is rotten?
  // Task 2: If the egg is rotten, crack another egg
  // Task 3: If there are any errors, print "Sorry! Something wen't wrong."
  def fry(power: Ref[IO, Boolean], eggBox: Queue[IO, RawEgg]): IO[CookedEgg] = {

    IO.println(s"We're about to crack an egg")
      .flatMap { _ =>
        crackAndRetry(eggBox)
          .flatMap(egg => {
            IO.println(s"We cracked an egg: $egg").as(egg)
          })
          .flatMap { (egg: RawEgg.FreshEgg) =>
            cook(power)(egg)
          }
          .recoverWith { case YolkIsBroken =>
            IO.println("The yolk is broken! We're scrambling the egg.")
              .as(CookedEgg.Scrambled)
          }
//          .handleErrorWith(err =>
//            IO.println(s"We're about to handle the error: $err").flatMap(_ =>
//            fry(power, eggBox)
//              .flatTap(egg =>
//                IO.println(s"We handled the error: $err with $egg")
//              )
//            )
//          )
      }
      .flatTap((egg: CookedEgg) => IO.println(s"We cooked an egg: $egg"))
  }
}

object FryEggApp extends IOApp.Simple {
  val power: IO[Ref[IO, Boolean]] = Ref.of[IO, Boolean](true)
  val eggBox: IO[Queue[IO, RawEgg]] = {
    Queue.unbounded[IO, RawEgg].flatMap { queue =>
      Stream[IO, RawEgg](
        RawEgg.RottenEgg,
        RawEgg.RottenEgg,
        RawEgg.RottenEgg,
        RawEgg.FreshEgg(yolkIsFragile = true, isSmall = false),
        RawEgg.FreshEgg(yolkIsFragile = true, isSmall = false)
      ).enqueueUnterminated(queue).compile.drain.as(queue)
    }
  }

  def run: IO[Unit] = {
    for {
      power <- power
      eggBox <- eggBox
      egg <- FryCook.fry(power, eggBox)
      _ <- IO.println(egg)
    } yield ()
  }
}

object FrySeveralEggsApp extends IOApp.Simple {
  val power: IO[Ref[IO, Boolean]] = Ref.of[IO, Boolean](true)
  val eggBox: IO[Queue[IO, RawEgg]] = {
    Queue.unbounded[IO, RawEgg].flatMap { queue =>
      Stream[IO, RawEgg](
        RawEgg.RottenEgg,
        RawEgg.FreshEgg(yolkIsFragile = true, isSmall = true),
        RawEgg.FreshEgg(yolkIsFragile = false, isSmall = false),
        RawEgg.RottenEgg,
        RawEgg.FreshEgg(yolkIsFragile = false, isSmall = false),
        RawEgg.RottenEgg,
        RawEgg.FreshEgg(yolkIsFragile = false, isSmall = false)
      ).enqueueUnterminated(queue).compile.drain.as(queue)
    }
  }

  def run: IO[Unit] = {
    for {
      power <- power
      eggBox <- eggBox
      eggs <- Stream
        .repeatEval(FryCook.fry(power, eggBox))
        .take(2)
        .compile
        .toList
      _ <- IO.println(eggs)
    } yield ()
  }
}

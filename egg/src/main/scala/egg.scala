package egg

import cats.effect.IO
import cats.effect.Ref
import cats.effect.std.Queue
import cats.effect.IOApp
import cats.implicits.*
import fs2.*

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

  def crack(eggBox: Queue[IO, RawEgg]): IO[RawEgg.FreshEgg] = {
    eggBox.take.flatMap {
      case RawEgg.RottenEgg     => IO.raiseError(RottenEggError)
      case egg: RawEgg.FreshEgg => IO.pure(egg)
    }
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

  // Task 1: If the yolk is broken during cooking, return a scrambled egg instead
  // Task 2: If the egg is rotten, crack another egg
  // Task 3: If there are any errors, print "Sorry! Something wen't wrong."
  def fry(power: Ref[IO, Boolean], eggBox: Queue[IO, RawEgg]): IO[CookedEgg] = {

    IO.println(s"We're about to crack an egg")
      .flatMap { _ =>
        crack(eggBox)
          .flatMap(egg =>
            {
              IO.println(s"We cracked an egg: $egg")
                .flatMap(_ => IO.pure(egg))
            }
              // .asInstanceOf[IO[RawEgg.FreshEgg]]

          )
          // .flatTap(egg => IO.println(s"We cracked an egg: $egg"))
          .flatMap { (egg: RawEgg.FreshEgg) =>
            cook(power)(egg)
          // To here
          }
          .recoverWith { case YolkIsBroken =>
            IO.println("The yolk is broken! We're scrambling the egg.")
              .as(CookedEgg.Scrambled)
          }
          .handleErrorWith(err =>
            IO.println(s"We're about to handle the error: $err").flatMap(_ =>
            fry(power, eggBox)
              .flatTap(egg =>
                IO.println(s"We handled the error: $err with $egg")
              )
              .flatTap(_ => IO.raiseError(new OutOfMemoryError()))
            )
          )
          // This never gets executed
          // We've already handled all the exceptions
          // .handleErrorWith(err => IO.println(s"BOOOM!!!!").as(CookedEgg.Scrambled))
          // .handleError(_ =>
          //   CookedEgg.Scrambled
          // ) // Handles all the different cases
          .handleErrorWith(err =>
            IO.println(s"BOOOM!!!! $err").as(CookedEgg.Scrambled)
          )
      }

    // Be cautious where you place error handlers
    // The differences between recoverWith / handleErrorWith
    // The left and right channels, and map / flatMap vs recoverWith / handleErrorWith
    // IO is similar to Either, with left and right channels
    // Tracing the code, adding println statements to see what's going on


// [info] We're about to crack an egg
// [info] We're about to crack an egg
// [info] We cracked an egg: FreshEgg(true,false)
// [info] The yolk is broken! We're scrambling the egg.
// [info] We cooked an egg: Scrambled
// [info] We handled the error: Scrambled
// [info] We cooked an egg: Scrambled
// [info] Scrambled

      // From here

      // This is a partial function, so if the error isn't handled by `recover` then the IO remains in the same state.
      // `flatTap` taps into the type of IO (CookedEgg), but if the IO is in an "Exception" state then this won't be printed
      // IO is a fancy version of Either. There is a "failed" case for holding an exception
      .flatTap((egg: CookedEgg) => IO.println(s"We cooked an egg: $egg"))
    // We'll never get scrambled eggs because we'll call fry again even if the `YolkIsBroken`

    // IO.raiseError[CookedEgg](RottenEgg)
    //   .flatTap((egg: CookedEgg) => IO.println(s"We cooked an egg: $egg"))

    // IO.pure(CookedEgg.Scrambled)
    //   .handleErrorWith { _ => IO.println(s"We're recovering").as(CookedEgg.Scrambled)}

    // Left(RottenEgg)
    //   .map((egg: CookedEgg) => println(s"We cooked an egg: $egg"))
    // IO.pure(CookedEgg.Scrambled)
  }
}

object FryEggApp extends IOApp.Simple {
  val power: IO[Ref[IO, Boolean]] = Ref.of[IO, Boolean](true)
  val eggBox: IO[Queue[IO, RawEgg]] = {
    Queue.unbounded[IO, RawEgg].flatMap { queue =>
      Stream[IO, RawEgg](
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

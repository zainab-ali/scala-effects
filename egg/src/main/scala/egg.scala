package egg

import cats.effect.IO
import cats.effect.Ref
import cats.effect.std.Queue
import cats.effect.IOApp
import fs2._

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

object RottenEgg extends Exception("The egg was rotten.")
object YolkIsBroken extends Exception("The yolk broke during frying.")
object Overcooked extends Exception("The egg is overcooked.")
object PowerCut extends Exception("There is a power cut.")

object FryCook {

  def crack(eggBox: Queue[IO, RawEgg]): IO[RawEgg.FreshEgg] = {
    eggBox.take.flatMap {
      case RawEgg.RottenEgg     => IO.raiseError(RottenEgg)
      case egg: RawEgg.FreshEgg => IO.pure(egg)
    }
  }

  def cook(power: Ref[IO, Boolean])(rawEgg: RawEgg.FreshEgg): IO[CookedEgg] = {
    power.get
      .flatMap { hasPower =>
        if (!hasPower) IO.raiseError(PowerCut)
        else IO.unit
      }
      .map { _ => cookWithPower(rawEgg) }
  }

  def cookWithPower(rawEgg: RawEgg.FreshEgg): CookedEgg = {
    if (rawEgg.yolkIsFragile) throw YolkIsBroken
    else if (rawEgg.isSmall) throw Overcooked
    else CookedEgg.Fried
  }

  // Task 1: If the yolk is broken during cooking, return a scrambled egg instead
  // Task 2: If the egg is rotten, crack another egg
  // Task 3: If there are any errors, print "Sorry! Something wen't wrong."
  def fry(power: Ref[IO, Boolean], eggBox: Queue[IO, RawEgg]): IO[CookedEgg] = {
    crack(eggBox).flatMap { egg =>
      cook(power)(egg)
    }
  }
}

object FryEggApp extends IOApp.Simple {
  val power: IO[Ref[IO, Boolean]] = Ref.of[IO, Boolean](true)
  val eggBox: IO[Queue[IO, RawEgg]] = {
    Queue.unbounded[IO, RawEgg].flatMap { queue =>
      Stream[IO, RawEgg](
        RawEgg.FreshEgg(yolkIsFragile = true, isSmall = false),
        RawEgg.RottenEgg,
        RawEgg.FreshEgg(yolkIsFragile = true, isSmall = true)
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

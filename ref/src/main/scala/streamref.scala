package streamref

import cats.effect.IO
import cats.effect.Resource
import cats.effect.IOApp
import cats.effect.Ref
import cats.implicits.*
import cats.effect.implicits.*
import scala.io.Source
import scala.concurrent.duration.*
import fs2.*

object CatNamesApp extends IOApp.Simple {

  sealed trait State
  object State {
    object Running extends State
    object Finished extends State
    object Cancelled extends State
  }

  def manyCats: Stream[IO, String] =
    Stream("Mao", "Maru", "Popcorn").repeatN(100)

  def printCat: Pipe[IO, String, String] =
    _.evalTap(IO.println)

  def finish(stateRef: Ref[IO, State]): IO[Unit] =
    stateRef.set(State.Finished)

  def printState(state: Ref[IO, State]): IO[Unit] =
    state.get.flatMap(c => IO.println(s"The stream state is $c"))

  def run: IO[Unit] = {
    val stateRef: IO[Ref[IO, State]] = Ref.of[IO, State](State.Running)

      stateRef.flatMap { (state: Ref[IO, State]) =>

        val printCatNames: IO[Unit] =
          manyCats.through(printCat)
            .evalMap { _ => state.get }
            .takeWhile(_ != State.Cancelled)
            .compile
            .drain
        
        val printCatNamesAndFinish = printCatNames >> finish(state)

        val requestCancellation: IO[Unit] =
          IO.sleep(1.second).flatMap(_ => state.set(State.Cancelled))

        (printCatNamesAndFinish, requestCancellation).parTupled >> printState(state)
      }
  }
}

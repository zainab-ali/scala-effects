package ioref

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Ref
import cats.implicits.*
import cats.effect.implicits.*

object CatNamesApp extends IOApp.Simple {

  def manyCats: List[String] =
    List("Mao", "Maru", "Popcorn", "Totoro")

  def incrementCount(counter: Ref[IO, Int]): IO[Unit] =
    counter.get.flatMap(currentvalue => counter.set(currentvalue + 1))

  def printCount(counter: Ref[IO, Int]): IO[Unit] =
    counter.get.flatMap(c => IO.println(s"There are $c cats."))

  def run: IO[Unit] = {
    val counterRef: IO[Ref[IO, Int]] = Ref.of[IO, Int](0)
    counterRef.flatMap { (counter: Ref[IO, Int]) =>
      manyCats.traverse(_ => incrementCount(counter))
      .flatMap(_ => printCount(counter))
    }

  }
}

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

  def manyCats: Stream[IO, String] =
    Stream("Mao", "Maru", "Popcorn").repeatN(10)


  def printCatOrError(name: String): IO[String] =
    if (name.contains("n")) {
      IO.raiseError(new Exception("MEOW!")).as(name)
    } else {
      IO.println(name).as(name)
    }

  def printCat: Pipe[IO, String, String] = _.evalTap(IO.println)

  def incrementCount(counter: Ref[IO, Int]): IO[Unit] =
    counter.update(_ + 1)

  def updateCount(counter: Ref[IO, Int]): Pipe[IO, String, String] =
    _.evalTap(_ => incrementCount(counter))

  def printCount(counter: Ref[IO, Int]): IO[Unit] =
    counter.get.flatMap(c => IO.println(s"There are $c cats."))

  def run: IO[Unit] = {
    val counterRef: IO[Ref[IO, Int]] = Ref.of[IO, Int](0)
    counterRef.flatMap { (counter: Ref[IO, Int]) =>
      val printCatNamesStream: Stream[IO, String] = manyCats
        .through(printCat)
        .through(updateCount(counter))

      printCatNamesStream
        .compile
        .drain >>
        printCount(counter)
    }
  }
}

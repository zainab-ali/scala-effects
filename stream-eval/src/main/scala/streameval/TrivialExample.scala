package streameval

import fs2._
import cats.effect._

object TrivialExample extends IOApp.Simple {

  case class Cat(name: String)

  val stream: IO[List[Unit]] = Stream(Cat("Mao")).repeat
    .take(4)
    .evalMap(cat => IO.println(cat))
    .compile
    .toList

  println(stream)

  def run: IO[Unit] = stream.void
    // println(stream)
}


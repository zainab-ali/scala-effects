package ioeval

import cats.effect._
import cats.implicits._
import scala.concurrent.duration._

object E3 extends IOApp.Simple {

  val doug: IO[Unit] = IO.sleep(1.second) >> IO.println("Hey! I'm Doug.")
  val dan: IO[Unit] = IO.println("Hey! I'm Dan.")


  def run: IO[Unit] = (doug, dan).parMapN {
    case _ => ()
  }
}

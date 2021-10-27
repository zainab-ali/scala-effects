package ioeval

import cats.effect._
import cats.implicits._
import scala.concurrent.duration._

object E2 extends IOApp.Simple {

  val doug: IO[Unit] = IO.sleep(1.second) >> IO.println("Hey! I'm Doug.")
  val dan: IO[Unit] = IO.println("Hey! I'm Dan.")

  // Applicative 
  // doug.flatMap(d => (d, dan))
  def run: IO[Unit] = (doug, dan).mapN {
    case _ => ()
  }
}

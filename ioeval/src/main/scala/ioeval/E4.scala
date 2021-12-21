package ioeval

import cats.effect._

object E4 extends IOApp.Simple {

  val doug: IO[Unit] = IO.println("Hey! I'm Doug.")
  val runVal: IO[Unit] = doug.flatMap(_ => runVal)

  // We print Doug continuously
  // We don't print anything (the IO creation never completes)
  // We might StackOverflow (but would the compiler optimize it?)
  def run: IO[Unit] = runVal
    // doug.flatMap(_ => run)
}

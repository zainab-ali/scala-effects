package ioeval

import cats.effect._

object E1 extends IOApp.Simple {

  // val hello: IO[Unit] = IO.println("Hey! I'm Doug.")
  val hello = println("Hey! I'm doug.")

  def run: IO[Unit] = IO {
    println("Hey! I'm doug.")
    println("Hey! I'm doug.")
  }
    // IO.println("Hey! I'm Doug.") >> IO.println("Hey! I'm Doug.")
  // hello.flatMap(_ => hello)
}

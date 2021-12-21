package streameval

import fs2._
import cats.effect._
import cats.effect.std.Dispatcher

object Push1b extends IOApp.Simple {

  import cats.effect.unsafe.implicits.global

  case class Cat(name: String)

  class RandomCatSource(handler: Cat => Unit) {
    val random = scala.util.Random(123)

    final def makeCats(): Unit = {
      try {
        val cat = if (random.nextBoolean) Cat("Mao") else Cat("Maru")
      handler(cat)
      Thread.sleep(2000)
      makeCats()
      } catch {
        e =>
        println(s"Interrupted!")
        e.printStackTrace()
        throw e
      }
    }
  }

  import scala.concurrent.duration._

  def recIO: IO[Unit] = IO.delay {
    println(s"recursing")
  } >> recIO

  def run: IO[Unit] = Dispatcher[IO].use { dispatcher =>
    val io1 = IO.delay {
      // Is it possible for me to write a pure functional version?
      new RandomCatSource(cat => dispatcher.unsafeRunSync(IO.println(cat) >> IO.cede)).makeCats()
    }
    io1.void
    // val io2 = IO.delay {
    //   println(s"DONE")
    // }
    // IO.race(io1, io2).void
  }
}

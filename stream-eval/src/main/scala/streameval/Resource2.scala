package streameval

import fs2._
import cats.effect._

object Resource2 extends IOApp.Simple {

  case class Cat(name: String)

  val randomCat: IO[Cat] = {
    val random = scala.util.Random(123)
    IO {
      Cat(random.nextInt.toString)
    }
  }

  val randomCatResource = Resource.make(
    IO.println("Making a cat") >> randomCat)(
    cat => IO.println(s"Releasing $cat")
  )

  def run: IO[Unit] = Stream
    .resource(randomCatResource)
    .flatMap(cat => Stream.eval(IO.println(cat)))
    .repeat
    .take(10)
    .compile
    .drain
}

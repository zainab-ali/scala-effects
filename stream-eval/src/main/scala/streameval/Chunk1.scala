package streameval

import fs2._
import cats.effect._

object Chunk1 extends IOApp.Simple {

  case class Cat(name: String)

  val randomCats: IO[Chunk[Cat]] = {
    val random = scala.util.Random(123)
    IO {
      if (random.nextBoolean)
        Chunk(Cat("Mao"), Cat("Maru"))
      else
        Chunk(Cat("Bob"), Cat("Nyan Cat"))
    }
  }

  def printCat(cat: Cat): IO[Cat] = IO.println(cat).as(cat)

  val result: IO[List[Cat]] = Stream.evalUnChunk(randomCats) // Stream[IO, Cat]
    .repeat
    // Pulling a single chunk
    .take(3)
    // See before
    // .chunks
    // .evalMap(chunk => IO.println(s"Chunk before is: $chunk").as(chunk))
    // .flatMap(chunk => Stream.chunk(chunk))
    // 
    .evalMapChunk(cat => printCat(cat))
    // .chunks
    // .evalMap(chunk => IO.println(s"Chunk is: $chunk").as(chunk))
    // .flatMap(chunk => Stream.chunk(chunk))
    .compile
    .toList


  def run: IO[Unit] = result.flatMap(IO.println)
}

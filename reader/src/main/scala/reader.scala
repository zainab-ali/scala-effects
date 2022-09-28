package reader

import cats.effect.IO
import cats.effect.Resource
import cats.effect.IOApp
import cats.implicits.*
import cats.effect.implicits.*
import scala.io.Source
import scala.concurrent.duration.*
import fs2.*

object ReadFileApp extends IOApp.Simple {

  val openSource: IO[Source] = IO(Source.fromFile("cats.txt"))

  val sourceResource: Resource[IO, Source] =
    Resource.make({
      IO.println("Creating resource") *> openSource
    })(source => {
      IO.println("Closing resource") *>
      IO(source.close())
    })

  def readLine(s: Source): IO[String] = {
    val readChar = IO(s.nextOption())

    def go(cs: List[Char]): IO[String] =
      readChar.flatMap {
        case None | Some ('\n') => cs.reverse.mkString.pure[IO]
        case Some(c) => go(c :: cs)
      }
    go(Nil)
  }

  def printLine(s: Source): IO[Unit] =
    readLine(s).flatMap(IO.println)

  def run: IO[Unit] = {
    // Get a list of open file descriptors with:
    // ps | grep App | cut -d' ' -f 1 | head -n 1 | xargs lsof -p | grep cats.txt

    val range: List[Int] = (0 until 5).toList
    sourceResource.use(s => range.traverse(_ => printLine(s))).void

    val sourceStream: Stream[IO, Source] = Stream.resource(sourceResource)
    val printLineStream: Stream[IO, Unit] = ???
    printLineStream.take(5).compile.drain
  }
}

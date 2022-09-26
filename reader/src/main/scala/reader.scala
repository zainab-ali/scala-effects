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

    // Task: Which of these expressions reads five lines from the file?
    // Option 1
//    val p = sourceResource.flatMap(s => printLine(s).toResource)
//    val s: Resource[IO, List[Unit]] = range.traverse(_ => sourceResource.flatMap(s => printLine(s).toResource))
//    val r = p.flatMap(_ => p.flatMap(_ => p))
//    val t: IO[Unit] = r.use_
//
//    t
    // Option 2
//     range.traverse(_ => sourceResource.use(printLine)).void

    // Option 3
     sourceResource.use(s => range.traverse(_ => {
       printLine(s)
       printLine(s).toResource.use_
     })).void

    val s1: Resource[IO, Source] = sourceResource
    val s2: Resource[IO, Source] = sourceResource.use(s => s.pure[IO]).toResource
    // sourceResource.use(s => s.pure[IO])
    val s3: IO[Source] = openSource.flatMap { s =>
      s.pure[IO].flatMap { _ =>
        IO(s.close())
      }
    }

    // Option 4
    s1.flatMap(s => range.traverse(_ => {
      printLine(s).toResource
    })).use_
  }

  class FileService() {
    def getSource(fileNme: String): Resource[IO, Source] = ???
  }

}

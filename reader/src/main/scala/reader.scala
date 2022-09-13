package reader

import cats.effect.IO
import cats.effect.Resource
import cats.effect.IOApp
import cats.implicits.*
import scala.io.Source
import scala.concurrent.duration.*
import fs2.*

object ReadFileApp extends IOApp.Simple {

  val openSource: IO[Source] = IO(Source.fromFile("cats.txt"))

  def readLine(s: Source): IO[String] = {
    val readChar = IO(s.nextOption())

    def go(cs: List[Char]): IO[String] =
      readChar.flatMap {
        case None | Some ('\n') => cs.reverse.mkString.pure[IO]
        case Some(c) => go(c :: cs)
      }
    go(Nil)
  }

  def run: IO[Unit] = {
    // Get a list of open file descriptors with:
    // ps | grep App | cut -d' ' -f 1 | head -n 1 | xargs lsof -p | grep cats.txt
    val readFirstLine = openSource.flatMap(readLine).flatTap(IO.println)
    readFirstLine >> IO.sleep(100.seconds)
  }
}

package concurrency
import cats.effect._
import java.awt.event.ActionListener

object EAsync2 {
  import cats.implicits._
  trait ActionListener[T] {
    def onResponse(response: T): Unit
    def onFailure(e: Exception): Unit
  }

  trait Cancellable {
    def cancel(): Unit
  }

  def async[F[_]: Async, T](action: ActionListener[T] => Cancellable): F[T] =
    Async[F].async { cb =>
      Async[F].delay {
          val al = new ActionListener[T] {
            override def onResponse(response: T): Unit = cb(Right(response))
            override def onFailure(e: Exception): Unit = cb(Left(e))
          }
          action(al)
        }
        .map(cancelable => Some(Async[F].delay(cancelable.cancel())))
    }

}

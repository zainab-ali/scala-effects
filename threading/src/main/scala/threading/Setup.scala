package threading

import cats.effect.*
import cats.effect.unsafe.IORuntimeConfig
import cats.effect.unsafe.IORuntime
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import scala.concurrent.duration.*
import cats.effect.implicits.*
import cats.implicits.*
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/** This code sets up the IORuntime.  You don't need to understand how
  * this works ⸺ It's a bit complex because of the inner plumbing of
  * cats-effect.
  */
object Setup {

  /** We need a lazy reference to the IORuntime-to-be in order to set
    * up a work-stealing thread pool.  The parameter `() => IORuntime`
    * represents this lazy reference.
    */
  type ExecutionContextCreator = (() => IORuntime) => ExecutionContext

  /** Create an unbounded thread pool. */
  def unbounded(prefix: String): ExecutionContextCreator = { _ =>
    IORuntime.createDefaultBlockingExecutionContext(prefix)._1
  }

  /** Create an bounded work-stealing thread pool. */
  def bounded(prefix: String, numThreads: Int): ExecutionContextCreator = {
    (runtimeThunk: () => IORuntime) =>
      lazy val lazyRuntime = runtimeThunk()
      IORuntime
        .createDefaultComputeThreadPool(
          lazyRuntime,
          threads = numThreads,
          threadPrefix = prefix)
        ._1
  }

  def fixed(prefix: String, numThreads: Int): ExecutionContextCreator = _ => {
      val threadCount = new AtomicInteger(0)
      val executor = Executors.newFixedThreadPool(numThreads, { (r: Runnable) =>
        val t = new Thread(r)
        t.setName(s"${prefix}-${threadCount.getAndIncrement()}")
        t.setDaemon(true)
        t
      } )
      ExecutionContext.fromExecutor(executor)
    }

  /** Create an IORuntime given some bounded / unbounded thread pools. */
  def createRuntime(
      compute: ExecutionContextCreator,
      blocking: ExecutionContextCreator
  ): IORuntime = {
    val (scheduler, _) = IORuntime.createDefaultScheduler()

    lazy val runtime: IORuntime = IORuntime(
      compute = compute(() => runtime),
      blocking = blocking(() => runtime),
      scheduler = scheduler,
      shutdown = () => (),
      config = IORuntimeConfig()
    )

    runtime
  }

  def createBasicRuntime(compute: ExecutionContextCreator): IORuntime =
    createRuntime(compute, unbounded("this-should-never-be-used"))
}



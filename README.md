# The Supermarket

You can compile the project with sbt:

```sh
scala-effects> sbt
sbt> project supermarket
sbt> compile
```

Our aim is to code up a stream that will represent shoppers paying at two checkouts, one of which is for fast shoppers only. You can see the illustration in `supermarket/supermarket.png` for a rough idea of the input and output streams.

You can see and edit a diagram [here](https://drive.google.com/file/d/14q4GXcz5Bl8C8BZTkPNPVLt4mB_Vgejd/view?usp=sharing).

# Threading

## Warm-up exercise

Take a look at the `App` in the `threading` project. Check that you can compile and run it.

```sh
sbt
sbt> project threading
sbt> compile
```

*Note*: that the `supermarket` project will fail to compile. Please only compile `threading`.

The app can run two different kinds of work, found in the `Work` object.

 - How long does the `writeToTheDatabase` work take to run?

 - How long does the `calculateHash` work take to run?

   You will need to edit the following line of code:

   ```scala
	Work.time(Work.writeToTheDatabase)
   ```

## Session 2 - warm up exercise

Find out the number of available processors on your computer:

 - Enter the SBT console

 ```
 sbt
 sbt> project threading
 sbt> console
 scala> Runtime.getRuntime().availableProcessors()
 val res0: Int = 16 // This is the number of available processors
 ```

 - How long does it take to run the app with this number of threads?


   ```scala
   // In App.scala
	override def runtime: unsafe.IORuntime = Setup.createBasicRuntime(Setup.bounded("global", 16))
   ```

 - What about twice this number?
 - What about half this number?

## Session 3 - warm up exercise

The `evalOn` function allows us to execute an `IO` on a different thread pool (an `ExecutionContext` is another name for a thread pool).

 1. Take a look at the new `Work.factorial` function. Time it and see how long it takes:

	```scala
	// In App.scala
	  def run: IO[Unit] = Work.time(factorial)
	```

	If it takes less than a second, increase the `2000000000L` number within the function.

	What is printed as the thread name?

 2. Now execute it on the `scala.concurrent.ExecutionContext.global`

	```scala
	// In App.scala
	  def run: IO[Unit] = Work.time(factorial.evalOn(scala.concurrent.ExecutionContext.global))
	```

	What is printed as the thread name?

## Session 4 - warm up exercise

1. Take a look at `writeToTheDatabase`. It now queries postgres.

2. Run docker with `docker compose up -d`. This should start a postgres container.

3. Run the application with `sbt`. Check that you can connect to postgres. How long does the query take?

## Session 5 - warm up exercise

The app runs a single `writeToTheDatabase` task. It has:
 - an unbounded blocking thread pool as part of `IORuntime`
 - a bounded compute pool
 - an `ec` threadpool with a single thread that is passed to hikari.

1. Predict which threads will be blocked when running the app.

2. Run the app. In the session, we will profile this with visualvm to check your results.


# Session 6 - warm up exercise

The `snooze` task sleeps a thread for 100 seconds.

Consider a factorial task followed by a snooze task:

```scala
Work.factorial >> Work.snooze
```

1. If many of these tasks are run in parallel, predict how many factorials will be computed in the first 30 seconds.

  ```scala
  Work.doLotsOf(Work.time(Work.factorial) >> Work.snooze)
  ```

2. Run the app. In the session, we will profile this with visualvm to check your results.

# Session 7 - warm up exercise

This exercise explores the thread pool used by Hikari.

The hikari threadpool is configured with a single thread. There are only three connections allowed at once (the `maximumPoolSize` is `3`).  There is a connection timeout of two seconds.

1. Consider:

   ```scala
   Work.doLotsOf(Work.handleError(Work.writeToTheDatabase(transactor)))
   ```
   
   What errors do you expect to be printed to the console and when?
   
2. Consider configuring the thread pool with two threads:

   ```scala
   val ecResource: Resource[IO, ExecutionContext] = ExecutionContexts.fixedThreadPool[IO](2)
   ```

   What do you expect to be printed to the console and when?

# Session 8 - warm up exercise

The threading project now contains a `HttpApp`.

1. Start the app with `run`.
2. Query the app with `./work.sh 1`. How many factorial tasks do you expect to run?

# Session 9 - warm up exercise

The `HttpApp` runs items of `work`.

1. Start the app with `sbt run`.
2. Query the app with `./work.sh 5`. How many work items do you expect to run concurrently?
3. Consider the route:

	```
	case GET -> Root / "work" =>
	  work >> IO.println("Wrote to the db") >> Ok("Wrote to the db\n")
	```

	Modify `work` to `work.start`. Query the app again with `./work.sh 5`. How many work items do you expect to run concurrently?

# Session 10 - warm up exercise

The app now has two endpoints: `sync-work` and `async-work`.
  
1. Start the app with `sbt run`.

You can call the endpoints with the shell script, e.g: `./work.sh sync-work 4`.
2. Consider the difference between the `sync-work` and `async-work` endpoints.
  - How do they behave on failure? The fourth request made will fail due to a connection timeout.
  - What status codes do they respond with?
  - In both cases, how do they schedule work?

# Session 11 - warm up exercise

The app now has two endpoints under `work`.
 - The `POST` endpoint starts an async task.
 - The `GET` endpoint checks its status.

1. Think about the code needed to properly implement these endpoints.  Draw a rough diagram of the design in Excelidraw (or your preferred tool).

We'll begin today's session by mobbing on a design.

# Session 12 - warm up exercise

The app has some stubbed code under the `work` endpoint.

```scala
 for {
   taskId <- Work.randomUUID
   _ <- Work.queueTask(taskId)
   _ <- Work.recordTask(taskId)
   result <- Ok(taskId.toString)
 } yield result
```

This queues a task (e.g. by sending it a kafka topic) and records it in some data store.

1. What possible states can a task be in? You can consider "queued" and "running" to be states.
2. What happens if `queueTask` succeeds, but `recordTask` fails?
3. Can `recordTask` ever succeed if `queueTask` fails?

# Session 13 - Warm up exercise

The `messageQueue` project consumes messages (from kafka, for example), processes them and commits the offset.

Take a look at the `processMessages` function.

1. Can it ever commit an offset for a task before the task has been processed?
2. Can it ever process a task more than once? Consider the case of application failure and restarts.

# Session 14 - Warm up exercise

Consider `processMessages`. It processes each message sequentially.

1. Could we use `parEvalMap` to process these messages? 
2. What would the consequences be of using `parEvalMapUnordered`?


# Session 15 - Warm up exercise

In this session, we'll take a look at error handling. The code has been amended such that the message time is an `Int`. 

1. Should it be possible for the user to submit negative times? If so, would you expect an error?
2. What possible errors can occur when querying the database? For each error, consider whether we should recover from it.

# Session 16 - Warm up exercise

We'll explore error handling with the `egg` project.

1. Run the `FryEggApp`:

   ```
   sbt
   sbt> project egg
   sbt:egg> run
   ```

   You should see an exception being thrown indicating `"The yolk broke during frying"`.

2. Read through the `FryCook.fry` function to get a gist of what it does.
3. Take a look at the `cookWithPower` function.
   - What is the difference between throwing an exception and returning a value?
   - Is this a pure function? If not, how could we make it pure?

# Session 17 - warm up exercise

1. Take a look at the [cats API docs for `ApplicativeError`](https://typelevel.org/cats/api/cats/ApplicativeError.html). In particular, look at the `handleError` and `recover` functions.
2. The `crack` and `cook` functions capture errors in an `IO`: either function may fail. Consider how you can use the functions on `ApplicativeError` to perform the following tasks:
 - If the yolk is broken during cooking, return a scrambled egg instead
 - If the egg is rotten, crack another egg
 - If there are any errors, print "Sorry! Something wen't wrong."

# Session 18 - warm up exercise

In this session, we'll take a look at error handling and scopes.

For reference, here is our current implementation of `fry`:

```scala
def fry(power: Ref[IO, Boolean], eggBox: Queue[IO, RawEgg]): IO[CookedEgg] = {
  crack(eggBox).flatMap { egg =>
    cook(power)(egg)
	  .recover { case YolkIsBroken => CookedEgg.Scrambled }
  }.handleErrorWith(_ => fry(power, eggBox))
}
```

1. Consider the following implementation of `fry`, paying attention to the position of the `recover` function. Is the implementation correct?:


```scala
def fry(power: Ref[IO, Boolean], eggBox: Queue[IO, RawEgg]): IO[CookedEgg] = {
  crack(eggBox).flatMap { egg =>
	cook(power)(egg)
  }.recover { case YolkIsBroken => CookedEgg.Scrambled }
  .handleErrorWith(_ => fry(power, eggBox))
}
```

2. What about the following implementation, paying attention to `handleErrorWith`?


```scala
def fry(power: Ref[IO, Boolean], eggBox: Queue[IO, RawEgg]): IO[CookedEgg] = {
  crack(eggBox).flatMap { egg =>
	cook(power)(egg)
	  .recover { case YolkIsBroken => CookedEgg.Scrambled }
	  .handleErrorWith(_ => fry(power, eggBox))
  }
}
```

3. What about the following implementation?

```scala
def fry(power: Ref[IO, Boolean], eggBox: Queue[IO, RawEgg]): IO[CookedEgg] = {
  crack(eggBox).flatMap { egg =>
    cook(power)(egg)
  }
  .handleErrorWith(_ => fry(power, eggBox))
  .recover { case YolkIsBroken => CookedEgg.Scrambled }
}
```
# Session 19 - warm up exercise

In this session, we'll experiment with the order in which we handle errors.

For reference, here is our current implementation of `fry`:

```scala
def fry(power: Ref[IO, Boolean], eggBox: Queue[IO, RawEgg]): IO[CookedEgg] = {
  crack(eggBox).flatMap { egg =>
    cook(power)(egg) // Previous position of `recover` handler
  }
  .recover { case YolkIsBroken => CookedEgg.Scrambled } // Current position
  .handleErrorWith(_ => fry(power, eggBox))
}
```

We saw that moving the `recover` handler did not change the behaviour.

1. What about the following implementation? Are `YolkIsBroken` exceptions handled in the same way?

```scala
def fry(power: Ref[IO, Boolean], eggBox: Queue[IO, RawEgg]): IO[CookedEgg] = {
  crack(eggBox).flatMap { egg =>
    cook(power)(egg)
  }
  .handleErrorWith(_ => fry(power, eggBox))
  .recover { case YolkIsBroken => CookedEgg.Scrambled }
}
```


2. What about the following implementation, paying attention to the position of `handleErrorWith`? Are `RottenEgg` exceptions still handled in the same way?

```scala
def fry(power: Ref[IO, Boolean], eggBox: Queue[IO, RawEgg]): IO[CookedEgg] = {
  crack(eggBox).flatMap { egg =>
	cook(power)(egg)
	  .recover { case YolkIsBroken => CookedEgg.Scrambled }
	  .handleErrorWith(_ => fry(power, eggBox))
  }
}
```

# Session 22 - Warm up exercise

Take a look at the `numbers` project:

```
sbt 
> project numbers
> compile
> test
```

 - Run the code with `sbt run`
 - Test the code with `sbt test`
 - You'll see some tests in `NumbersTest` that are failing. How can you use the `handleError` functions to implement the correct behaviour?

# Session 23 - Warm up exercise

Take a look at the `numbers.scala` file.

1. In the last session, we changed the signature of `process` from:

	```scala
	def process(message: Message): Stream[IO, Unit]
	```

	to:

	```scala
	def process(message: Message): IO[Unit]
	```

	We did this by "compiling" the stream into an `IO` using `stream.compile.drain`.

	How is the resulting `IO` different from the stream?
		- Will it ever time out, where the stream wouldn't?
		- Will it hold more data in memory than the stream?

2. The signature for `run` is as follows:

   ```scala
   val run: Stream[IO, Unit]
   ```

	- What is the meaning of `Unit` in this signature?
	- Would this function signature be better as `val run: IO[Unit]`?


# Session 25 - Warm up exercise

Take a look at the `egg` project:

```
sbt 
> project egg
> compile
```

Run the code with `sbt run`.
 - What happens when the egg taken from the egg box is rotten?
 - Try and write a function with the following signature:
 ```scala
   def crackAndRetry(eggBox: Queue[IO, RawEgg]): IO[RawEgg.FreshEgg] = ???
 ```
 
 This function should call `crack`, but crack another egg if the egg is rotten.


# Session 26 - Warm up exercise

In the previous session, we attempted to retry the following action:

```scala
def crack(eggBox: Queue[IO, RawEgg]): IO[RawEgg.FreshEgg] = {
  eggBox.take.flatMap {
    case re @ RawEgg.RottenEgg => IO.raiseError(RottenEggError)
    case egg: RawEgg.FreshEgg => IO.pure(egg)
  }
}

```

We did so as follows:

```scala
def crackAndRetry(eggBox: Queue[IO, RawEgg]): IO[RawEgg.FreshEgg] = {
  val policy = RetryPolicies.constantDelay[IO](2.seconds)

  def onFailure(failedValue: RawEgg, details: RetryDetails): IO[Unit] = {
    IO(println(s"Retrying on $failedValue: $details"))
  }

  def isSuccessful(value: RawEgg): IO[Boolean] =
    value match {
      case RawEgg.FreshEgg(yolkIsFragile, isSmall) => IO.pure(true)
      case RawEgg.RottenEgg => IO.pure(false)
    }

  val action: IO[RawEgg.FreshEgg] = crack(eggBox)
  retryingOnFailures(policy,
    isSuccessful,
    onFailure
  )(action)
}
```

 - Run the app. Does the current solution retry on rotten eggs?
 - Will the `action` ever result in an `IO[RawEgg.RottenEgg]`?
 - Will the `isSuccessful` function ever result in an `IO(false)`?

# Session 27 - Warm up exercise

In the past few sessions, we've examined error handling with `IO`. This time, we'll take a look at error handling with `fs2.Stream.`

The `FrySeveralEggsApp` in `egg.scala` is meant to repeatedly cracks and cooks eggs.

 - Run the app. Why does it raise an exception?
 - Can we recover from the error using `Stream.handleErrorWith`? If not, why?
 
```scala
Stream
  .repeatEval(FryCook.crack(eggBox))
  .handleErrorWith(err => ...)
  .evalMap(FryCook.cook(power))
```
 - What other functions on `Stream` enable us to handle this error?

# Session 28 - Warm up exercise

This session will kick off the topic of resources.

Take a look at the `reader` project. This reads a file, `cats.txt`, prints the first line, then sleeps.

 1. Check that you can compile and run the app.
 2. The following command gets a list of open file descriptors and searches it for `cats.txt`:
    ```
    ps | grep App | cut -d' ' -f 1 | head -n 1 | xargs lsof -p | grep cats.txt
	```
	Run the command while running the app. What does it tell you about `scala.io.Source`?

# Session 29 - Warm up exercise

Take a look at the `reader.scala` file:

Which of these expressions constructs an `IO` that reads five lines from the `cats.txt` file?

```scala
// Option 1
range.traverse(_ => sourceResource.flatMap(s => printLine(s).toResource)).use_

// Option 2
range.traverse(_ => sourceResource.use(printLine)).void

// Option 3
sourceResource.use(s => range.traverse(_ => printLine(s))).void

// Option 4
sourceResource.flatMap(s => range.traverse(_ => printLine(s).toResource)).use_
```

For each expression, figure out how many times the source file is opened and closed. 

# Session 30: Warm up exercise

The following code in `reader.scala` reads five lines from the file:

```scala
val range: List[Int] = (0 until 5).toList
sourceResource.use(s => range.traverse(_ => printLine(s))).void
```

We can define a `sourceStream` as follows:

```scala
val sourceStream: Stream[IO, Source] = Stream.resource(sourceResource)
```

 1. How many elements can this stream contain?
 2. Combine this stream with the `printLine` function to print lines from the file such that the following code prints five lines:
 
    ```scala
    val printLineStream: Stream[IO, Unit] = ???
    printLineStream.take(5).compile.drain
	```

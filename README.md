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

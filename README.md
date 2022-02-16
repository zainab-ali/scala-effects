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


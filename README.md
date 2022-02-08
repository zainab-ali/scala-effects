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

The app can run two different kinds of work, found in the `Work` object.

 - How long does the `writeToTheDatabase` work take to run?

 - How long does the `calculateHash` work take to run?

   You will need to edit the following line of code:

   ```scala
	Work.time(Work.writeToTheDatabase)
   ```

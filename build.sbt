lazy val commonSettings = Seq(
  scalaVersion := "3.1.0",
  libraryDependencies += "co.fs2" %% "fs2-core" % "3.0.6",
  libraryDependencies += "co.fs2" %% "fs2-io" % "3.0.6",
  fork := true
)

// IO puzzlers
lazy val ioeval = (project in file("ioeval"))
  .settings(commonSettings)

// Functional streams with fs2. Needs cleaning up
lazy val streamEval = (project in file("stream-eval"))
  .settings(commonSettings)

lazy val backpressure1 = (project in file("backpressure-1"))
  .settings(commonSettings)

lazy val backpressure2 = (project in file("backpressure-2"))
  .settings(commonSettings)

lazy val concurrency = (project in file("concurrency"))
  .settings(commonSettings)

lazy val supermarket = (project in file("supermarket"))
  .settings(commonSettings)

lazy val threading = (project in file("threading"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core"      % "1.0.0-RC2",
      "org.tpolecat" %% "doobie-hikari"    % "1.0.0-RC2",
      "org.tpolecat" %% "doobie-postgres"  % "1.0.0-RC2",
      "org.http4s"   %% "http4s-blaze-server" % "0.23.10",
      "org.http4s"   %% "http4s-dsl" % "0.23.10"
    ))

lazy val messageQueue = (project in file("queue"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core"      % "1.0.0-RC2",
      "org.tpolecat" %% "doobie-hikari"    % "1.0.0-RC2",
      "org.tpolecat" %% "doobie-postgres"  % "1.0.0-RC2",
      "com.github.fd4s" %% "fs2-kafka" % "2.5.0-M3"
    )
  )

lazy val egg = (project in file("egg"))
  .settings(commonSettings)

lazy val numbers = (project in file("numbers"))
  .settings(commonSettings)
  .settings(
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    libraryDependencies ++=
      Seq(
        "com.disneystreaming" %% "weaver-core" % "0.7.9" % "test",
        "com.disneystreaming" %% "weaver-cats" % "0.7.9" % "test"
      ))

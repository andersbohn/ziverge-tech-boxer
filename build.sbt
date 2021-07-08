ThisBuild / resolvers ++= Seq("public", "snapshots", "releases").map(Resolver.sonatypeRepo)

val scala3Version = "3.0.0"

val zioVersion = "1.0.9"

val commonLibraryDependencies = Seq(
  "dev.zio" %% "zio"          % zioVersion,
  "dev.zio" %% "zio-test"     % zioVersion % "test",
  "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
  "dev.zio" %% "zio-logging"  % "0.5.11",
  "dev.zio" %% "zio-json"     % "0.1.5+39-baa4a668-SNAPSHOT",
  "io.d11"  %% "zhttp"        % "1.0.0.0-RC17"
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "ziverge-tech-boxer",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies ++= commonLibraryDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")

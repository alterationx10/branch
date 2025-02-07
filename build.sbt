ThisBuild / scalaVersion := "3.5.2"

ThisBuild / organization         := "dev.wishingtree"
ThisBuild / organizationName     := "Wishing Tree"
ThisBuild / organizationHomepage := Some(
  url("https://github.com/wishingtreedev")
)
ThisBuild / homepage             := Some(
  url("https://github.com/wishingtreedev/branch")
)
ThisBuild / description          := "A zero-dependency Scala framework"
ThisBuild / licenses             := List(
  "Apache 2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")
)
ThisBuild / developers           := List(
  Developer(
    id = "alterationx10",
    name = "Mark Rudolph",
    email = "mark@k8ty.app",
    url = url("https://alterationx10.com/")
  )
)

ThisBuild / scalacOptions ++= Seq(
  "-no-indent",
  "-rewrite",
  "-source:3.4-migration"
)

Test / fork := true

lazy val root = project
  .in(file("."))
  .settings(
    name           := "root",
    publish / skip := true
  )
  .aggregate(branch)

lazy val branch = project
  .in(file("branch"))
  .settings(
    name := "branch",
    libraryDependencies ++= Seq(
      "org.scalameta"     %% "munit"          % "1.1.0",
      "org.testcontainers" % "testcontainers" % "1.20.4",
      "org.postgresql"     % "postgresql"     % "42.7.5"
    ).map(_ % Test)
  )

ThisBuild / scalaVersion := "3.5.2"

lazy val root =
  project
    .in(file("."))
    .aggregate(lzy, http)

lazy val lzy =
  project.in(file("lzy"))

lazy val http =
  project
    .in(file("http"))
    .dependsOn(lzy)

lazy val example =
  project
    .in(file("example"))
    .dependsOn(lzy, http)

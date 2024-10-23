ThisBuild / scalaVersion           := "3.5.2"
ThisBuild / organization           := "dev.wishingtree"
ThisBuild / organizationName       := "Wishing Tree"
ThisBuild / organizationHomepage   := Some(url("https://wishingtree.dev"))
ThisBuild / homepage               := Some(
  url("https://github.com/wishingtreedev/branch")
)
ThisBuild / description            := "A zero-dependency Scala framework"
ThisBuild / licenses               := List(
  "Apache 2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")
)
ThisBuild / versionScheme          := Some("early-semver")
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository     := "https://s01.oss.sonatype.org/service/local"
ThisBuild / developers             := List(
  Developer(
    id = "alterationx10",
    name = "Mark Rudolph",
    email = "mark@wishingtree.dev",
    url = url("https://alterationx10.com/")
  )
)

lazy val root =
  project
    .in(file("."))
    .aggregate(lzy, spider)

lazy val lzy =
  project.in(file("lzy"))

lazy val spider =
  project
    .in(file("spider"))
    .dependsOn(lzy)

lazy val example =
  project
    .in(file("example"))
    .dependsOn(lzy, spider)
    .settings(
      fork := true
    )

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

ThisBuild / libraryDependencies += "org.scalameta" %% "munit" % "1.0.2" % Test

lazy val root =
  project
    .in(file("."))
    .settings(
      name := "branch"
    )
    .aggregate(lzy, spider)

lazy val macaroni =
  project
    .in(file("macaroni"))
    .settings(
      name := "macaroni"
    )

lazy val lzy =
  project
    .in(file("lzy"))
    .settings(
      name := "lzy"
    )

lazy val spider =
  project
    .in(file("spider"))
    .settings(
      name := "spider"
    )
    .dependsOn(lzy)

lazy val piggy =
  project
    .in(file("piggy"))
    .settings(
      name := "piggy",
      libraryDependencies ++= Seq(
        "com.h2database" % "h2" % "2.3.232" % Test
      )
    )
    .dependsOn(lzy)

lazy val friday =
  project
    .in(file("friday"))
    .settings(
      name := "friday"
    )
    .dependsOn(lzy)

lazy val example =
  project
    .in(file("example"))
    .dependsOn(lzy, spider, piggy)
    .settings(
      name := "example",
      libraryDependencies ++= Seq( // Examples and tests are allowed to have dependencies :-)
        "org.postgresql" % "postgresql" % "42.7.3"
      ),
      fork := true
    )

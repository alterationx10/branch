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

ThisBuild / scalacOptions ++= Seq(
  "-rewrite",
  "-no-indent"
)

ThisBuild / libraryDependencies += "org.scalameta" %% "munit" % "1.0.2" % Test

lazy val root =
  project
    .in(file("."))
    .settings(
      name := "branch"
    )
    .aggregate(branch)

lazy val branch =
  project
    .in(file("branch"))
    .settings(
      name := "branch",
      libraryDependencies ++= Seq(
        "com.h2database" % "h2" % "2.3.232" % Test
      )
    )

lazy val example =
  project
    .in(file("example"))
    .dependsOn(branch)
    .settings(
      name := "example",
      libraryDependencies ++= Seq( // Examples and tests are allowed to have dependencies :-)
        "org.postgresql" % "postgresql" % "42.7.3"
      ),
      fork := true
    )
addCommandAlias("fmtCheck", ";scalafmtCheckAll;scalafmtSbtCheck")
addCommandAlias("fmt", ";scalafmtAll;scalafmtSbt")

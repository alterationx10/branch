ThisBuild / scalaVersion := "3.7.3"

ThisBuild / organization         := "dev.alteration"
ThisBuild / organizationName     := "alterationx10"
ThisBuild / organizationHomepage := Some(
  url("https://github.com/alterationx10")
)
ThisBuild / homepage             := Some(
  url("https://github.com/alterationx10/branch")
)
ThisBuild / description          := "A zero-dependency Scala framework"
ThisBuild / licenses             := List(
  "Apache 2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")
)
ThisBuild / scmInfo              := Some(
  ScmInfo(
    url("https://github.com/alterationx10/branch"),
    "scm:git@github.com:alterationx10/branch.git"
  )
)
ThisBuild / developers           := List(
  Developer(
    id = "alterationx10",
    name = "Mark Rudolph",
    email = "mark@alteration.dev",
    url = url("https://alterationx10.com/")
  )
)

ThisBuild / version       := sys.env.getOrElse("BRANCH_VERSION", "0.0.0-SNAPSHOT")
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / publishTo     := Some(
  "Local Bundle" at (baseDirectory.value / "bundle").toURI.toString
)
credentials += Credentials(
  "GnuPG Key ID",
  "gpg",
  "0AD2D8FBE323DED1",
  ""
)

ThisBuild / scalacOptions ++= Seq(
  "-no-indent",
  "-rewrite",
  "-source:3.4-migration",
  "-Wunused:all",
  "-deprecation",
  "-feature",
  "-Xmax-inlines", "64",
)

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion += scalafixSemanticdb.revision

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
      "org.scalameta"     %% "munit"          % "1.2.0",
      "org.testcontainers" % "testcontainers" % "1.21.3",
      "org.postgresql"     % "postgresql"     % "42.7.8"
    ).map(_ % Test)
  )

addCommandAlias("fmt", "scalafmtAll")
addCommandAlias("fix", "scalafixAll")

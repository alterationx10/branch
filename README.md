# Branch

*Branch* is a zero-dependency framework for Scala 3 on Java 21+.

Why zero-dependency? *For fun!* Fun, **and** to illustrate how much you can get done with Scala without relying on
bigger Scala frameworks. *Branch* will not be the fastest, most performant solution, **but** it will (hopefully) let you
get things done quickly! Think of it as the framework for your side-project, not your job.

It's a pretty early stage project, and very much an evolving work-in-progress at the moment.

The docs (so far ™️) are on : https://branch.alteration.dev

## Development

It's a fairly standard `sbt` project; download and open in IntelliJ IDEA, or Visual Studio Code/Your editor with
Metals.

Some tests use `testcontainers` to run tests, so you will need Docker running to be able to run those tests
successfully.
# Branch

*Branch* is a zero-dependency framework for Scala 3 on Java 21+.

Why zero-dependency? *For fun!* Fun, **and** to illustrate how much you can get done with Scala without relying on
bigger Scala frameworks. *Branch* will not be the fastest, most performant solution, **but** it will (hopefully) let you
get things done quickly! Think of it as the framework for your side-project, not your job.

It's a pretty early stage project, and very much an evolving work-in-progress at the moment.

The docs (so far ™️) are on : https://branch.wishingtree.dev

## Development

I've set up the project to use `scala-cli` as the build tool. The code is in the `branch` directory.
To set up the project, run the following command in the project root directory:

```bash
scala-cli setup-ide branch
```

and then open the project in your IDE, and (e.g. for IntelliJ) import as a module.
If you wanted to add a submodule, you can run similar commands for subfolders.
See https://scala-cli.virtuslab.org/docs/cookbooks/ide/intellij-multi-bsp for details for Idea.

```bash
salac-cli publish local branch
```

I'm trying something new out, and keeping the tests alongside the source code (`.test.scala` files).
Some reusable testing components are in the `teskit` package, which are scoped to `test`.

You can run the tests with:

```bash
scala-cli test branch
```

Some tests use `testcontainers` to run tests, so you will need Docker running to be able to run those tests
successfully.
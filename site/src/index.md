# Branch Framework

*Branch* is a zero-dependency framework for Scala 3 on Java 21+.

It heavily leans on the virtual threads introduced in Java 21.

Its goal is to be a collections of resources a user can lean on to get things done quickly and easily, without being
overloaded by the myriad of other choices in the ecosystem. *Branch* will not be the fastest, most performant solution,
**but** it will (hopefully) let you get things done the quickest! Think of it as the framework for your side-project,
not your job.

Why zero-dependency? *For fun!* Fun, **and** to illustrate how much you can get done with Scala without relying on
bigger frameworks. Hopefully this will inspire confidence to the even larger things you can with bigger frameworks in
the ecosystem.

*Branch* is made up of a collection of modules, each focusing on different parts:

- **lzy** - Lazy Futures or Tiny Effects?
- **Spider** - A wrapper/framework around the Java HttpServer (bet you didn't even know there was one!)
- **Piggy** - A SQL framework, probably focused on PostgreSQL.
- **Scarecrow** - A Json framework, because Scala doesn't already have enough.

... and *more to come*!

A list of other things important to this framework's goals are (but not limited to):

- HTTP Client
- Configuration
- Web Sockets
- Event Bus
- Forms/Validations
- (HTML) Templating
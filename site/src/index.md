# Branch Framework

*Branch* is a zero-dependency framework for Scala 3 on Java 21+.

Why zero-dependency? *For fun!* Fun, **and** to illustrate how much you can get done with Scala without relying on
bigger frameworks. *Branch* will not be the fastest, most performant solution, **but** it will (hopefully) let you get
things done quickly! Think of it as the framework for your side-project, not your job.

*Branch* is made up of a collection of modules, each focusing on different parts:

- **Lzy** - Lazy Futures or Tiny Effects?
- **Spider** - A wrapper/framework around the Java HttpServer (I bet you didn't even know there was one!), as well as
  HttpClient helpers.
- **Piggy** - A SQL framework, probably focused on PostgreSQL.
- **Friday** - A Json library, because Scala doesn't already have enough.
- **Macaroni** - Some re-usable helpers and meta-programming utilities.
- **Veil** - `.env` / (Json based) Config utilities.
- **Blammo** - It's better than bad, it's (Json) logging!
- **Keanu** - A simple *typed* EventBus implementation, and a mediocre *untyped* ActorSystem.
- **Ursula** - A slim CLI framework.

... and *more to come*!

A list of other things important to this framework's goals are (but not limited to):

- Web Sockets
- (HTML) Templating
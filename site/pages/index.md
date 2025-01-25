---
title: Branch Framework
description: A zero-dependency framework for Scala 3 on Java 21+.
author: Mark Rudolph
published:
lastUpdated:
tags: scala
---

# Branch Framework

*Branch* is a zero-dependency framework for Scala 3 on Java 21+.

Why zero-dependency? *For fun!* Fun, **and** to illustrate how much you can get done with Scala without relying on
bigger frameworks. *Branch* will not be the fastest, most performant solution, **but** it will (hopefully) let you get
things done quickly! Think of it as the framework for your side-project, not your job.

*Branch* is made up of a collection of modules, each focusing on different parts:

- **[Lzy](/lzy)** - Lazy Futures or Tiny Effects?
- **[Spider](/spider)** - A wrapper/framework around the Java HttpServer (I bet you didn't even know there was one!), as well as
  HttpClient helpers.
- **[Piggy](/piggy)** - A SQL framework, probably focused on PostgreSQL.
- **[Friday](/friday)** - A Json library, because Scala doesn't already have enough.
- **[Macaroni](/macaroni)** - Some re-usable helpers and meta-programming utilities.
- **[Veil](/veil)** - `.env` / (Json based) Config utilities.
- **[Blammo](/blammo)** - It's better than bad, it's (Json) logging!
- **[Keanu](/keanu)** - A simple *typed* EventBus implementation, and a mediocre *untyped* ActorSystem.
- **[Ursula](/ursula)** - A slim CLI framework.
- **[Mustachio](/mustachio)** - A Mustache template engine, great for HTML templating.

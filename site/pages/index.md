---
title: Branch Framework
description: A zero-dependency framework for Scala 3 on Java 21+.
author: Mark Rudolph
published: 2025-01-25T04:36:00Z
lastUpdated: 2025-01-25T04:36:00Z
tags:
  - scala
  - framework
  - java21
---

# Branch Framework

_Branch_ is a zero-dependency framework for Scala 3 on Java 21+.

Why zero-dependency? _For fun!_ Fun, **and** to illustrate how much you can get done with Scala without relying on bigger frameworks. _Branch_ will not be the fastest, most performant solution, **but** it will (hopefully) let you get things done quickly! Think of it as the framework for your side-project, not your job.

_Branch_ is made up of a collection of modules, each focusing on different parts:

- **[Lzy](/lzy)** - Lazy Futures or Tiny Effects?
- **[Spider](/spider)** - A wrapper/framework around the Java HttpServer, as well as
  HttpClient helpers, a Web Socket Server implementation, and a server-side reactive UI framework named WebView
- **[Piggy](/piggy)** - A SQL framework around java.sql
- **[Friday](/friday)** - A JSON library, because Scala doesn't already have enough
- **[Macaroni](/macaroni)** - Some reusable helpers and meta-programming utilities
- **[Veil](/veil)** - `.env` / (JSON based) Config utilities
- **[Blammo](/blammo)** - It's better than bad, it's (JSON) logging! (And Metrics! And Tracing!)
- **[Keanu](/keanu)** - A simple _typed_ EventBus implementation, and a mediocre _untyped_ ActorSystem
- **[Ursula](/ursula)** - A slim CLI framework
- **[Mustachio](/mustachio)** - A Mustache template engine, great for HTML templating
- **[Holywood](/hollywood)** - A library for LLM Agents, with local LLMs in mind

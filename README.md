# Branch

*Branch* is a zero-dependency framework for Scala 3 on Java 21+.

Why zero-dependency? *For fun!* Fun, **and** to illustrate how much you can get done with Scala without relying on
bigger Scala frameworks. *Branch* will not be the fastest, most performant solution, **but** it will (hopefully) let you
get things done quickly! Think of it as the framework for your side-project, not your job.

It's a pretty early stage project and very much an evolving work-in-progress at the moment.

The docs (so far ™️) are on: https://branch.alteration.dev

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

## Development

It's a fairly standard `sbt` project; download and open in IntelliJ IDEA, or Visual Studio Code/Your editor with Metals.

Some tests use `testcontainers` to run tests, so you will need Docker running to be able to run those tests
successfully. Test fixtures are available:

- `HttpBinContainerSuite` - spins up an httpbin container for HTTP testing
- `PGContainerSuite` - spins up a PostgreSQL container with configurable per-test or per-suite lifecycle

The hollywood tests have a fixture to spin up llama-server, and are being tested against:
`llama-server -hf ggml-org/gpt-oss-20b-GGUF --ctx-size 8192 --jinja -ub 2048 -b 2048 --embeddings --pooling mean`

These tests are controlled by environment variables in `LlamaServerFixture`:

- Set `LLAMA_SERVER_TEST` to enable running the tests (otherwise they are ignored by default)
- Set `LLAMA_SERVER_START` to have the fixture automatically start llama-server (otherwise use an external instance)

or override them in the suite directly.
---
title: Veil
description: Configs and Environment Variables
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - env
  - config
---

# Veil

Veil is a small layer to help with configs and environment variables in Scala applications.

## Environment Variables

Veil provides a simple way to access environment variables through both `.env` files and system environment variables. The environment loading is determined by the `SCALA_ENV` environment variable, which can be:

- `DEV` (default) - loads from `.env`
- `TEST` - loads from `.env.test`
- `PROD` - loads from `.env.prod`

You can access environment variables using:

```scala
Veil.get(key: String): Option[String]
```

This will first check the loaded `.env` file, then fall back to `System.getenv()` if not found. Values in `.env` files that are quoted will have their quotes automatically stripped.

## Configuration Files

Veil provides a `Config` type-class for loading and parsing JSON configuration files into case classes. This makes it easy to load typed configuration from either files or resources.

Example usage:

```scala
// Define your config case class
case class AppConfig(host: String, port: Int) derives Config

// Load from a resource file
val config: Try[AppConfig] = Config.of[AppConfig].fromResource("app-config.json")

// Or load from a file path
val config: Try[AppConfig] = Config.of[AppConfig].fromFile("config/app-config.json")
```

The `Config` type-class will automatically handle JSON parsing and mapping to your case class. It uses the [Friday JSON library](/friday) under the hood.

### Auto-Derivation

The `Config` type-class can be automatically derived for any case class that has a `JsonDecoder` instance available. You can either:

1. Use the `derives Config` syntax as shown above
2. Let the compiler auto-derive it implicitly

Both approaches work equivalently:

```scala
// Explicit derivation
case class AppConfig(host: String, port: Int) derives Config

// Implicit derivation
case class AppConfig2(host: String, port: Int)
val config = Config.of[AppConfig2].fromResource("config.json")
```

This makes it easy to load strongly-typed configuration with minimal boilerplate code.

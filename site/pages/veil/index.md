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

Veil is a small layer to help with configs and environment variables.

Veil can load a `.env`, `.env.test`, or `.env.prod` file based on the environment variable `SCALA_ENV` being set to
`DEV`, `TEST`, or `PROD`. Values in this file are loaded into an in-memory map, and you can look up an env variable with
`Veil.get(key: String): Option[String]`. If it's not present in the in-memory map, it will then search Java's
`System.getenv()`.

There is also a `Config` type-class that helps with loading json from files/resources, and mapping them to a case
class (which presumably is used for configuration).
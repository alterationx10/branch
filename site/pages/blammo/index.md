---
title: Blammo
description: java.util.logging helpers
author: Mark Rudolph
published: 2025-01-25T04:36:00Z
lastUpdated: 2025-01-25T04:36:00Z
tags: 
  - logging
  - json
---

# Blammo

This module contains some helpers around `java.util.logging`. So far, the main thing it has going for it is a
`JsonFormatter` for some structured logging.

There is also a `JsonConsoleLogger` trait that you can mix in to your classes to get a logger that uses the formatter.
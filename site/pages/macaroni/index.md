---
title: Macaroni
description: A collection of reusable modules
author: Mark Rudolph
published: 2025-01-25T04:37:00Z
lastUpdated: 2025-01-25T04:37:00Z
tags:
  - parser
  - pool
  - metaprogramming
---

# Macaroni

This module has a collection of reusable modules that could be helpful in any project.

## Meta Programming

There are a couple of reusable inline helpers to summon lists of things by type, as well as some extra type helpers for
Tuples.

## Parser

There is a parser, which is the topic of a chapter in
[Function Programming in Scala (2nd Ed)](https://www.manning.com/books/functional-programming-in-scala-second-edition)
of parser combinators. It is currently used to power the [Friday](../friday/index.md) JSON library, but is useful for
any parsing application, I imagine. There is a `Parsers` trait, and a `Reference` implementation which can be used.

## ResourcePool

If you need a resource pool of type `R`, then there is a simple `trait ResourcePool[R]` to extend.

The borrowing of resources is gated by a `Semaphore` with `val poolSize: Int` (defaults to 5) permits.

The pool is eagerly filled on create.

Implement `def acquire: R` with how to create a resource, and `def release(resource: R): Unit` with how to cleanly close
the resource when shutting the pool down.

You can optionally over `def test(resource: R): Boolean` to provide a test to run after borrowing a resource, to make
sure it is still healthy.
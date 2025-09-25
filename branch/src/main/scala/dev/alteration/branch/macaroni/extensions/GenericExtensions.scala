package dev.alteration.branch.macaroni.extensions

import dev.alteration.branch.lzy.Lazy

object GenericExtensions {

  /** Lifts a value into a Lazy[A] */
  extension [A](a: A) {
    def lzy(): Lazy[A] = Lazy.fn(a)
  }

}

package dev.wishingtree.branch.macaroni.meta

import dev.wishingtree.branch.macaroni.meta.Types.UAnyType

import scala.compiletime.*

object Summons {

  inline def summonListOfValues[A <: Tuple]: List[?] =
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[ValueOf[t]].value :: summonListOfValues[ts]
    }

  inline def summonListOfValuesAs[A <: Tuple, B]: List[B] =
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[ValueOf[t]].value
          .asInstanceOf[B] :: summonListOfValuesAs[ts, B]
    }

  inline def summonListOf[A <: Tuple]: List[UAnyType[A]] =
    _summonListOf[A, UAnyType[A]]

  private inline def _summonListOf[A <: Tuple, U]: List[U] =
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[A].asInstanceOf[U] :: _summonListOf[ts, U]
    }

  inline def summonHigherListOf[A <: Tuple, F[_]]: List[F[UAnyType[A]]] =
    _summonHigherListOf[A, UAnyType[A], F]

  /** A helper method for summonHigherListOf, since the main operations is
    * recursive for T, and we would lose a type in the Tuple each time, we keep
    * a version of it constant - U.
    */
  private inline def _summonHigherListOf[A <: Tuple, U, F[U]]: List[F[U]] =
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[F[t]].asInstanceOf[F[U]] :: _summonHigherListOf[ts, U, F]
    }

}

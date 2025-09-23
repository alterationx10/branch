package dev.alteration.branch.macaroni.meta

import Types.UAnyType

import scala.compiletime.*

/** A collection of summoning methods.
  */
object Summons {

  /** Summon a list of values from a Tuple.
    */
  inline def summonListOfValues[A <: Tuple]: List[?] =
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[ValueOf[t]].value :: summonListOfValues[ts]
    }

  /** Summon a list of values from a Tuple, cast as a specific type.
    */
  inline def summonListOfValuesAs[A <: Tuple, B]: List[B] =
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[ValueOf[t]].value
          .asInstanceOf[B] :: summonListOfValuesAs[ts, B]
    }

  /** Summon a list whose type is a union type of the Tuple type arument.
    */
  inline def summonListOf[A <: Tuple]: List[UAnyType[A]] =
    _summonListOf[A, UAnyType[A]]

  /** A helper method for summonListOf, since the main operations is recursive
    * for T, and we would lose a type in the Tuple each time, we keep a version
    * of it constant.
    */
  private inline def _summonListOf[A <: Tuple, U]: List[U] =
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[A].asInstanceOf[U] :: _summonListOf[ts, U]
    }

  /** Summon a higher-kinded list whose type is a union type of the Tuple type
    * arument.
    */
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

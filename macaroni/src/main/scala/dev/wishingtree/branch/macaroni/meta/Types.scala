package dev.wishingtree.branch.macaroni.meta

object Types {
  
  /** An Intersection type, but with the default of Any if empty. Useful for
    * turning CaseClass(a: A, b: B, ...) into a type A & B & ... via a Mirror.
    */
  type IAnyType[T <: Tuple] = Tuple.Fold[T, Any, [x, y] =>> x & y]

  /** An Intersection type, but with the default of Nothing if empty. Useful for
    * turning CaseClass(a: A, b: B, ...) into a type A & B & ... via a Mirror.
    */
  type INothingType[T <: Tuple] = Tuple.Fold[T, Nothing, [x, y] =>> x & y]

  /** A Union type, but with the default of Any if empty. Useful for turning
    * CaseClass(a: A, b: B, ...) into a type A | B | ... via a Mirror. Note that
    * the there is a built-in scala.Union type for this which defaults to
    * Nothing.
    */
  type UAnyType[T <: Tuple] = Tuple.Fold[T, Any, [x, y] =>> x | y]

}

package dev.wishingtree.branch.piggy

import java.sql.ResultSet
import java.time.Instant
import java.util.UUID
import scala.deriving.Mirror
import scala.compiletime.*

/** Trait for parsing a `ResultSet` into a type `A`.
  * @tparam A
  *   the type to parse from the `ResultSet`
  */
trait ResultSetParser[A] {

  /** Parses a `ResultSet` into a type `A`.
    * @param resultSet
    *   the `ResultSet` to parse
    * @return
    *   the parsed value of type `A`
    */
  def parse(resultSet: ResultSet): A

  /** Maps the parsed value of type `A` to a value of type `B`.
    * @param f
    *   the function to map the parsed value
    * @tparam B
    *   the target type
    * @return
    *   a new `ResultSetParser` for type `B`
    */
  def map[B](f: A => B): ResultSetParser[B] = (resultSet: ResultSet) =>
    f(parse(resultSet))
}

object ResultSetParser {

  /** `ResultSetParser` instance for `String` values. */
  given ResultSetParser[String] with {
    def parse(resultSet: ResultSet): String =
      summonInline[ResultSetGetter[String]].get(resultSet, 1)
  }

  /** `ResultSetParser` instance for `UUID` values. */
  given ResultSetParser[UUID] =
    summonInline[ResultSetParser[String]].map(UUID.fromString)

  /** `ResultSetParser` instance for `Int` values. */
  given ResultSetParser[Int] with {
    def parse(resultSet: ResultSet): Int =
      summonInline[ResultSetGetter[Int]].get(resultSet, 1)
  }

  /** `ResultSetParser` instance for `Long` values. */
  given ResultSetParser[Long] with {
    def parse(resultSet: ResultSet): Long =
      summonInline[ResultSetGetter[Long]].get(resultSet, 1)
  }

  /** `ResultSetParser` instance for `Float` values. */
  given ResultSetParser[Float] with {
    def parse(resultSet: ResultSet): Float =
      summonInline[ResultSetGetter[Float]].get(resultSet, 1)
  }

  /** `ResultSetParser` instance for `Double` values. */
  given ResultSetParser[Double] with {
    def parse(resultSet: ResultSet): Double =
      summonInline[ResultSetGetter[Double]].get(resultSet, 1)
  }

  /** `ResultSetParser` instance for `Boolean` values. */
  given ResultSetParser[Boolean] with {
    def parse(resultSet: ResultSet): Boolean =
      summonInline[ResultSetGetter[Boolean]].get(resultSet, 1)
  }

  /** `ResultSetParser` instance for `BigDecimal` values. */
  given ResultSetParser[BigDecimal] with {
    def parse(resultSet: ResultSet): BigDecimal =
      summonInline[ResultSetGetter[BigDecimal]].get(resultSet, 1)
  }

  /** `ResultSetParser` instance for `java.sql.Date` values. */
  given ResultSetParser[java.sql.Date] with {
    def parse(resultSet: ResultSet): java.sql.Date =
      summonInline[ResultSetGetter[java.sql.Date]].get(resultSet, 1)
  }

  /** `ResultSetParser` instance for `java.sql.Time` values. */
  given ResultSetParser[java.sql.Time] with {
    def parse(resultSet: ResultSet): java.sql.Time =
      summonInline[ResultSetGetter[java.sql.Time]].get(resultSet, 1)
  }

  /** `ResultSetParser` instance for `java.sql.Timestamp` values. */
  given ResultSetParser[java.sql.Timestamp] with {
    def parse(resultSet: ResultSet): java.sql.Timestamp =
      summonInline[ResultSetGetter[java.sql.Timestamp]].get(resultSet, 1)
  }

  /** `ResultSetParser` instance for `Instant` values. */
  given ResultSetParser[Instant] =
    summonInline[ResultSetParser[java.sql.Timestamp]].map(_.toInstant)

  /** `ResultSetParser` instance for `Array[Byte]` values. */
  given ResultSetParser[Array[Byte]] with {
    def parse(resultSet: ResultSet): Array[Byte] =
      summonInline[ResultSetGetter[Array[Byte]]].get(resultSet, 1)
  }

  /** `ResultSetParser` instance for `Short` values. */
  given ResultSetParser[Short] with {
    def parse(resultSet: ResultSet): Short =
      summonInline[ResultSetGetter[Short]].get(resultSet, 1)
  }

  /** `ResultSetParser` instance for `Byte` values. */
  given ResultSetParser[Byte] with {
    def parse(resultSet: ResultSet): Byte =
      summonInline[ResultSetGetter[Byte]].get(resultSet, 1)
  }

  /** Summons a list of `ResultSetGetter` instances for a tuple type.
    * @tparam T
    *   the tuple type
    * @return
    *   a list of `ResultSetGetter` instances
    */
  private inline def summonGetter[T <: Tuple]: List[ResultSetGetter[?]] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonInline[ResultSetGetter[t]] :: summonGetter[ts]
    }

  /** Creates a `ResultSetParser` for a tuple type.
    * @tparam T
    *   the tuple type
    * @return
    *   a `ResultSetParser` for the tuple type
    */
  inline def ofTuple[T <: Tuple]: ResultSetParser[T] = {
    (resultSet: ResultSet) =>
      {
        val getters = summonGetter[T]
        val values  = getters.zipWithIndex.map((getter, index) => {
          getter.get(resultSet, index + 1)
        })
        values
          .foldRight(EmptyTuple: Tuple) { (value, acc) =>
            value *: acc
          }
          .asInstanceOf[T]
      }
  }

  /** Derives a `ResultSetParser` for a product type.
    * @param m
    *   the Mirror for the product type
    * @tparam A
    *   the product type
    * @return
    *   a new `ResultSetParser` for the product type
    */
  inline def derived[A](using m: Mirror.Of[A]): ResultSetParser[A] = {
    inline m match {
      case ms: Mirror.SumOf[A]     =>
        compiletime.error("Auto derivation of sum types not yet supported")
      case mp: Mirror.ProductOf[A] =>
        ofTuple[mp.MirroredElemTypes]
          .map(mp.fromProduct)
    }
  }
}

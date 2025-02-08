package dev.wishingtree.branch.piggy

import java.sql.ResultSet
import scala.deriving.Mirror
import scala.compiletime.*

trait ResultSetParser[A] {
  def parse(resultSet: ResultSet): A
}

object ResultSetParser {

  given ResultSetParser[String] with {
    def parse(resultSet: ResultSet): String =
      summonInline[ResultSetGetter[String]].get(resultSet, 1)
  }

  given ResultSetParser[Int] with {
    def parse(resultSet: ResultSet): Int =
      summonInline[ResultSetGetter[Int]].get(resultSet, 1)
  }

  given ResultSetParser[Long] with {
    def parse(resultSet: ResultSet): Long =
      summonInline[ResultSetGetter[Long]].get(resultSet, 1)
  }

  given ResultSetParser[Float] with {
    def parse(resultSet: ResultSet): Float =
      summonInline[ResultSetGetter[Float]].get(resultSet, 1)
  }

  given ResultSetParser[Double] with {
    def parse(resultSet: ResultSet): Double =
      summonInline[ResultSetGetter[Double]].get(resultSet, 1)
  }

  given ResultSetParser[Boolean] with {
    def parse(resultSet: ResultSet): Boolean =
      summonInline[ResultSetGetter[Boolean]].get(resultSet, 1)
  }

  given ResultSetParser[BigDecimal] with {
    def parse(resultSet: ResultSet): BigDecimal =
      summonInline[ResultSetGetter[BigDecimal]].get(resultSet, 1)
  }

  given ResultSetParser[java.sql.Date] with {
    def parse(resultSet: ResultSet): java.sql.Date =
      summonInline[ResultSetGetter[java.sql.Date]].get(resultSet, 1)
  }

  given ResultSetParser[java.sql.Time] with {
    def parse(resultSet: ResultSet): java.sql.Time =
      summonInline[ResultSetGetter[java.sql.Time]].get(resultSet, 1)
  }

  given ResultSetParser[java.sql.Timestamp] with {
    def parse(resultSet: ResultSet): java.sql.Timestamp =
      summonInline[ResultSetGetter[java.sql.Timestamp]].get(resultSet, 1)
  }

  given ResultSetParser[Array[Byte]] with {
    def parse(resultSet: ResultSet): Array[Byte] =
      summonInline[ResultSetGetter[Array[Byte]]].get(resultSet, 1)
  }

  given ResultSetParser[Short] with {
    def parse(resultSet: ResultSet): Short =
      summonInline[ResultSetGetter[Short]].get(resultSet, 1)
  }

  given ResultSetParser[Byte] with {
    def parse(resultSet: ResultSet): Byte =
      summonInline[ResultSetGetter[Byte]].get(resultSet, 1)
  }

  private inline def summonGetter[T <: Tuple]: List[ResultSetGetter[?]] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonInline[ResultSetGetter[t]] :: summonGetter[ts]
    }

  inline def derived[A](using m: Mirror.Of[A]): ResultSetParser[A] = {
    inline m match {
      case ms: Mirror.SumOf[A]     =>
        compiletime.error("Auto derivation of sum types not yet supported")
      case mp: Mirror.ProductOf[A] =>
        (resultSet: ResultSet) => {
          val getters = summonGetter[mp.MirroredElemTypes]
          val values  = getters.zipWithIndex.map((getter, index) => {
            getter.get(resultSet, index + 1)
          })
          val tuples  = values
            .foldLeft(EmptyTuple: Tuple) { (acc, value) =>
              value *: acc
            }
            .asInstanceOf[mp.MirroredElemTypes]
          mp.fromTuple(tuples)
        }
    }
  }
}

package dev.wishingtree.branch.piggy

import java.sql.ResultSet

/** A type-class for getting values from a ResultSet.
  */
trait ResultSetGetter[A] {
  def get(rs: ResultSet)(index: Int): A
}

/** A companion object for ResultSetGetter. import ResultSetGetter.given to
  * access default implementations.
  */
object ResultSetGetter {

  /** A ResultSetGetter for Int */
  given ResultSetGetter[Int] = new ResultSetGetter[Int] {
    def get(rs: ResultSet)(index: Int): Int = rs.getInt(index)
  }

  /** A ResultSetGetter for Long */
  given ResultSetGetter[Long] = new ResultSetGetter[Long] {
    def get(rs: ResultSet)(index: Int): Long = rs.getLong(index)
  }

  /** A ResultSetGetter for Float */
  given ResultSetGetter[Float] = new ResultSetGetter[Float] {
    def get(rs: ResultSet)(index: Int): Float = rs.getFloat(index)
  }

  /** A ResultSetGetter for Double */
  given ResultSetGetter[Double] = new ResultSetGetter[Double] {
    def get(rs: ResultSet)(index: Int): Double = rs.getDouble(index)
  }

  /** A ResultSetGetter for String */
  given ResultSetGetter[String] = new ResultSetGetter[String] {
    def get(rs: ResultSet)(index: Int): String = rs.getString(index)
  }

  /** A ResultSetGetter for Boolean */
  given ResultSetGetter[Boolean] = new ResultSetGetter[Boolean] {
    def get(rs: ResultSet)(index: Int): Boolean = rs.getBoolean(index)
  }

}

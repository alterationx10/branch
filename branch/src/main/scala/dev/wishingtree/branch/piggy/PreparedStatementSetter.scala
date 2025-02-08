package dev.wishingtree.branch.piggy

import java.sql.PreparedStatement

/** A type-class for setting values on a PreparedStatement.
  */
trait PreparedStatementSetter[A] {
  def set(ps: PreparedStatement)(index: Int)(value: A): Unit
}

/** A companion object for PreparedStatementSetter. import
  * PreparedStatementSetter.given to access default implementations.
  */
object PreparedStatementSetter {

  /** A PreparedStatementSetter for Int */
  given PreparedStatementSetter[Int] with {
    def set(ps: PreparedStatement)(index: Int)(value: Int): Unit =
      ps.setInt(index, value)
  }

  /** A PreparedStatementSetter for Long */
  given PreparedStatementSetter[Long] with {
    def set(ps: PreparedStatement)(index: Int)(value: Long): Unit =
      ps.setLong(index, value)
  }

  /** A PreparedStatementSetter for Float */
  given PreparedStatementSetter[Float] with {
    def set(ps: PreparedStatement)(index: Int)(value: Float): Unit =
      ps.setFloat(index, value)
  }

  /** A PreparedStatementSetter for Double */
  given PreparedStatementSetter[Double] with {
    def set(ps: PreparedStatement)(index: Int)(value: Double): Unit =
      ps.setDouble(index, value)
  }

  /** A PreparedStatementSetter for String */
  given PreparedStatementSetter[String] with {
    def set(ps: PreparedStatement)(index: Int)(value: String): Unit =
      ps.setString(index, value)
  }

  /** A PreparedStatementSetter for Boolean */
  given PreparedStatementSetter[Boolean] with {
    def set(ps: PreparedStatement)(index: Int)(value: Boolean): Unit =
      ps.setBoolean(index, value)
  }

}
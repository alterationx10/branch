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
  given PreparedStatementSetter[Int] = new PreparedStatementSetter[Int] {
    def set(ps: PreparedStatement)(index: Int)(value: Int): Unit =
      ps.setInt(index, value)
  }

  /** A PreparedStatementSetter for Long */
  given PreparedStatementSetter[Long] = new PreparedStatementSetter[Long] {
    def set(ps: PreparedStatement)(index: Int)(value: Long): Unit =
      ps.setLong(index, value)
  }

  /** A PreparedStatementSetter for Float */
  given PreparedStatementSetter[Float] = new PreparedStatementSetter[Float] {
    def set(ps: PreparedStatement)(index: Int)(value: Float): Unit =
      ps.setFloat(index, value)
  }

  /** A PreparedStatementSetter for Double */
  given PreparedStatementSetter[Double] = new PreparedStatementSetter[Double] {
    def set(ps: PreparedStatement)(index: Int)(value: Double): Unit =
      ps.setDouble(index, value)
  }

  /** A PreparedStatementSetter for String */
  given PreparedStatementSetter[String] = new PreparedStatementSetter[String] {
    def set(ps: PreparedStatement)(index: Int)(value: String): Unit =
      ps.setString(index, value)
  }

  /** A PreparedStatementSetter for Boolean */
  given PreparedStatementSetter[Boolean] =
    new PreparedStatementSetter[Boolean] {
      def set(ps: PreparedStatement)(index: Int)(value: Boolean): Unit =
        ps.setBoolean(index, value)
    }

}

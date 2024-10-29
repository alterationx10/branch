package dev.wishingtree.branch.piggy

import java.sql.PreparedStatement

trait PreparedStatementSetter[A] {
  def set(ps: PreparedStatement)(index: Int)(value: A): Unit
}

object PreparedStatementSetter {

  given PreparedStatementSetter[Int] = new PreparedStatementSetter[Int] {
    def set(ps: PreparedStatement)(index: Int)(value: Int): Unit =
      ps.setInt(index, value)
  }

  given PreparedStatementSetter[Long] = new PreparedStatementSetter[Long] {
    def set(ps: PreparedStatement)(index: Int)(value: Long): Unit =
      ps.setLong(index, value)
  }

  given PreparedStatementSetter[Float] = new PreparedStatementSetter[Float] {
    def set(ps: PreparedStatement)(index: Int)(value: Float): Unit =
      ps.setFloat(index, value)
  }

  given PreparedStatementSetter[Double] = new PreparedStatementSetter[Double] {
    def set(ps: PreparedStatement)(index: Int)(value: Double): Unit =
      ps.setDouble(index, value)
  }

  given PreparedStatementSetter[String] = new PreparedStatementSetter[String] {
    def set(ps: PreparedStatement)(index: Int)(value: String): Unit =
      ps.setString(index, value)
  }

  given PreparedStatementSetter[Boolean] =
    new PreparedStatementSetter[Boolean] {
      def set(ps: PreparedStatement)(index: Int)(value: Boolean): Unit =
        ps.setBoolean(index, value)
    }

}

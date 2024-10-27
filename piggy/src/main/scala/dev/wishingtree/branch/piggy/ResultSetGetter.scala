package dev.wishingtree.branch.piggy

import java.sql.ResultSet

trait ResultSetGetter[A] {
  def get(rs: ResultSet)(index: Int): A
}

object ResultSetGetter {

  given ResultSetGetter[Int] = new ResultSetGetter[Int] {
    def get(rs: ResultSet)(index: Int): Int = rs.getInt(index)
  }

  given ResultSetGetter[Long] = new ResultSetGetter[Long] {
    def get(rs: ResultSet)(index: Int): Long = rs.getLong(index)
  }

  given ResultSetGetter[Float] = new ResultSetGetter[Float] {
    def get(rs: ResultSet)(index: Int): Float = rs.getFloat(index)
  }

  given ResultSetGetter[Double] = new ResultSetGetter[Double] {
    def get(rs: ResultSet)(index: Int): Double = rs.getDouble(index)
  }

  given ResultSetGetter[String] = new ResultSetGetter[String] {
    def get(rs: ResultSet)(index: Int): String = rs.getString(index)
  }

  given ResultSetGetter[Boolean] = new ResultSetGetter[Boolean] {
    def get(rs: ResultSet)(index: Int): Boolean = rs.getBoolean(index)
  }

}

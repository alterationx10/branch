package dev.wishingtree.branch.piggy

import java.sql.{Connection, PreparedStatement, ResultSet}
import scala.compiletime.*

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

object Sql {

  private inline def parseRs[T <: Tuple](rs: ResultSet)(index: Int): Tuple =
    inline erasedValue[T] match
      case _: EmptyTuple =>
        EmptyTuple
      case _: (t *: ts)  =>
        summonInline[ResultSetGetter[t]].get(rs)(index) *: parseRs[ts](rs)(
          index + 1
        )

  private inline def setPs[A](ps: PreparedStatement)(index: Int)(
      value: A
  ): Unit =
    summonInline[PreparedStatementSetter[A]].set(ps)(index)(value)

  extension (rs: ResultSet) {

    inline def tupled[A <: Tuple]: A = {
      rs.next() // dangerous, should be handled
      parseRs[A](rs)(1).asInstanceOf[A]
    }

    inline def tupledList[A <: Tuple]: List[A] = {
      val b = List.newBuilder[A]
      while rs.next() do b += parseRs[A](rs)(1).asInstanceOf[A]
      b.result()
    }

  }

  extension (sc: StringContext) {
    def ps(args: Any*)(using connection: Connection): PreparedStatement = {
      val sql = sc.s(args.map(_ => "?")*)
      val ps  = connection.prepareStatement(sql)
      args.zipWithIndex.map({ case (a, i) => a -> (i + 1) }).foreach {
        case (a: Int, i)     => ps.setInt(i, a)
        case (a: Long, i)    => ps.setLong(i, a)
        case (a: Float, i)   => ps.setFloat(i, a)
        case (a: Double, i)  => ps.setDouble(i, a)
        case (a: String, i)  => ps.setString(i, a)
        case (a: Boolean, i) => ps.setBoolean(i, a)
        case _               => throw new IllegalArgumentException("Unsupported type")
      }
      ps
    }
  }

}

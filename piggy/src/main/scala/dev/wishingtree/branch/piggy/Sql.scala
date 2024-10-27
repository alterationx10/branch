package dev.wishingtree.branch.piggy

import java.sql.{Connection, PreparedStatement, ResultSet}
import scala.compiletime.*

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

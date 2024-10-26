package dev.wishingtree.branch.piggy

import java.sql.{Connection, PreparedStatement}

object Sql {

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

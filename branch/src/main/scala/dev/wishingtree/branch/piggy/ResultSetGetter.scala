package dev.wishingtree.branch.piggy

import java.sql.ResultSet

trait ResultSetGetter[A] {
  def get(rs: ResultSet, col: String | Int): A
}

object ResultSetGetter {

  given ResultSetGetter[String] with {
    override def get(rs: ResultSet, col: String | Int): String = {
      col match {
        case label: String => rs.getString(label)
        case index: Int    => rs.getString(index)
      }
    }
  }

  given ResultSetGetter[Int] with {
    override def get(rs: ResultSet, col: String | Int): Int = {
      col match {
        case label: String => rs.getInt(label)
        case index: Int    => rs.getInt(index)
      }
    }
  }

  given ResultSetGetter[Long] with {
    override def get(rs: ResultSet, col: String | Int): Long = {
      col match {
        case label: String => rs.getLong(label)
        case index: Int    => rs.getLong(index)
      }
    }
  }

  given ResultSetGetter[Float] with {
    override def get(rs: ResultSet, col: String | Int): Float = {
      col match {
        case label: String => rs.getFloat(label)
        case index: Int    => rs.getFloat(index)
      }
    }
  }

  given ResultSetGetter[Double] with {
    override def get(rs: ResultSet, col: String | Int): Double = {
      col match {
        case label: String => rs.getDouble(label)
        case index: Int    => rs.getDouble(index)
      }
    }
  }

  given ResultSetGetter[Boolean] with {
    override def get(rs: ResultSet, col: String | Int): Boolean = {
      col match {
        case label: String => rs.getBoolean(label)
        case index: Int    => rs.getBoolean(index)
      }
    }
  }

  given ResultSetGetter[BigDecimal] with {
    override def get(rs: ResultSet, col: String | Int): BigDecimal = {
      col match {
        case label: String => rs.getBigDecimal(label)
        case index: Int    => rs.getBigDecimal(index)
      }
    }
  }

  given ResultSetGetter[java.sql.Date] with {
    override def get(rs: ResultSet, col: String | Int): java.sql.Date = {
      col match {
        case label: String => rs.getDate(label)
        case index: Int    => rs.getDate(index)
      }
    }
  }

  given ResultSetGetter[java.sql.Timestamp] with {
    override def get(rs: ResultSet, col: String | Int): java.sql.Timestamp = {
      col match {
        case label: String => rs.getTimestamp(label)
        case index: Int    => rs.getTimestamp(index)
      }
    }
  }

  given ResultSetGetter[java.sql.Time] with {
    override def get(rs: ResultSet, col: String | Int): java.sql.Time = {
      col match {
        case label: String => rs.getTime(label)
        case index: Int    => rs.getTime(index)
      }
    }
  }

  given ResultSetGetter[Array[Byte]] with {
    override def get(rs: ResultSet, col: String | Int): Array[Byte] = {
      col match {
        case label: String => rs.getBytes(label)
        case index: Int    => rs.getBytes(index)
      }
    }
  }

  given ResultSetGetter[Short] with {
    override def get(rs: ResultSet, col: String | Int): Short = {
      col match {
        case label: String => rs.getShort(label)
        case index: Int    => rs.getShort(index)
      }
    }
  }

  given ResultSetGetter[Byte] with {
    override def get(rs: ResultSet, col: String | Int): Byte = {
      col match {
        case label: String => rs.getByte(label)
        case index: Int    => rs.getByte(index)
      }
    }
  }

}

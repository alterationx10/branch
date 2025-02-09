package dev.wishingtree.branch.piggy

import java.sql.ResultSet
import java.util.UUID
import scala.compiletime.summonInline

/** Trait for getting values from a `ResultSet` by column name or index.
  * @tparam A
  *   the type of the value to get from the `ResultSet`
  */
trait ResultSetGetter[A] { self =>

  /** Gets a value from the `ResultSet` by column name or index.
    * @param rs
    *   the `ResultSet` to get the value from
    * @param col
    *   the column name or index
    * @return
    *   the value of type `A` from the `ResultSet`
    */
  def get(rs: ResultSet, col: String | Int): A

  /** Maps a function over the value of type `A` obtained from the `ResultSet`.
    * @param f
    *   the function to map over the value of type `A`
    * @tparam B
    *   the type of the value to map to
    * @return
    *   a new `ResultSetGetter` instance for the value of type `B`
    */
  def map[B](f: A => B): ResultSetGetter[B] =
    (rs: ResultSet, col: String | Int) => f(self.get(rs, col))
}

object ResultSetGetter {

  /** `ResultSetGetter` instance for `String` values. */
  given ResultSetGetter[String] with {
    override def get(rs: ResultSet, col: String | Int): String = {
      col match {
        case label: String => rs.getString(label)
        case index: Int    => rs.getString(index)
      }
    }
  }

  /** `ResultSetGetter` instance for `UUID` values. */
  given ResultSetGetter[UUID] =
    summonInline[ResultSetGetter[String]].map(UUID.fromString)

  /** `ResultSetGetter` instance for `Int` values. */
  given ResultSetGetter[Int] with {
    override def get(rs: ResultSet, col: String | Int): Int = {
      col match {
        case label: String => rs.getInt(label)
        case index: Int    => rs.getInt(index)
      }
    }
  }

  /** `ResultSetGetter` instance for `Long` values. */
  given ResultSetGetter[Long] with {
    override def get(rs: ResultSet, col: String | Int): Long = {
      col match {
        case label: String => rs.getLong(label)
        case index: Int    => rs.getLong(index)
      }
    }
  }

  /** `ResultSetGetter` instance for `Float` values. */
  given ResultSetGetter[Float] with {
    override def get(rs: ResultSet, col: String | Int): Float = {
      col match {
        case label: String => rs.getFloat(label)
        case index: Int    => rs.getFloat(index)
      }
    }
  }

  /** `ResultSetGetter` instance for `Double` values. */
  given ResultSetGetter[Double] with {
    override def get(rs: ResultSet, col: String | Int): Double = {
      col match {
        case label: String => rs.getDouble(label)
        case index: Int    => rs.getDouble(index)
      }
    }
  }

  /** `ResultSetGetter` instance for `Boolean` values. */
  given ResultSetGetter[Boolean] with {
    override def get(rs: ResultSet, col: String | Int): Boolean = {
      col match {
        case label: String => rs.getBoolean(label)
        case index: Int    => rs.getBoolean(index)
      }
    }
  }

  /** `ResultSetGetter` instance for `BigDecimal` values. */
  given ResultSetGetter[BigDecimal] with {
    override def get(rs: ResultSet, col: String | Int): BigDecimal = {
      col match {
        case label: String => rs.getBigDecimal(label)
        case index: Int    => rs.getBigDecimal(index)
      }
    }
  }

  /** `ResultSetGetter` instance for `java.sql.Date` values. */
  given ResultSetGetter[java.sql.Date] with {
    override def get(rs: ResultSet, col: String | Int): java.sql.Date = {
      col match {
        case label: String => rs.getDate(label)
        case index: Int    => rs.getDate(index)
      }
    }
  }

  /** `ResultSetGetter` instance for `java.sql.Timestamp` values. */
  given ResultSetGetter[java.sql.Timestamp] with {
    override def get(rs: ResultSet, col: String | Int): java.sql.Timestamp = {
      col match {
        case label: String => rs.getTimestamp(label)
        case index: Int    => rs.getTimestamp(index)
      }
    }
  }

  /** `ResultSetGetter` instance for `java.time.Instant` values. */
  given ResultSetGetter[java.time.Instant] =
    summonInline[ResultSetGetter[java.sql.Timestamp]].map(_.toInstant)

  /** `ResultSetGetter` instance for `java.sql.Time` values. */
  given ResultSetGetter[java.sql.Time] with {
    override def get(rs: ResultSet, col: String | Int): java.sql.Time = {
      col match {
        case label: String => rs.getTime(label)
        case index: Int    => rs.getTime(index)
      }
    }
  }

  /** `ResultSetGetter` instance for `Array[Byte]` values. */
  given ResultSetGetter[Array[Byte]] with {
    override def get(rs: ResultSet, col: String | Int): Array[Byte] = {
      col match {
        case label: String => rs.getBytes(label)
        case index: Int    => rs.getBytes(index)
      }
    }
  }

  /** `ResultSetGetter` instance for `Short` values. */
  given ResultSetGetter[Short] with {
    override def get(rs: ResultSet, col: String | Int): Short = {
      col match {
        case label: String => rs.getShort(label)
        case index: Int    => rs.getShort(index)
      }
    }
  }

  /** `ResultSetGetter` instance for `Byte` values. */
  given ResultSetGetter[Byte] with {
    override def get(rs: ResultSet, col: String | Int): Byte = {
      col match {
        case label: String => rs.getByte(label)
        case index: Int    => rs.getByte(index)
      }
    }
  }

}

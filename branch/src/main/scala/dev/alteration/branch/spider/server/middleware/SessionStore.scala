package dev.alteration.branch.spider.server.middleware

import java.time.Instant
import scala.collection.concurrent.TrieMap

/** Interface for session storage backends.
  *
  * Implementations can provide different storage mechanisms (in-memory,
  * file-based, database, Redis, etc.)
  */
trait SessionStore {

  /** Save or update a session in the store.
    *
    * @param session
    *   The session to save
    */
  def save(session: Session): Unit

  /** Retrieve a session by ID.
    *
    * @param id
    *   The session ID
    * @return
    *   Some(session) if found and not expired, None otherwise
    */
  def get(id: String): Option[Session]

  /** Delete a session from the store.
    *
    * @param id
    *   The session ID to delete
    */
  def delete(id: String): Unit

  /** Remove all expired sessions from the store. */
  def cleanup(): Unit

  /** Check if a session exists (and is not expired).
    *
    * @param id
    *   The session ID
    * @return
    *   true if session exists and is valid
    */
  def exists(id: String): Boolean =
    get(id).isDefined
}

/** In-memory session store using a concurrent TrieMap.
  *
  * This is suitable for single-instance applications or development. For
  * production multi-instance deployments, use a distributed store like Redis.
  */
class InMemorySessionStore extends SessionStore {

  private val sessions: TrieMap[String, Session] = TrieMap.empty

  override def save(session: Session): Unit = {
    sessions.put(session.id, session)
    ()
  }

  override def get(id: String): Option[Session] = {
    sessions.get(id).filter(!_.isExpired)
  }

  override def delete(id: String): Unit = {
    sessions.remove(id)
    ()
  }

  override def cleanup(): Unit = {
    val expiredIds = sessions.collect {
      case (id, session) if session.isExpired => id
    }
    expiredIds.foreach(sessions.remove)
    ()
  }

  /** Get the number of sessions currently stored */
  def size: Int = sessions.size

  /** Clear all sessions (useful for testing) */
  def clear(): Unit = {
    sessions.clear()
    ()
  }
}

object InMemorySessionStore {
  def apply(): InMemorySessionStore = new InMemorySessionStore()
}

/** File-based session store that persists sessions to disk.
  *
  * This implementation is more durable than in-memory but still suitable for
  * single-instance applications. Sessions are stored as individual files in a
  * directory.
  *
  * @param directory
  *   The directory to store session files
  */
class FileSessionStore(directory: java.io.File) extends SessionStore {

  import java.io.{File, FileWriter}
  import scala.io.Source
  import scala.util.{Try, Using}

  // Ensure directory exists
  if (!directory.exists()) {
    directory.mkdirs()
  }

  override def save(session: Session): Unit = {
    val file = new File(directory, s"${session.id}.session")
    Try {
      Using(new FileWriter(file)) { writer =>
        // Simple serialization: one line per field
        writer.write(s"${session.id}\n")
        writer.write(s"${session.createdAt.toEpochMilli}\n")
        writer.write(s"${session.lastAccessedAt.toEpochMilli}\n")
        writer.write(s"${session.expiresAt.toEpochMilli}\n")
        writer.write(s"${session.data.size}\n")
        session.data.foreach { case (k, v) =>
          writer.write(s"$k\n$v\n")
        }
      }
    }
    ()
  }

  override def get(id: String): Option[Session] = {
    val file = new File(directory, s"$id.session")
    if (!file.exists()) {
      None
    } else {
      Try {
        Using(Source.fromFile(file)) { source =>
          val lines        = source.getLines().toList
          val id           = lines(0)
          val created      = Instant.ofEpochMilli(lines(1).toLong)
          val lastAccessed = Instant.ofEpochMilli(lines(2).toLong)
          val expires      = Instant.ofEpochMilli(lines(3).toLong)
          val dataSize     = lines(4).toInt

          val dataLines = lines.drop(5)
          val data      = (0 until dataSize).map { i =>
            val key   = dataLines(i * 2)
            val value = dataLines(i * 2 + 1)
            key -> value
          }.toMap

          Session(id, data, created, lastAccessed, expires)
        }.toOption
      }.toOption.flatten.filter(!_.isExpired)
    }
  }

  override def delete(id: String): Unit = {
    val file = new File(directory, s"$id.session")
    if (file.exists()) {
      file.delete()
      ()
    }
  }

  override def cleanup(): Unit = {
    Option(directory.listFiles()).foreach { files =>
      files.filter(_.getName.endsWith(".session")).foreach { file =>
        val id = file.getName.stripSuffix(".session")
        get(id) match {
          case Some(_) =>               // Session is valid, keep it
          case None    => file.delete() // Session expired or invalid, delete
        }
      }
    }
    ()
  }
}

object FileSessionStore {
  def apply(directory: java.io.File): FileSessionStore =
    new FileSessionStore(directory)

  def apply(directoryPath: String): FileSessionStore =
    new FileSessionStore(new java.io.File(directoryPath))
}

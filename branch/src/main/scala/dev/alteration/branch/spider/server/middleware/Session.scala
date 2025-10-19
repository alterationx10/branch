package dev.alteration.branch.spider.server.middleware

import java.time.Instant
import java.util.UUID

/** A session representing user state across HTTP requests.
  *
  * @param id
  *   Unique session identifier
  * @param data
  *   Session data stored as key-value pairs
  * @param createdAt
  *   When the session was first created
  * @param lastAccessedAt
  *   When the session was last accessed
  * @param expiresAt
  *   When the session expires
  */
case class Session(
    id: String,
    data: Map[String, String] = Map.empty,
    createdAt: Instant = Instant.now(),
    lastAccessedAt: Instant = Instant.now(),
    expiresAt: Instant
) {

  /** Check if the session has expired */
  def isExpired: Boolean = Instant.now().isAfter(expiresAt)

  /** Update the last accessed time to now */
  def touch: Session =
    copy(lastAccessedAt = Instant.now())

  /** Get a value from session data */
  def get(key: String): Option[String] =
    data.get(key)

  /** Set a value in session data */
  def set(key: String, value: String): Session =
    copy(data = data + (key -> value))

  /** Remove a value from session data */
  def remove(key: String): Session =
    copy(data = data - key)

  /** Clear all session data */
  def clear: Session =
    copy(data = Map.empty)

  /** Check if session contains a key */
  def contains(key: String): Boolean =
    data.contains(key)

  /** Regenerate the session ID (for security after privilege escalation) */
  def regenerateId: Session =
    copy(id = Session.generateId())

  /** Extend the session expiration by the given duration in seconds */
  def extend(durationSeconds: Long): Session =
    copy(expiresAt = Instant.now().plusSeconds(durationSeconds))
}

object Session {

  /** Generate a new cryptographically secure session ID */
  def generateId(): String =
    UUID.randomUUID().toString

  /** Create a new session with the given duration in seconds.
    *
    * @param durationSeconds
    *   How long the session should last
    * @return
    *   A new session instance
    */
  def create(durationSeconds: Long): Session = {
    val now = Instant.now()
    Session(
      id = generateId(),
      data = Map.empty,
      createdAt = now,
      lastAccessedAt = now,
      expiresAt = now.plusSeconds(durationSeconds)
    )
  }

  /** Create a new session with custom data and duration.
    *
    * @param data
    *   Initial session data
    * @param durationSeconds
    *   How long the session should last
    * @return
    *   A new session instance
    */
  def create(data: Map[String, String], durationSeconds: Long): Session = {
    val now = Instant.now()
    Session(
      id = generateId(),
      data = data,
      createdAt = now,
      lastAccessedAt = now,
      expiresAt = now.plusSeconds(durationSeconds)
    )
  }
}

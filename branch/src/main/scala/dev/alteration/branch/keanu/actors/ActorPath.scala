package dev.alteration.branch.keanu.actors

/** Represents a hierarchical path to an actor in the actor system.
  *
  * Actor paths follow a hierarchical structure similar to file systems, with
  * segments separated by forward slashes. All user actors live under the
  * `/user` root.
  *
  * Example paths:
  *   - `/user/myActor` - Top-level user actor
  *   - `/user/parent/child` - Child actor under parent
  *   - `/user/supervisor/worker1` - Worker under supervisor
  *
  * @param segments
  *   The path segments (e.g., List("user", "parent", "child"))
  */
case class ActorPath(segments: List[String]) {

  /** Creates a child path by appending a segment.
    *
    * @param child
    *   The child segment name
    * @return
    *   A new ActorPath with the child appended
    */
  def /(child: String): ActorPath = {
    require(child != null && child.nonEmpty, "Child name cannot be null or empty")
    require(!child.contains("/"), "Child name cannot contain '/'")
    ActorPath(segments :+ child)
  }

  /** Returns the parent path if this path has a parent.
    *
    * @return
    *   Some(parent path) or None if this is the root
    */
  def parent: Option[ActorPath] = segments.dropRight(1) match {
    case Nil  => None
    case segs => Some(ActorPath(segs))
  }

  /** Returns the name of this actor (the last segment).
    *
    * @return
    *   The actor name, or empty string if this is the root
    */
  def name: String = segments.lastOption.getOrElse("")

  /** Returns the string representation of this path.
    *
    * @return
    *   The path as a string (e.g., "/user/parent/child")
    */
  override def toString: String =
    if (segments.isEmpty) "/"
    else segments.mkString("/", "/", "")

  /** Returns whether this path is a descendant of the given path.
    *
    * @param other
    *   The potential ancestor path
    * @return
    *   true if this path is a descendant of the other path
    */
  def isDescendantOf(other: ActorPath): Boolean =
    segments.startsWith(other.segments) && segments.length > other.segments.length

  /** Returns whether this path is a direct child of the given path.
    *
    * @param other
    *   The potential parent path
    * @return
    *   true if this path is a direct child of the other path
    */
  def isChildOf(other: ActorPath): Boolean =
    segments.startsWith(other.segments) && segments.length == other.segments.length + 1
}

object ActorPath {

  /** The root path for all user actors. */
  val userRoot: ActorPath = ActorPath(List("user"))

  /** Creates an ActorPath from a string representation.
    *
    * @param path
    *   The path string (e.g., "/user/parent/child")
    * @return
    *   Some(ActorPath) if valid, None otherwise
    */
  def fromString(path: String): Option[ActorPath] = {
    if (path == null || path.isEmpty) return None

    val segments = path.split("/").filter(_.nonEmpty).toList
    if (segments.isEmpty) None
    else Some(ActorPath(segments))
  }

  /** Creates an ActorPath for a top-level user actor.
    *
    * @param name
    *   The actor name
    * @return
    *   An ActorPath under /user
    */
  def user(name: String): ActorPath = {
    require(name != null && name.nonEmpty, "Actor name cannot be null or empty")
    require(!name.contains("/"), "Actor name cannot contain '/'")
    userRoot / name
  }
}

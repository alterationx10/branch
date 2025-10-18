package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.server.{Cookie, Request, Response}

/** Middleware for session management.
  *
  * This middleware:
  *   - Loads sessions from cookies in preProcess
  *   - Stores the session in a thread-local context
  *   - Saves sessions back to cookies in postProcess
  *   - Handles session expiration and renewal
  *   - Supports session ID rotation for security
  *
  * @param config
  *   Session configuration
  * @param store
  *   Session storage backend
  */
class SessionMiddleware[I, O](
    config: SessionConfig,
    store: SessionStore
) extends Middleware[I, O] {

  /** Load session from cookie if present. */
  override def preProcess(
      request: Request[I]
  ): MiddlewareResult[Response[O], Request[I]] = {
    // Try to get session ID from cookie
    val sessionIdOpt = request.cookie(config.cookieName)

    sessionIdOpt match {
      case Some(sessionId) =>
        // Try to load session from store
        store.get(sessionId) match {
          case Some(session) =>
            // Session exists and is valid
            val updatedSession = if (config.slidingExpiration) {
              // Extend expiration on each access
              session.touch.extend(config.maxAge)
            } else {
              // Just update last accessed time
              session.touch
            }

            // Store in context for handler to access
            SessionContext.set(updatedSession)

            // Continue with request
            Continue(request)

          case None =>
            // Session not found or expired - continue without session
            SessionContext.clear()
            Continue(request)
        }

      case None =>
        // No session cookie - continue without session
        SessionContext.clear()
        Continue(request)
    }
  }

  /** Save session to cookie if present in context. */
  override def postProcess(
      request: Request[I],
      response: Response[O]
  ): Response[O] = {
    SessionContext.get() match {
      case Some(session) =>
        // Save session to store
        store.save(session)

        // Create session cookie
        val sessionCookie = Cookie(
          name = config.cookieName,
          value = session.id,
          path = Some(config.path),
          domain = config.domain,
          maxAge = Some(config.maxAge),
          secure = config.secure,
          httpOnly = config.httpOnly,
          sameSite = Some(config.sameSite)
        )

        // Add cookie to response
        response.withCookie(sessionCookie)

      case None =>
        // No session in context - return response as-is
        response
    }
  }
}

object SessionMiddleware {

  /** Create a session middleware with the given configuration and store.
    *
    * @param config
    *   Session configuration
    * @param store
    *   Session storage backend
    */
  def apply[I, O](
      config: SessionConfig,
      store: SessionStore
  ): SessionMiddleware[I, O] =
    new SessionMiddleware[I, O](config, store)

  /** Create a session middleware with default configuration and in-memory
    * store.
    */
  def default[I, O]: SessionMiddleware[I, O] =
    new SessionMiddleware[I, O](SessionConfig.default, InMemorySessionStore())

  /** Create a session middleware with development configuration and in-memory
    * store.
    */
  def development[I, O]: SessionMiddleware[I, O] =
    new SessionMiddleware[I, O](
      SessionConfig.development,
      InMemorySessionStore()
    )

  /** Create a session middleware with strict configuration and in-memory
    * store.
    */
  def strict[I, O]: SessionMiddleware[I, O] =
    new SessionMiddleware[I, O](SessionConfig.strict, InMemorySessionStore())
}

/** Thread-local storage for the current session.
  *
  * This allows handlers to access and modify the session without explicitly
  * passing it around.
  */
object SessionContext {

  private val context = new ThreadLocal[Option[Session]] {
    override def initialValue(): Option[Session] = None
  }

  /** Get the current session, if any. */
  def get(): Option[Session] = context.get()

  /** Set the current session. */
  def set(session: Session): Unit = context.set(Some(session))

  /** Clear the current session. */
  def clear(): Unit = context.set(None)

  /** Get the current session or create a new one.
    *
    * @param config
    *   Session configuration for creating new session
    */
  def getOrCreate(config: SessionConfig): Session = {
    get() match {
      case Some(session) => session
      case None =>
        val newSession = Session.create(config.maxAge)
        set(newSession)
        newSession
    }
  }

  /** Update the current session if present.
    *
    * @param f
    *   Function to transform the session
    */
  def update(f: Session => Session): Option[Session] = {
    get().map { session =>
      val updated = f(session)
      set(updated)
      updated
    }
  }

  /** Destroy the current session. */
  def destroy(): Unit = clear()

  /** Regenerate the session ID (for security after authentication).
    *
    * This creates a new session ID while preserving the session data.
    */
  def regenerateId(): Option[Session] = {
    update(_.regenerateId)
  }
}

/** Extension methods for working with sessions in requests and responses. */
object SessionExtensions {

  extension [I](request: Request[I]) {

    /** Get the current session from the context. */
    def session: Option[Session] = SessionContext.get()

    /** Get the current session or create a new one. */
    def getOrCreateSession(config: SessionConfig): Session =
      SessionContext.getOrCreate(config)

    /** Get a value from the session. */
    def sessionGet(key: String): Option[String] =
      SessionContext.get().flatMap(_.get(key))

    /** Set a value in the session. */
    def sessionSet(key: String, value: String): Option[Session] =
      SessionContext.update(_.set(key, value))

    /** Remove a value from the session. */
    def sessionRemove(key: String): Option[Session] =
      SessionContext.update(_.remove(key))

    /** Clear all session data. */
    def sessionClear(): Option[Session] =
      SessionContext.update(_.clear)

    /** Destroy the current session. */
    def sessionDestroy(): Unit =
      SessionContext.destroy()

    /** Regenerate session ID. */
    def sessionRegenerateId(): Option[Session] =
      SessionContext.regenerateId()
  }
}

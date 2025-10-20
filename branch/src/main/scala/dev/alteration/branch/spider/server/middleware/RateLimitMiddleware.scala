package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.server.{Request, Response}
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

/** Rate limiting algorithm. */
enum RateLimitAlgorithm {
  case TokenBucket
  case SlidingWindow
}

/** Configuration for rate limiting.
  *
  * @param maxRequests
  *   Maximum number of requests allowed per window
  * @param windowMillis
  *   Time window in milliseconds
  * @param algorithm
  *   Rate limiting algorithm to use
  * @param keyExtractor
  *   Function to extract the rate limit key from a request (e.g., IP address)
  */
case class RateLimitConfig(
    maxRequests: Int = 100,
    windowMillis: Long = 60000, // 1 minute
    algorithm: RateLimitAlgorithm = RateLimitAlgorithm.TokenBucket,
    keyExtractor: Request[?] => String = defaultKeyExtractor
)

/** Default key extractor uses X-Forwarded-For or remote address. */
private def defaultKeyExtractor(request: Request[?]): String = {
  request.headers
    .get("X-Forwarded-For")
    .flatMap(_.headOption)
    .map(_.split(",").head.trim)
    .getOrElse("unknown")
}

/** Token bucket for rate limiting. */
private class TokenBucket(maxTokens: Int, refillRatePerSecond: Double) {
  @volatile private var tokens: Double   = maxTokens.toDouble
  @volatile private var lastRefill: Long = System.nanoTime()

  def tryConsume(): Boolean = {
    refill()
    synchronized {
      if (tokens >= 1.0) {
        tokens -= 1.0
        true
      } else {
        false
      }
    }
  }

  def availableTokens: Int = {
    refill()
    tokens.toInt
  }

  private def refill(): Unit = {
    val now         = System.nanoTime()
    val elapsed     = (now - lastRefill) / 1_000_000_000.0 // Convert to seconds
    val tokensToAdd = elapsed * refillRatePerSecond

    synchronized {
      tokens = math.min(maxTokens.toDouble, tokens + tokensToAdd)
      lastRefill = now
    }
  }
}

/** Sliding window counter for rate limiting. */
private class SlidingWindow(maxRequests: Int, windowMillis: Long) {
  private val requests = new ConcurrentHashMap[Long, Int]()

  def tryConsume(): Boolean = {
    val now         = System.currentTimeMillis()
    val windowStart = now - windowMillis

    // Clean old entries
    requests
      .keySet()
      .asScala
      .filter(_ < windowStart)
      .foreach(requests.remove)

    // Count requests in current window
    val count = requests.values().asScala.sum

    if (count < maxRequests) {
      requests.put(now, 1)
      true
    } else {
      false
    }
  }

  def currentCount: Int = {
    val now         = System.currentTimeMillis()
    val windowStart = now - windowMillis

    // Clean old entries
    requests
      .keySet()
      .asScala
      .filter(_ < windowStart)
      .foreach(requests.remove)

    requests.values().asScala.sum
  }
}

/** Rate limiting middleware.
  *
  * Features:
  *   - Token bucket or sliding window algorithm
  *   - Per-IP or custom key rate limiting
  *   - Standard rate limit headers (X-RateLimit-*)
  *   - Configurable limits and windows
  *
  * Example:
  * {{{
  * // 100 requests per minute per IP
  * val rateLimiter = RateLimitMiddleware(
  *   RateLimitConfig(maxRequests = 100, windowMillis = 60000)
  * )
  *
  * // Per-user rate limiting
  * val userRateLimiter = RateLimitMiddleware(
  *   RateLimitConfig(
  *     maxRequests = 1000,
  *     windowMillis = 3600000, // 1 hour
  *     keyExtractor = req => req.headers.get("X-User-ID").flatMap(_.headOption).getOrElse("anonymous")
  *   )
  * )
  * }}}
  */
case class RateLimitMiddleware[I, O](
    config: RateLimitConfig,
    errorBody: O
) extends Middleware[I, O] {

  private val buckets = new ConcurrentHashMap[String, TokenBucket]()
  private val windows = new ConcurrentHashMap[String, SlidingWindow]()

  override def preProcess(
      request: Request[I]
  ): MiddlewareResult[Response[O], Request[I]] = {
    val key = config.keyExtractor(request)

    val allowed = config.algorithm match {
      case RateLimitAlgorithm.TokenBucket =>
        val bucket = buckets.computeIfAbsent(
          key,
          _ => {
            val refillRate =
              config.maxRequests.toDouble / (config.windowMillis / 1000.0)
            new TokenBucket(config.maxRequests, refillRate)
          }
        )
        bucket.tryConsume()

      case RateLimitAlgorithm.SlidingWindow =>
        val window = windows.computeIfAbsent(
          key,
          _ => new SlidingWindow(config.maxRequests, config.windowMillis)
        )
        window.tryConsume()
    }

    if (allowed) {
      Continue(request)
    } else {
      // Rate limit exceeded - return 429 Too Many Requests
      val response = Response[O](
        statusCode = 429,
        headers = Map(
          "X-RateLimit-Limit"     -> List(config.maxRequests.toString),
          "X-RateLimit-Remaining" -> List("0"),
          "X-RateLimit-Reset"     -> List(
            (System.currentTimeMillis() + config.windowMillis).toString
          ),
          "Retry-After"           -> List((config.windowMillis / 1000).toString),
          "Content-Type"          -> List("application/json")
        ),
        body = errorBody
      )
      Respond(response)
    }
  }

  override def postProcess(
      request: Request[I],
      response: Response[O]
  ): Response[O] = {
    val key = config.keyExtractor(request)

    // Add rate limit headers to successful responses
    val (remaining, limit) = config.algorithm match {
      case RateLimitAlgorithm.TokenBucket =>
        val bucket = buckets.get(key)
        if (bucket != null) {
          (bucket.availableTokens, config.maxRequests)
        } else {
          (config.maxRequests, config.maxRequests)
        }

      case RateLimitAlgorithm.SlidingWindow =>
        val window = windows.get(key)
        if (window != null) {
          (config.maxRequests - window.currentCount, config.maxRequests)
        } else {
          (config.maxRequests, config.maxRequests)
        }
    }

    val resetTime = System.currentTimeMillis() + config.windowMillis

    response
      .withHeader("X-RateLimit-Limit", limit.toString)
      .withHeader("X-RateLimit-Remaining", remaining.toString)
      .withHeader("X-RateLimit-Reset", resetTime.toString)
  }

  /** Clean up old buckets/windows (for memory management). */
  def cleanup(): Unit = {
    buckets.clear()
    windows.clear()
  }
}

object RateLimitMiddleware {

  /** Create a rate limiter with default config (100 req/min per IP). For
    * Array[Byte] responses, use the default error body.
    */
  def forBytes(
      config: RateLimitConfig = RateLimitConfig()
  ): RateLimitMiddleware[Array[Byte], Array[Byte]] = {
    val errorBody =
      """{"error":"Rate limit exceeded","status":429}""".getBytes("UTF-8")
    RateLimitMiddleware(config, errorBody)
  }

  /** Create a per-IP rate limiter with custom limits for Array[Byte] responses.
    */
  def perIp(
      maxRequests: Int,
      windowMillis: Long
  ): RateLimitMiddleware[Array[Byte], Array[Byte]] = {
    val errorBody =
      """{"error":"Rate limit exceeded","status":429}""".getBytes("UTF-8")
    RateLimitMiddleware(
      RateLimitConfig(
        maxRequests = maxRequests,
        windowMillis = windowMillis
      ),
      errorBody
    )
  }

  /** Create a rate limiter with custom key extraction for Array[Byte]
    * responses.
    */
  def withKeyExtractor(
      maxRequests: Int,
      windowMillis: Long,
      keyExtractor: Request[?] => String
  ): RateLimitMiddleware[Array[Byte], Array[Byte]] = {
    val errorBody =
      """{"error":"Rate limit exceeded","status":429}""".getBytes("UTF-8")
    RateLimitMiddleware(
      RateLimitConfig(
        maxRequests = maxRequests,
        windowMillis = windowMillis,
        keyExtractor = keyExtractor
      ),
      errorBody
    )
  }

  /** Create a rate limiter using sliding window algorithm for Array[Byte]
    * responses.
    */
  def withSlidingWindow(
      maxRequests: Int,
      windowMillis: Long
  ): RateLimitMiddleware[Array[Byte], Array[Byte]] = {
    val errorBody =
      """{"error":"Rate limit exceeded","status":429}""".getBytes("UTF-8")
    RateLimitMiddleware(
      RateLimitConfig(
        maxRequests = maxRequests,
        windowMillis = windowMillis,
        algorithm = RateLimitAlgorithm.SlidingWindow
      ),
      errorBody
    )
  }
}

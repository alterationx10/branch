package dev.alteration.branch.spider.server

/** Configuration for SocketServer hardening and limits.
  *
  * These settings help protect against:
  *   - DoS attacks (large requests, slow clients)
  *   - Resource exhaustion (too many headers, large bodies)
  *   - Malformed requests
  *
  * @param maxRequestLineLength
  *   Maximum length of the HTTP request line (e.g., "GET /path HTTP/1.1").
  *   Default: 8KB. Protects against extremely long URLs.
  * @param maxHeaderCount
  *   Maximum number of HTTP headers allowed. Default: 100. Prevents header
  *   flooding.
  * @param maxHeaderSize
  *   Maximum size of a single header value in bytes. Default: 8KB. Prevents
  *   header overflow attacks.
  * @param maxTotalHeadersSize
  *   Maximum total size of all headers combined in bytes. Default: 64KB.
  * @param maxRequestBodySize
  *   Maximum size of request body in bytes. Default: 10MB. Set to None for
  *   unlimited (not recommended for production).
  * @param socketTimeout
  *   Socket read timeout in milliseconds. Default: 30 seconds. Prevents slow
  *   client attacks.
  * @param requestTimeout
  *   Overall timeout for processing a single request in milliseconds. Default:
  *   60 seconds.
  * @param enableChunkedEncoding
  *   Enable support for chunked transfer encoding. Default: true.
  * @param enableKeepAlive
  *   Enable HTTP/1.1 keep-alive connections. Default: true.
  * @param maxKeepAliveRequests
  *   Maximum number of requests per keep-alive connection. Default: 100. Set
  *   to None for unlimited.
  */
case class ServerConfig(
    maxRequestLineLength: Int = 8192, // 8 KB
    maxHeaderCount: Int = 100,
    maxHeaderSize: Int = 8192, // 8 KB
    maxTotalHeadersSize: Int = 65536, // 64 KB
    maxRequestBodySize: Option[Long] = Some(10L * 1024 * 1024), // 10 MB
    socketTimeout: Int = 30000, // 30 seconds
    requestTimeout: Int = 60000, // 60 seconds
    enableChunkedEncoding: Boolean = true,
    enableKeepAlive: Boolean = true,
    maxKeepAliveRequests: Option[Int] = Some(100)
)

object ServerConfig {

  /** Default production configuration with reasonable limits. */
  def default: ServerConfig = ServerConfig()

  /** Permissive configuration for development/testing.
    *
    * Higher limits and longer timeouts for easier debugging.
    */
  def development: ServerConfig = ServerConfig(
    maxRequestLineLength = 16384, // 16 KB
    maxHeaderCount = 200,
    maxHeaderSize = 16384, // 16 KB
    maxTotalHeadersSize = 131072, // 128 KB
    maxRequestBodySize = Some(100L * 1024 * 1024), // 100 MB
    socketTimeout = 120000, // 2 minutes
    requestTimeout = 300000, // 5 minutes
    maxKeepAliveRequests = None // unlimited
  )

  /** Strict configuration for high-security environments.
    *
    * Lower limits and shorter timeouts to minimize attack surface.
    */
  def strict: ServerConfig = ServerConfig(
    maxRequestLineLength = 4096, // 4 KB
    maxHeaderCount = 50,
    maxHeaderSize = 4096, // 4 KB
    maxTotalHeadersSize = 32768, // 32 KB
    maxRequestBodySize = Some(1L * 1024 * 1024), // 1 MB
    socketTimeout = 10000, // 10 seconds
    requestTimeout = 30000, // 30 seconds
    maxKeepAliveRequests = Some(10)
  )
}

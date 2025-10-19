package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.middleware.CorsConfig.*

class CorsConfigSpec extends munit.FunSuite {

  test("CorsConfig default values") {
    val config = CorsConfig()
    assertEquals(config.allowedOrigins, Set("*"))
    assert(config.allowedMethods.contains(HttpMethod.GET))
    assert(config.allowedMethods.contains(HttpMethod.POST))
    assertEquals(config.allowedHeaders, Set("*"))
    assertEquals(config.allowCredentials, false)
    assertEquals(config.maxAge, Some(3600L))
  }

  test("CorsConfig.permissive allows all") {
    val config = CorsConfig.permissive
    assertEquals(config.allowedOrigins, Set("*"))
    assertEquals(config.allowedMethods, HttpMethod.values.toSet)
    assertEquals(config.allowedHeaders, Set("*"))
    assertEquals(config.allowCredentials, false)
  }

  test("CorsConfig.restrictive is restrictive") {
    val config = CorsConfig.restrictive
    assertEquals(config.allowedOrigins, Set.empty)
    assertEquals(config.allowedMethods, Set(HttpMethod.GET))
    assert(config.allowedHeaders.contains("Content-Type"))
    assertEquals(config.allowCredentials, false)
  }

  test("withOrigins sets allowed origins") {
    val config =
      CorsConfig().withOrigins("https://example.com", "https://test.com")
    assertEquals(
      config.allowedOrigins,
      Set("https://example.com", "https://test.com")
    )
  }

  test("withMethods sets allowed methods") {
    val config = CorsConfig().withMethods(HttpMethod.GET, HttpMethod.POST)
    assertEquals(config.allowedMethods, Set(HttpMethod.GET, HttpMethod.POST))
  }

  test("withHeaders sets allowed headers") {
    val config = CorsConfig().withHeaders("Content-Type", "Authorization")
    assertEquals(config.allowedHeaders, Set("Content-Type", "Authorization"))
  }

  test("withExposedHeaders sets exposed headers") {
    val config = CorsConfig().withExposedHeaders("X-Custom-Header")
    assertEquals(config.exposedHeaders, Set("X-Custom-Header"))
  }

  test("withCredentials enables credentials") {
    val config = CorsConfig().withCredentials
    assertEquals(config.allowCredentials, true)
  }

  test("withoutCredentials disables credentials") {
    val config = CorsConfig().withCredentials.withoutCredentials
    assertEquals(config.allowCredentials, false)
  }

  test("withMaxAge sets max age") {
    val config = CorsConfig().withMaxAge(7200)
    assertEquals(config.maxAge, Some(7200L))
  }

  test("withoutMaxAge disables max age") {
    val config = CorsConfig().withoutMaxAge
    assertEquals(config.maxAge, None)
  }

  test("isOriginAllowed with wildcard allows any origin") {
    val config = CorsConfig(allowedOrigins = Set("*"))
    assert(config.isOriginAllowed("https://example.com"))
    assert(config.isOriginAllowed("https://anything.com"))
  }

  test("isOriginAllowed with specific origins") {
    val config = CorsConfig(allowedOrigins = Set("https://example.com"))
    assert(config.isOriginAllowed("https://example.com"))
    assert(!config.isOriginAllowed("https://other.com"))
  }

  test("isMethodAllowed checks method") {
    val config = CorsConfig(allowedMethods = Set(HttpMethod.GET, HttpMethod.POST))
    assert(config.isMethodAllowed(HttpMethod.GET))
    assert(config.isMethodAllowed(HttpMethod.POST))
    assert(!config.isMethodAllowed(HttpMethod.DELETE))
  }

  test("areHeadersAllowed with wildcard allows any headers") {
    val config = CorsConfig(allowedHeaders = Set("*"))
    assert(config.areHeadersAllowed(List("Content-Type", "Authorization")))
    assert(config.areHeadersAllowed(List("X-Custom-Header")))
  }

  test("areHeadersAllowed with specific headers") {
    val config = CorsConfig(allowedHeaders = Set("content-type", "authorization"))
    assert(config.areHeadersAllowed(List("Content-Type")))
    assert(config.areHeadersAllowed(List("Authorization")))
    assert(config.areHeadersAllowed(List("Content-Type", "Authorization")))
    assert(!config.areHeadersAllowed(List("X-Custom-Header")))
  }

  test("areHeadersAllowed is case-insensitive") {
    val config = CorsConfig(allowedHeaders = Set("content-type"))
    assert(config.areHeadersAllowed(List("Content-Type")))
    assert(config.areHeadersAllowed(List("CONTENT-TYPE")))
    assert(config.areHeadersAllowed(List("content-type")))
  }

  test("builder pattern chain") {
    val config = CorsConfig.restrictive
      .withOrigins("https://example.com")
      .withMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT)
      .withHeaders("Content-Type", "Authorization")
      .withCredentials
      .withMaxAge(7200)

    assertEquals(config.allowedOrigins, Set("https://example.com"))
    assertEquals(
      config.allowedMethods,
      Set(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT)
    )
    assertEquals(config.allowedHeaders, Set("Content-Type", "Authorization"))
    assertEquals(config.allowCredentials, true)
    assertEquals(config.maxAge, Some(7200L))
  }

}

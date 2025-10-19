package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.Cookie

class CsrfConfigSpec extends munit.FunSuite {

  test("CsrfConfig default values") {
    val config = CsrfConfig.default
    assertEquals(config.tokenLength, 32)
    assertEquals(config.cookieName, "XSRF-TOKEN")
    assertEquals(config.headerName, "X-XSRF-TOKEN")
    assertEquals(config.fieldName, "csrf_token")
    assert(config.exemptMethods.contains(HttpMethod.GET))
    assert(config.exemptMethods.contains(HttpMethod.HEAD))
    assert(config.exemptMethods.contains(HttpMethod.OPTIONS))
    assertEquals(config.cookieSecure, true)
    assertEquals(config.cookieHttpOnly, false)
  }

  test("CsrfConfig.strict has stronger settings") {
    val config = CsrfConfig.strict
    assertEquals(config.tokenLength, 64)
    assertEquals(config.cookieSecure, true)
    assertEquals(config.cookieSameSite, Some(Cookie.SameSite.Strict))
  }

  test("CsrfConfig.development has relaxed settings") {
    val config = CsrfConfig.development
    assertEquals(config.cookieSecure, false)
    assertEquals(config.cookieSameSite, Some(Cookie.SameSite.Lax))
  }

  test("withTokenLength sets token length") {
    val config = CsrfConfig.default.withTokenLength(64)
    assertEquals(config.tokenLength, 64)
  }

  test("withCookieName sets cookie name") {
    val config = CsrfConfig.default.withCookieName("CUSTOM-CSRF")
    assertEquals(config.cookieName, "CUSTOM-CSRF")
  }

  test("withHeaderName sets header name") {
    val config = CsrfConfig.default.withHeaderName("X-CUSTOM-CSRF")
    assertEquals(config.headerName, "X-CUSTOM-CSRF")
  }

  test("withFieldName sets form field name") {
    val config = CsrfConfig.default.withFieldName("custom_csrf")
    assertEquals(config.fieldName, "custom_csrf")
  }

  test("withExemptMethods sets exempt methods") {
    val config = CsrfConfig.default.withExemptMethods(HttpMethod.GET)
    assertEquals(config.exemptMethods, Set(HttpMethod.GET))
  }

  test("withExemptPaths adds exempt paths") {
    val config = CsrfConfig.default
      .withExemptPaths("/api/public")
      .withExemptPaths("/api/webhook")
    assert(config.exemptPaths.contains("/api/public"))
    assert(config.exemptPaths.contains("/api/webhook"))
  }

  test("withExemptPathsOnly replaces exempt paths") {
    val config = CsrfConfig.default
      .withExemptPaths("/api/old")
      .withExemptPathsOnly("/api/new")
    assert(!config.exemptPaths.contains("/api/old"))
    assert(config.exemptPaths.contains("/api/new"))
  }

  test("withSecureCookie enables secure") {
    val config = CsrfConfig.default.withoutSecureCookie.withSecureCookie
    assertEquals(config.cookieSecure, true)
  }

  test("withoutSecureCookie disables secure") {
    val config = CsrfConfig.default.withoutSecureCookie
    assertEquals(config.cookieSecure, false)
  }

  test("withSameSite sets SameSite attribute") {
    val config = CsrfConfig.default.withSameSite(Cookie.SameSite.Lax)
    assertEquals(config.cookieSameSite, Some(Cookie.SameSite.Lax))
  }

  test("withoutSameSite removes SameSite attribute") {
    val config = CsrfConfig.default.withoutSameSite
    assertEquals(config.cookieSameSite, None)
  }

  test("isPathExempt checks exact path match") {
    val config = CsrfConfig.default.withExemptPaths("/api/public")
    assert(config.isPathExempt("/api/public"))
    assert(!config.isPathExempt("/api/private"))
  }

  test("isPathExempt supports wildcard patterns") {
    val config = CsrfConfig.default.withExemptPaths("/api/public/*")
    assert(config.isPathExempt("/api/public/users"))
    assert(config.isPathExempt("/api/public/posts"))
    assert(!config.isPathExempt("/api/private/users"))
  }

  test("isPathExempt supports prefix wildcard") {
    val config = CsrfConfig.default.withExemptPaths("/webhook*")
    assert(config.isPathExempt("/webhook"))
    assert(config.isPathExempt("/webhooks"))
    assert(config.isPathExempt("/webhook/github"))
  }

  test("isMethodExempt checks method") {
    val config = CsrfConfig.default
    assert(config.isMethodExempt(HttpMethod.GET))
    assert(config.isMethodExempt(HttpMethod.HEAD))
    assert(config.isMethodExempt(HttpMethod.OPTIONS))
    assert(!config.isMethodExempt(HttpMethod.POST))
    assert(!config.isMethodExempt(HttpMethod.PUT))
    assert(!config.isMethodExempt(HttpMethod.DELETE))
  }

  test("builder pattern chain") {
    val config = CsrfConfig.default
      .withTokenLength(64)
      .withCookieName("MY-CSRF")
      .withHeaderName("X-MY-CSRF")
      .withExemptPaths("/api/public/*")
      .withExemptMethods(HttpMethod.GET, HttpMethod.HEAD)
      .withoutSecureCookie
      .withSameSite(Cookie.SameSite.Lax)

    assertEquals(config.tokenLength, 64)
    assertEquals(config.cookieName, "MY-CSRF")
    assertEquals(config.headerName, "X-MY-CSRF")
    assert(config.isPathExempt("/api/public/users"))
    assertEquals(config.exemptMethods, Set(HttpMethod.GET, HttpMethod.HEAD))
    assertEquals(config.cookieSecure, false)
    assertEquals(config.cookieSameSite, Some(Cookie.SameSite.Lax))
  }

}

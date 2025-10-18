package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.server.Cookie

class SessionConfigSpec extends munit.FunSuite {

  test("SessionConfig.default has secure defaults") {
    val config = SessionConfig.default

    assertEquals(config.cookieName, "SESSION_ID")
    assertEquals(config.maxAge, 3600L)
    assertEquals(config.secure, true)
    assertEquals(config.httpOnly, true)
    assertEquals(config.sameSite, Cookie.SameSite.Strict)
    assertEquals(config.path, "/")
    assertEquals(config.rotateOnAuth, true)
    assertEquals(config.slidingExpiration, true)
  }

  test("SessionConfig.development has relaxed security") {
    val config = SessionConfig.development

    assertEquals(config.secure, false)
    assertEquals(config.sameSite, Cookie.SameSite.Lax)
    assertEquals(config.maxAge, 7200L)
  }

  test("SessionConfig.strict has maximum security") {
    val config = SessionConfig.strict

    assertEquals(config.secure, true)
    assertEquals(config.httpOnly, true)
    assertEquals(config.sameSite, Cookie.SameSite.Strict)
    assertEquals(config.rotateOnAuth, true)
    assertEquals(config.slidingExpiration, false)
    assertEquals(config.maxAge, 1800L)
  }

  test("SessionConfig.longLived has extended duration") {
    val config = SessionConfig.longLived

    assertEquals(config.maxAge, 2592000L) // 30 days
    assertEquals(config.slidingExpiration, true)
  }

  test("SessionConfig can be customized") {
    val config = SessionConfig(
      cookieName = "CUSTOM_SESSION",
      maxAge = 1200,
      secure = false,
      httpOnly = false,
      sameSite = Cookie.SameSite.None,
      path = "/app",
      domain = Some("example.com"),
      rotateOnAuth = false,
      slidingExpiration = false
    )

    assertEquals(config.cookieName, "CUSTOM_SESSION")
    assertEquals(config.maxAge, 1200L)
    assertEquals(config.secure, false)
    assertEquals(config.httpOnly, false)
    assertEquals(config.sameSite, Cookie.SameSite.None)
    assertEquals(config.path, "/app")
    assertEquals(config.domain, Some("example.com"))
    assertEquals(config.rotateOnAuth, false)
    assertEquals(config.slidingExpiration, false)
  }
}

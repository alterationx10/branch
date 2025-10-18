package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.macaroni.typeclasses.*
import dev.alteration.branch.spider.server.{Request, Response}
import munit.FunSuite

import java.net.URI

class MiddlewareResultSpec extends FunSuite {

  val testRequest: Request[String] = Request(
    URI.create("http://localhost:9000/test"),
    Map("Content-Type" -> List("text/plain")),
    "test body"
  )

  val testResponse: Response[String] = Response(
    200,
    "test response",
    Map("Content-Type" -> List("text/plain"))
  )

  test("Continue should create a Continue result") {
    val result = Continue(testRequest)
    assert(result.isContinue)
    assert(!result.isRespond)
    assertEquals(result.getRequest, Some(testRequest))
    assertEquals(result.getResponse, None)
  }

  test("Respond should create a Respond result") {
    val result = Respond(testResponse)
    assert(result.isRespond)
    assert(!result.isContinue)
    assertEquals(result.getResponse, Some(testResponse))
    assertEquals(result.getRequest, None)
  }

  test("Bifunctor - bimap over Continue") {
    val result    = Continue(testRequest)
    val bifunctor = summon[Bifunctor[MiddlewareResult]]

    val mapped = bifunctor.bimap(result)(
      (resp: Response[String]) => resp.copy(statusCode = 500),
      (req: Request[String]) => req.copy(body = "modified")
    )

    assert(mapped.isContinue)
    assertEquals(mapped.getRequest.map(_.body), Some("modified"))
  }

  test("Bifunctor - bimap over Respond") {
    val result    = Respond(testResponse)
    val bifunctor = summon[Bifunctor[MiddlewareResult]]

    val mapped = bifunctor.bimap(result)(
      (resp: Response[String]) => resp.copy(statusCode = 404),
      (req: Request[String]) => req.copy(body = "should not change")
    )

    assert(mapped.isRespond)
    assertEquals(mapped.getResponse.map(_.statusCode), Some(404))
  }

  test("Bifunctor - leftMap") {
    val result    = Respond(testResponse)
    val bifunctor = summon[Bifunctor[MiddlewareResult]]

    val mapped = bifunctor.leftMap(result)(resp => resp.copy(statusCode = 500))

    assert(mapped.isRespond)
    assertEquals(mapped.getResponse.map(_.statusCode), Some(500))
  }

  test("Bifunctor - rightMap") {
    val result    = Continue(testRequest)
    val bifunctor = summon[Bifunctor[MiddlewareResult]]

    val mapped = bifunctor.rightMap(result)(req => req.copy(body = "new body"))

    assert(mapped.isContinue)
    assertEquals(mapped.getRequest.map(_.body), Some("new body"))
  }

  test("Functor - map over Continue") {
    val result: MiddlewareResult[Response[String], Request[String]] =
      Continue(testRequest)
    val functor                                                     =
      summon[Functor[[Req] =>> MiddlewareResult[Response[String], Req]]]

    val mapped = functor.map(result)(req => req.copy(body = "mapped"))

    assert(mapped.isContinue)
    assertEquals(mapped.getRequest.map(_.body), Some("mapped"))
  }

  test("Functor - map over Respond should not affect response") {
    val result: MiddlewareResult[Response[String], Request[String]] =
      Respond(testResponse)
    val functor                                                     =
      summon[Functor[[Req] =>> MiddlewareResult[Response[String], Req]]]

    val mapped =
      functor.map(result)(req => req.copy(body = "should not change"))

    assert(mapped.isRespond)
    assertEquals(mapped.getResponse, Some(testResponse))
  }

  test("Monad - pure") {
    val monad  = summon[Monad[[Req] =>> MiddlewareResult[Response[String], Req]]]
    val result = monad.pure(testRequest)

    assert(result.isContinue)
    assertEquals(result.getRequest, Some(testRequest))
  }

  test("Monad - flatMap with Continue -> Continue") {
    val monad  = summon[Monad[[Req] =>> MiddlewareResult[Response[String], Req]]]
    val result = monad.flatMap(Continue(testRequest))(req =>
      Continue(req.copy(body = "modified"))
    )

    assert(result.isContinue)
    assertEquals(result.getRequest.map(_.body), Some("modified"))
  }

  test("Monad - flatMap with Continue -> Respond (short-circuit)") {
    val monad  = summon[Monad[[Req] =>> MiddlewareResult[Response[String], Req]]]
    val result =
      monad.flatMap(Continue(testRequest))(_ => Respond(testResponse))

    assert(result.isRespond)
    assertEquals(result.getResponse, Some(testResponse))
  }

  test("Monad - flatMap with Respond should short-circuit") {
    val monad  = summon[Monad[[Req] =>> MiddlewareResult[Response[String], Req]]]
    val result = monad.flatMap(Respond(testResponse))((req: Request[String]) =>
      Continue(req.copy(body = "should not run"))
    )

    assert(result.isRespond)
    assertEquals(result.getResponse, Some(testResponse))
  }

  test("Applicative - ap with Continue and Continue") {
    val monad = summon[Monad[[Req] =>> MiddlewareResult[Response[String], Req]]]
    val ff: MiddlewareResult[Response[String], Request[String] => Request[
      String
    ]] =
      Continue(req => req.copy(body = "modified"))
    val fa    = Continue(testRequest)

    val result = monad.ap(ff)(fa)

    assert(result.isContinue)
    assertEquals(result.getRequest.map(_.body), Some("modified"))
  }

  test("Applicative - ap with Respond should short-circuit") {
    val monad = summon[Monad[[Req] =>> MiddlewareResult[Response[String], Req]]]
    val ff: MiddlewareResult[Response[String], Request[String] => Request[
      String
    ]] =
      Respond(testResponse)
    val fa    = Continue(testRequest)

    val result = monad.ap(ff)(fa)

    assert(result.isRespond)
    assertEquals(result.getResponse, Some(testResponse))
  }

  test("Semigroup - combine Continue with Continue") {
    val semigroup =
      summon[Semigroup[MiddlewareResult[Response[String], Request[String]]]]
    val result1   = Continue(testRequest)
    val result2   = Continue(testRequest.copy(body = "second"))

    val combined = semigroup.combine(result1, result2)

    assert(combined.isContinue)
    assertEquals(combined.getRequest.map(_.body), Some("second"))
  }

  test("Semigroup - combine Respond with Continue (Respond wins)") {
    val semigroup =
      summon[Semigroup[MiddlewareResult[Response[String], Request[String]]]]
    val result1   = Respond(testResponse)
    val result2   = Continue(testRequest)

    val combined = semigroup.combine(result1, result2)

    assert(combined.isRespond)
    assertEquals(combined.getResponse, Some(testResponse))
  }

  test("Semigroup - combine Continue with Respond") {
    val semigroup =
      summon[Semigroup[MiddlewareResult[Response[String], Request[String]]]]
    val result1   = Continue(testRequest)
    val result2   = Respond(testResponse)

    val combined = semigroup.combine(result1, result2)

    assert(combined.isRespond)
    assertEquals(combined.getResponse, Some(testResponse))
  }

  test("Semigroup - |+| operator") {
    val semigroup                                                    =
      summon[Semigroup[MiddlewareResult[Response[String], Request[String]]]]
    val result1: MiddlewareResult[Response[String], Request[String]] =
      Continue(testRequest)
    val result2: MiddlewareResult[Response[String], Request[String]] =
      Respond(testResponse)

    val combined = semigroup.combine(result1, result2)

    assert(combined.isRespond)
  }

  test("extension method - mapRequest") {
    val result = Continue(testRequest)
    val mapped = result.mapRequest(req => req.copy(body = "mapped"))

    assert(mapped.isContinue)
    assertEquals(mapped.getRequest.map(_.body), Some("mapped"))
  }

  test("extension method - mapResponse") {
    val result = Respond(testResponse)
    val mapped = result.mapResponse(resp => resp.copy(statusCode = 404))

    assert(mapped.isRespond)
    assertEquals(mapped.getResponse.map(_.statusCode), Some(404))
  }

  test("extension method - mapRequest on Respond should not change") {
    val result = Respond(testResponse)
    val mapped = result.mapRequest((req: Request[String]) =>
      req.copy(body = "should not change")
    )

    assert(mapped.isRespond)
    assertEquals(mapped.getResponse, Some(testResponse))
  }

  test("extension method - mapResponse on Continue should not change") {
    val result = Continue(testRequest)
    val mapped = result.mapResponse((resp: Response[String]) =>
      resp.copy(statusCode = 500)
    )

    assert(mapped.isContinue)
    assertEquals(mapped.getRequest, Some(testRequest))
  }
}

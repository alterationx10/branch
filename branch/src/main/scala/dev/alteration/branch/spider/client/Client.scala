package dev.alteration.branch.spider.client

import java.net.http.HttpClient

/** A helper object with HttpClient utilities */
object Client {

  /** A default builder for creating a new HttpClient.
    */
  def builder: HttpClient.Builder =
    HttpClient.newBuilder()

  /** A method to build a new HttpClient with the given settings.
    */
  def build(settings: HttpClient.Builder => HttpClient.Builder*): HttpClient =
    settings
      .foldLeft(builder)((b, s) => s(b))
      .build()

  val defaultClient: HttpClient =
    builder.build()
    
}

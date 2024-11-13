package dev.wishingtree.branch.spider.client

import java.net.http.HttpClient

object Client {

  def builder: HttpClient.Builder =
    HttpClient.newBuilder()

  def build(settings: HttpClient.Builder => HttpClient.Builder*): HttpClient =
    settings
      .foldLeft(builder)((b, s) => s(b))
      .build()

}

package dev.alteration.branch.friday.http

import dev.alteration.branch.friday.{JsonDecoder, JsonEncoder}

object JsonConversions {

  /** A Conversion[Array[Byte], A], useful for marshalling request bodies in
    * spider
    */
  def convertFromBytes[A](using
      decoder: JsonDecoder[A]
  ): Conversion[Array[Byte], A] =
    (bytes: Array[Byte]) =>
      decoder
        .decode(new String(bytes))
        .getOrElse(throw new IllegalArgumentException("Failed to decode"))

  /** A Conversion[A, Array[Byte]], useful for marshalling response bodies in
    * spider
    */
  def convertToBytes[A](using
      encoder: JsonEncoder[A]
  ): Conversion[A, Array[Byte]] =
    (a: A) => encoder.encode(a).toJsonString.getBytes

}

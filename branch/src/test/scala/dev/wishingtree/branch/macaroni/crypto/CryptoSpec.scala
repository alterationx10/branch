package dev.wishingtree.branch.macaroni.crypto

import munit.*

class CryptoSpec extends FunSuite {

  test("Crypto.slowEquals") {
    val a = Array[Byte](1, 2, 3)
    val b = Array[Byte](1, 2, 3)
    val c = Array[Byte](1, 2, 4)
    val d = Array[Byte](1, 2, 3, 4)
    assert(Crypto.slowEquals(a, b))
    assert(!Crypto.slowEquals(a, c))
    assert(!Crypto.slowEquals(a, d))
  }

  test("Crypto.pbkdf2") {
    val message    = "password".toCharArray
    val salt       = Array[Byte](1, 2, 3)
    val iterations = 1000
    val bytes      = 24
    val hash       = Crypto.pbkdf2(message, salt, iterations, bytes)
    assert(hash.length == bytes)
  }

  test("Crypto.fromHex") {
    val hex   = "ff0080"
    val bytes = Crypto.fromHex(hex)
    assert(bytes sameElements Array(0xff.toByte, 0x00.toByte, 0x80.toByte))
    assert(Crypto.fromHex("FF") sameElements Crypto.fromHex("ff"))
  }

  test("Crypto.toHex") {
    val bytes = Array(0xff.toByte, 0x00.toByte, 0x80.toByte)
    val hex   = Crypto.toHex(bytes)
    assertEquals(hex, "FF0080")
  }

  test("Crypto.pbkdf2Hash") {
    val password = "password"
    val hash     = Crypto.pbkdf2Hash(password)
    assert(hash.split(":").length == 3)
  }

  test("Crypto.validatePbkdf2Hash") {
    val password = "password"
    val hash     = Crypto.pbkdf2Hash(password)
    assert(Crypto.validatePbkdf2Hash(password, hash))
  }

  test("Crypto.base64Encode") {
    val bytes     = Array[Byte](1, 2, 3)
    val str       = new String(bytes)
    val base64    = Crypto.base64Encode(bytes)
    val base64Str = new String(base64)
    assertEquals(base64, "AQID")
    assertEquals(base64, base64Str)
  }

  test("Crypto.base64Decode") {
    val base64 = "AQID"
    val bytes  = Crypto.base64Decode(base64)
    assert(bytes sameElements Array[Byte](1, 2, 3))
  }

  test("Crypto.base64DecodeToBytes") {
    val base64 = "AQID"
    val bytes  = Crypto.base64DecodeToBytes(base64)
    assert(bytes sameElements Array[Byte](1, 2, 3))
  }

  test("Crypto.aesEncrypt | Crypto.aesDecrypt") {
    val msg       = "Don't forget to drink your Ovaltine"
    val key       = Crypto.generatePrivateKey()
    val encrypted = Crypto.aesEncrypt(msg, key)
    assert(encrypted.isSuccess)
    val decrypted = Crypto.aesDecrypt(encrypted.get, key)
    assert(decrypted.isSuccess)
    assertEquals(decrypted.get, msg)
  }

  test("Crypto.hmac265 | Crypto.validateHmac256") {
    val msg  = "Don't forget to drink your Ovaltine"
    val key  = Crypto.generatePrivateKey()
    val hash = Crypto.hmac256(msg, key)
    assert(Crypto.validateHmac256(msg, key, hash))
  }

  test("Crypto.hmac512 | Crypto.validateHmac512") {
    val msg  = "Don't forget to drink your Ovaltine"
    val key  = Crypto.generatePrivateKey()
    val hash = Crypto.hmac512(msg, key)
    assert(Crypto.validateHmac512(msg, key, hash))
  }

  test("Crypto.generatePublicKey") {
    val key = Crypto.generatePublicKey()
    assertEquals(key.length, 16)
//    assertEquals(Crypto.generatePublicKey(32).length, 32)
  }

  test("Crypto.generatePrivateKey") {
    val key = Crypto.generatePrivateKey()
    assert(key.length == 32)
    assert(Crypto.generatePrivateKey(16).length == 16)
  }

}

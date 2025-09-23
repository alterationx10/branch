package dev.alteration.branch.macaroni.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.{Cipher, Mac, SecretKeyFactory}
import javax.crypto.spec.{PBEKeySpec, SecretKeySpec}
import scala.util.Try

private[crypto] trait Crypto {

  private val PBKDF2_ALGORITHM: String = "PBKDF2WithHmacSHA512"
  private val SALT_BYTE_SIZE: Int      = 24
  private val HASH_BYTE_SIZE: Int      = 24
  private val PBKDF2_ITERATIONS: Int   = 1000
  private val ITERATION_INDEX: Int     = 0
  private val SALT_INDEX: Int          = 1
  private val PBKDF2_INDEX: Int        = 2

  private val skf: SecretKeyFactory =
    SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)

  /** Compare two Byte arrays in length-constant time
    *
    * @param a
    *   Array 1
    * @param b
    *   Array 2
    * @return
    *   true if the same, false if not
    */
  def slowEquals(a: Array[Byte], b: Array[Byte]): Boolean = {
    val range = 0 until math.min(a.length, b.length)
    val diff  = range.foldLeft(a.length ^ b.length) { case (acc, i) =>
      acc | a(i) ^ b(i)
    }
    diff == 0
  }

  /** Performs PBKDF2 hashing. You likely want to use the [[pbkdf2Hash]] method,
    * which does the salting automatically.
    *
    * @param message
    *   Char Array of the message to be hashed
    * @param salt
    *   A random salt Byte Array
    * @param iterations
    *   The number of hashing iterations to perform
    * @param bytes
    *   The hash byte size
    * @return
    *   The computed has as a Byte Array
    */
  def pbkdf2(
      message: Array[Char],
      salt: Array[Byte],
      iterations: Int,
      bytes: Int
  ): Array[Byte] = {
    val keySpec: PBEKeySpec =
      new PBEKeySpec(message, salt, iterations, bytes * 8)
    skf.generateSecret(keySpec).getEncoded
  }

  /** Convert a hex string to a Byte Array (case-insensitive)
    *
    * @param hex
    *   The hex string to convert
    * @return
    *   The Byte Array
    */
  def fromHex(hex: String): Array[Byte] = {
    hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
  }

  /** Convert a Byte Array to a hex string (uppercase)
    *
    * @param array
    *   The Byte Array to convert
    * @return
    *   The hex string
    */
  def toHex(array: Array[Byte]): String = {
    array.map("%02X" format _).mkString
  }

  /** Creates a PBKDF2 hash string
    *
    * @param str
    * @return
    *   A hash of the form nIteration:salt:hash where salt and hash are in hex
    *   format
    */
  def pbkdf2Hash(str: String, iterations: Int = PBKDF2_ITERATIONS): String = {

    val rng: SecureRandom = new SecureRandom()
    val salt: Array[Byte] = Array.ofDim[Byte](SALT_BYTE_SIZE)
    rng.nextBytes(salt)
    val hashBytes         = pbkdf2(str.toCharArray, salt, iterations, HASH_BYTE_SIZE)
    s"$iterations:${toHex(salt)}:${toHex(hashBytes)}"
  }

  /** Validates a PBKDF2 hash
    *
    * @param str
    *   The plain text you are confirming
    * @param hash
    *   The hash, in form of nIteration:salt:hash
    * @return
    */
  def validatePbkdf2Hash(str: String, hash: String): Boolean = {
    val hashSegments   = hash.split(":")
    val validHash      = fromHex(hashSegments(PBKDF2_INDEX))
    val hashIterations = hashSegments(ITERATION_INDEX).toInt
    val hashSalt       = fromHex(hashSegments(SALT_INDEX))
    val testHash       =
      pbkdf2(str.toCharArray, hashSalt, hashIterations, HASH_BYTE_SIZE)
    slowEquals(validHash, testHash)
  }

  /** Base64 encode a Byte Array (without padding
    *
    * @param bytes
    *   The Byte Array to encode
    * @return
    *   The encoded string
    */
  def base64Encode(bytes: Array[Byte]): String =
    Base64.getUrlEncoder.withoutPadding().encodeToString(bytes)

  /** Base64 encode a String (without padding)
    *
    * @param str
    *   The string to encode
    * @return
    *   The encoded string
    */
  def base64Encode(str: String): String =
    Base64.getUrlEncoder.withoutPadding().encodeToString(str.getBytes("UTF-8"))

  /** Base64 decode a String
    *
    * @param str
    *   The string to decode
    * @return
    *   The decoded string
    */
  def base64Decode(str: String): String = new String(
    Base64.getUrlDecoder.decode(str)
  )

  /** Base64 decode a String to a Byte Array
    *
    * @param str
    *   The string to decode
    * @return
    *   The decoded Byte Array
    */
  def base64DecodeToBytes(str: String): Array[Byte] =
    Base64.getUrlDecoder.decode(str)

  /** Validate the length of a key used for AES De/Encryption
    *
    * @param key
    *   The AES encryption key. AES key length must be 16, 24, or 32
    * @return
    *   true if key length is valid length.
    */
  private def validateAESKeyLength(key: String): Boolean = {
    key.getBytes.length == 16 || key.getBytes.length == 24 || key.getBytes.length == 32
  }

  /** AES encrypt plainText using key. Key must be of length 16, 24, or 32
    *
    * @param key
    *   The AES encryption key
    * @param message
    *   The message to encrypt
    * @return
    *   A Try of [The encrypted message]
    */
  def aesEncrypt(message: String, key: String): Try[String] = {
    val attempt = for {
      cipher    <- Try(Cipher.getInstance("AES/ECB/PKCS5PADDING"))
      secretKey <- Try(new SecretKeySpec(key.getBytes("UTF-8"), "AES"))
    } yield {
      Try {
        if (!validateAESKeyLength(key))
          throw new IllegalArgumentException("Invalid AES key length")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        base64Encode(cipher.doFinal(message.getBytes))
      }
    }
    attempt.flatten
  }

  /** Decrypt an AES encrypted message
    *
    * @param message
    *   The encrypted message
    * @param key
    *   The AES key to use for decryption. Key must be of length 16, 24, or 32
    * @return
    *   A Try of [The decrypted message]
    */
  def aesDecrypt(message: String, key: String): Try[String] = {
    val attempt = for {
      cipher    <- Try(Cipher.getInstance("AES/ECB/PKCS5PADDING"))
      secretKey <- Try(new SecretKeySpec(key.getBytes("UTF-8"), "AES"))
    } yield {
      Try {
        if (!validateAESKeyLength(key))
          throw new IllegalArgumentException("Invalid AES key length")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        cipher.doFinal(base64DecodeToBytes(message)).map(_.toChar).mkString
      }
    }
    attempt.flatten
  }

  /** Helper method to do HMAC message hashing, based on the macInstance passed
    *
    * @param message
    *   The message to hash
    * @param key
    *   The secret to hash it against
    * @param macInstance
    *   The HMAC hash type (i.e. HmacSHA256, HmacSHA512)
    * @return
    *   The HMAC hash of the message against the key
    */
  private def hmac(
      message: String,
      key: String,
      macInstance: String
  ): String = {

    val mac       = Mac.getInstance(macInstance)
    val secretKey = new SecretKeySpec(key.getBytes("UTF-8"), macInstance)
    mac.init(secretKey)
    base64Encode(mac.doFinal(message.getBytes("UTF-8")))

  }

  /** Calculates a HmacSHA256 hash
    *
    * @param message
    *   The message to hash
    * @param key
    *   The secret to hash it against
    * @return
    *   The HMAC hash of the message against the key
    */
  def hmac256(message: String, key: String): String =
    hmac(message, key, "HmacSHA256")

  /** Validates a HmacSHA256 hash
    *
    * @param message
    *   The message to compare
    * @param key
    *   The secret to hash it against
    * @param hs256
    *   The HmacSHA256 hash to compare
    * @return
    *   True if the message and hash match, false otherwise
    */
  def validateHmac256(message: String, key: String, hs256: String): Boolean = {
    val calc = hmac256(message, key)
    slowEquals(calc.getBytes("UTF-8"), hs256.getBytes("UTF-8"))
  }

  /** Calculates a HmacSHA512 hash
    *
    * @param message
    *   The message to hash
    * @param key
    *   The secret to hash it against
    * @return
    *   The HMAC hash of the message against the key
    */
  def hmac512(message: String, key: String): String =
    hmac(message, key, "HmacSHA512")

  /** Validates a HmacSHA512 hash
    *
    * @param message
    *   The message to compare
    * @param key
    *   The secret to hash it against
    * @param hs512
    *   The HmacSHA512 hash to compare
    * @return
    *   True if the message and hash match, false otherwise
    */
  def validateHmac512(message: String, key: String, hs512: String): Boolean = {
    val calc = hmac512(message, key)
    slowEquals(calc.getBytes("UTF-8"), hs512.getBytes("UTF-8"))
  }

  /** Generate a (nice) random String that can be used as a public key in the
    * ascii 48 to 123 range, with some characters removed (58-64, 91-96).
    *
    * @param keyLength
    *   The length of the key to generate. Default is 16
    */
  def generatePublicKey(keyLength: Int = 16): String = {

    val random = new SecureRandom()

    // exclude 58-64, 91-96
    def loop(list: List[Int]): List[Int] = list.length match {
      case done if done == keyLength =>
        list
      case next if next < keyLength  =>
        val nextInt = random.nextInt(75) + 48
        if (
          (nextInt >= 58 && nextInt <= 64) || (nextInt >= 91 && nextInt <= 96)
        ) {
          loop(list)
        } else {
          loop(list :+ nextInt)
        }
    }

    loop(List()).map(_.toChar).mkString
  }

  /** Generate random String that can be used as a private key that's in the
    * ascii 48 to 123 range.
    *
    * @param keyLength
    *   The length of the key to generate. Default is 32
    */
  def generatePrivateKey(keyLength: Int = 32): String = {
    val random = new SecureRandom()
    (1 to keyLength)
      .map { _ =>
        (random.nextInt(75) + 48).toChar
      }
      .mkString
      .replaceAll("\\\\+", "/")
  }

}

object Crypto extends Crypto

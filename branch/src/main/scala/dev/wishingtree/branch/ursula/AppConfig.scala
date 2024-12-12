package dev.wishingtree.branch.ursula

import dev.wishingtree.branch.friday.Json.JsonObject
import dev.wishingtree.branch.friday.{Json, JsonCodec, JsonEncoder}
import dev.wishingtree.branch.veil.Config

import java.nio.file.{Files, Path}
import scala.collection.concurrent.TrieMap

trait AppConfig {
  def get(key: String): Option[String]
  def set(key: String, value: String): Unit
  def delete(key: String): Unit
}

object AppConfig extends AppConfig {

  private lazy val store = TrieMap.empty[String, String]

  override def get(key: String): Option[String] = store.get(key)

  override def set(key: String, value: String): Unit = store.put(key, value)

  override def delete(key: String): Unit = store.remove(key)

  private[ursula] def load(path: Path): Unit = {
    Json.parse(new String(Files.readAllBytes(path))).foreach {
      case JsonObject(store) =>
        store.foreach { (key, value) =>
          set(key, value.strVal)
        }
      case _                 =>
        throw new IllegalArgumentException("Invalid config file")
    }
  }

  private[ursula] def persist(path: Path): Unit = {
    val json = Json.encode(store.map { (key, value) =>
      key -> Json.JsonString(value)
    }.toMap)
    Files.write(path, json.toString.getBytes)
  }
}

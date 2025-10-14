package dev.alteration.branch.spider.webview

import dev.alteration.branch.friday.{Json, JsonCodec}
import scala.deriving.Mirror
import scala.util.{Failure, Success, Try}

/** Type-class for encoding and decoding WebView events.
  *
  * EventCodec handles the serialization and deserialization of typed events
  * between the client and server. Events are sent from the browser as JSON with
  * an event name and payload, and decoded into strongly-typed ADTs on the
  * server.
  *
  * Example:
  * {{{
  * sealed trait CounterEvent derives EventCodec
  * case object Increment extends CounterEvent
  * case object Decrement extends CounterEvent
  * case class SetCount(value: Int) extends CounterEvent
  *
  * // On the client, events are sent as:
  * // { "event": "Increment" }
  * // { "event": "SetCount", "value": 42 }
  *
  * // On the server, they're decoded to:
  * // Increment
  * // SetCount(42)
  * }}}
  *
  * @tparam Event
  *   The event type (typically a sealed trait)
  */
trait EventCodec[Event] {

  /** Encode an event to a JSON representation.
    *
    * This converts a strongly-typed event into JSON that can be sent to the
    * client (typically for testing or server-initiated events).
    *
    * @param event
    *   The event to encode
    * @return
    *   JSON representation of the event
    */
  def encode(event: Event): Json

  /** Decode an event from a JSON representation.
    *
    * This converts JSON from the client into a strongly-typed event. The JSON
    * typically contains an "event" field with the event name and additional
    * fields for the event payload.
    *
    * @param json
    *   The JSON to decode
    * @return
    *   Success with the decoded event, or Failure if decoding fails
    */
  def decode(json: Json): Try[Event]

  /** Decode an event from a JSON string.
    *
    * @param jsonStr
    *   The JSON string to decode
    * @return
    *   Success with the decoded event, or Failure if parsing or decoding fails
    */
  def decode(jsonStr: String): Try[Event] =
    Json.parse(jsonStr).toOption match {
      case Some(json) => decode(json)
      case None       =>
        Failure(new RuntimeException(s"Failed to parse JSON: $jsonStr"))
    }

  /** Decode an event from the client event format.
    *
    * The client sends events with:
    *   - event: the event name/type
    *   - target: the element ID that triggered the event (optional)
    *   - value: the event payload (optional)
    *
    * This method extracts the event name and payload and attempts to decode
    * them into a strongly-typed event.
    *
    * @param eventName
    *   The event name from the client
    * @param payload
    *   The event payload map (contains "target" and "value" fields)
    * @return
    *   Success with the decoded event, or Failure if decoding fails
    */
  def decodeFromClient(eventName: String, payload: Map[String, Any]): Try[Event]

  extension (event: Event) {

    /** Encode this event to JSON */
    def toJson: Json = encode(event)

    /** Encode this event to a JSON string */
    def toJsonString: String = toJson.toJsonString
  }
}

object EventCodec {

  /** Private implementation of EventCodec that delegates to JsonCodec.
    *
    * This class exists to avoid duplicating anonymous class definitions at each
    * inline site where the derived given is used.
    */
  class DerivedEventCodec[Event](jsonCodec: JsonCodec[Event])
      extends EventCodec[Event] {
    def encode(event: Event): Json = jsonCodec.encode(event)

    def decode(json: Json): Try[Event] = jsonCodec.decode(json)

    def decodeFromClient(
        eventName: String,
        payload: Map[String, Any]
    ): Try[Event] = {
      // Build JSON from event name and payload
      // Client sends: { event: "EventName", target: "...", value: "..." }
      // We construct: { "type": "EventName", ...fields from value if present }

      // Extract value from payload if present
      val fields = payload.get("value") match {
        case Some(Json.JsonObject(obj))    =>
          // Form data or nested object - use fields directly
          obj
        case Some(Json.JsonString(v)) if v.nonEmpty =>
          // Try to parse as JSON to extract fields
          Json.parse(v).toOption match {
            case Some(Json.JsonObject(obj)) => obj
            case Some(other)                => Map("value" -> other) // Single value field
            case None                       => Map("value" -> Json.JsonString(v))
          }
        case Some(json: Json)              =>
          // Other JSON types (number, bool, string, etc.)
          Map("value" -> json)
        case Some(v: String) if v.nonEmpty =>
          // String value (maybe no longer needed?)
          Json.parse(v).toOption match {
            case Some(Json.JsonObject(obj)) => obj
            case Some(other)                => Map("value" -> other)
            case None                       => Map("value" -> Json.JsonString(v))
          }
        case Some(v: Map[_, _])            =>
          // Form data as Map (maybe no longer needed?) - convert to JSON fields
          v.asInstanceOf[Map[String, Any]].map { case (key, value) =>
            key -> (value match {
              case i: Int     => Json.JsonNumber(i)
              case d: Double  => Json.JsonNumber(d)
              case b: Boolean => Json.JsonBool(b)
              case s: String  => Json.JsonString(s)
              case other      => Json.JsonString(other.toString)
            })
          }
        case Some(v)                       =>
          // Convert other types to JSON
          Map("value" -> (v match {
            case i: Int     => Json.JsonNumber(i)
            case d: Double  => Json.JsonNumber(d)
            case b: Boolean => Json.JsonBool(b)
            case other      => Json.JsonString(other.toString)
          }))
        case None                          =>
          Map.empty[String, Json]
      }

      // Construct the JSON object for decoding with the type field
      val eventJson =
        Json.JsonObject(fields + ("type" -> Json.JsonString(eventName)))

      decode(eventJson)
    }
  }

  /** Get the EventCodec instance for a given type.
    *
    * @tparam Event
    *   The event type
    * @return
    *   The EventCodec instance
    */
  def apply[Event](using codec: EventCodec[Event]): EventCodec[Event] = codec

  /** Create an EventCodec from explicit encode/decode functions.
    *
    * This is useful for custom event encoding/decoding logic that doesn't
    * follow the standard derived pattern.
    *
    * @param encodeFunc
    *   Function to encode an event to JSON
    * @param decodeFunc
    *   Function to decode JSON to an event
    * @param decodeClientFunc
    *   Function to decode client events (event name + payload)
    * @tparam Event
    *   The event type
    * @return
    *   A new EventCodec instance
    */
  def from[Event](
      encodeFunc: Event => Json,
      decodeFunc: Json => Try[Event],
      decodeClientFunc: (String, Map[String, Any]) => Try[Event]
  ): EventCodec[Event] =
    new EventCodec[Event] {
      def encode(event: Event): Json     = encodeFunc(event)
      def decode(json: Json): Try[Event] = decodeFunc(json)
      def decodeFromClient(
          eventName: String,
          payload: Map[String, Any]
      ): Try[Event] =
        decodeClientFunc(eventName, payload)
    }

  /** Automatically derive an EventCodec for a sealed trait.
    *
    * This uses Friday's automatic sum type derivation to generate encoding and
    * decoding logic for sealed trait ADTs.
    *
    * The derived codec encodes events using Friday's tagged union format:
    *   - Case objects: { "type": "ObjectName" }
    *   - Case classes: { "type": "ClassName", "field1": value1, "field2":
    *     value2, ... }
    *
    * For client events, it matches the event name (from the "type" field) to
    * the ADT case and decodes payload fields into the case class fields.
    *
    * @tparam Event
    *   The event type (must be a sealed trait)
    * @return
    *   A derived EventCodec instance
    */
  inline given derived[Event](using
      m: Mirror.SumOf[Event]
  ): EventCodec[Event] = {
    // Use Friday's JsonCodec which now supports sum types!
    val jsonCodec = JsonCodec.derived[Event]

    new DerivedEventCodec[Event](jsonCodec)
  }

  /** EventCodec for String events (for backward compatibility).
    *
    * This allows using String as the event type, which is useful for gradually
    * migrating from string-based events to typed events.
    */
  given stringEventCodec: EventCodec[String] = new EventCodec[String] {
    def encode(event: String): Json = Json.JsonString(event)

    def decode(json: Json): Try[String] = json match {
      case Json.JsonString(s) => Success(s)
      case _                  => Failure(new RuntimeException(s"Expected string, got: $json"))
    }

    def decodeFromClient(
        eventName: String,
        payload: Map[String, Any]
    ): Try[String] =
      Success(eventName)
  }
}

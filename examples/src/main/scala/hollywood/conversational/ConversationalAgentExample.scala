package hollywood.conversational

import dev.alteration.branch.hollywood.{
  ConversationalAgent,
  ConversationState,
  InMemoryState
}
import dev.alteration.branch.hollywood.clients.completions.ChatMessage

/** Example showing ConversationalAgent for stateful multi-turn conversations.
  *
  * ConversationalAgent maintains conversation history and context across
  * multiple interactions, making it ideal for:
  *   - Chat applications
  *   - Interactive assistants
  *   - Scenarios where context matters
  *
  * This example assumes you have an OpenAI-compatible LLM server running:
  *   - Default URL: http://localhost:8080
  *   - Or set LLAMA_SERVER_URL environment variable
  *
  * To run: sbt "examples/runMain
  * hollywood.conversational.ConversationalAgentExample"
  */
object ConversationalAgentExample {

  def main(args: Array[String]): Unit = {
    println("=== Hollywood ConversationalAgent Example ===\n")

    // Example 1: Basic conversational agent
    println("--- Example 1: Basic Conversation ---")
    val agent = ConversationalAgent()

    println("User: Hello! My name is Alice.")
    val response1 = agent.chat("Hello! My name is Alice.")
    println(s"Agent: $response1\n")

    println("User: What's my name?")
    val response2 = agent.chat("What's my name?")
    println(s"Agent: $response2\n")

    println("User: What did we talk about?")
    val response3 = agent.chat("What did we talk about?")
    println(s"Agent: $response3\n")

    // Example 2: Custom system prompt with conversation state
    println("--- Example 2: Role-playing Agent ---")
    val customState = new InMemoryState()
    // Add initial system message
    customState.update(
      List(
        ChatMessage(
          role = "system",
          content = Some(
            "You are Sherlock Holmes. Respond in character, with keen observations and deductive reasoning."
          )
        )
      )
    )

    val sherlockAgent = ConversationalAgent(
      conversationState = customState
    )

    println("User: I just arrived from the countryside.")
    val sherlock1 = sherlockAgent.chat("I just arrived from the countryside.")
    println(s"Sherlock: $sherlock1\n")

    println("User: How did you know?")
    val sherlock2 = sherlockAgent.chat("How did you know?")
    println(s"Sherlock: $sherlock2\n")

    // Example 3: Message limit demonstration
    println("--- Example 3: Conversation State Management ---")
    val limitedState = new InMemoryState(maxMessages = 4)
    val limitedAgent = ConversationalAgent(
      conversationState = limitedState
    )

    println("Sending 5 messages to agent with max 4 messages...")
    val messages = List(
      "Message 1: Remember the number one.",
      "Message 2: Remember the number two.",
      "Message 3: Remember the number three.",
      "Message 4: Remember the number four.",
      "Message 5: What numbers did I tell you to remember?"
    )

    messages.foreach { msg =>
      println(s"\nUser: $msg")
      val response = limitedAgent.chat(msg)
      println(s"Agent: $response")
    }
    println(
      "\nNote: The agent may not remember 'number one' as the state is limited to 4 messages"
    )
    println()

    // Example 4: Callback for monitoring turns
    println("--- Example 4: Monitoring Conversation Turns ---")
    var turnCount = 0

    val monitoredAgent = ConversationalAgent(
      onTurn = Some { (turn: Int, message: ChatMessage) =>
        turnCount += 1
        println(
          s"  [Turn $turn] Role: ${message.role}, Content: ${message.content.getOrElse("N/A").take(50)}..."
        )
      }
    )

    println("\nUser: Explain recursion in one sentence.")
    monitoredAgent.chat("Explain recursion in one sentence.")
    println(s"\nTotal turns for this request: $turnCount\n")

    // Example 5: Custom conversation state implementation
    println("--- Example 5: Custom Conversation State ---")
    class LoggingState extends ConversationState {
      private var messages: List[ChatMessage] = List.empty
      private var interactionCount            = 0

      def get: List[ChatMessage] = messages

      def update(newMessages: List[ChatMessage]): Unit = {
        messages = newMessages
        interactionCount += 1
        println(s"    [State Updated: Interaction #$interactionCount, " +
          s"Total messages: ${messages.size}]")
      }

      def getInteractionCount: Int = interactionCount
    }

    val loggingState = new LoggingState()
    val loggingAgent = ConversationalAgent(conversationState = loggingState)

    println("Starting conversation with logging state...")
    loggingAgent.chat("Hello")
    loggingAgent.chat("How are you?")
    loggingAgent.chat("Goodbye")

    println(
      s"Total interactions tracked: ${loggingState.getInteractionCount}\n"
    )

    println("=== Example Complete ===")
  }
}

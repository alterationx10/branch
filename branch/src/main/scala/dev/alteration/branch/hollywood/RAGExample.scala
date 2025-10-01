package dev.alteration.branch.hollywood

/** Example demonstrating RAG (Retrieval-Augmented Generation) agent usage */
object RAGExample {

  def main(args: Array[String]): Unit = {

    // Create an in-memory vector store
    val vectorStore = new InMemoryVectorStore()

    // Create RAG agent with custom configuration
    val ragConfig = RAGConfig(
      embeddingModel = "text-embedding-3-small",
      topK = 3,
      embeddingEndpoint = "http://localhost:8080/v1/embeddings"
    )

    val agentConfig = AgentConfig(
      maxTurns = 10,
      model = "gpt-oss",
      onTurn = Some { (turn, message) =>
        println(s"Turn $turn: ${message.role}")
        message.content.foreach(content => println(s"  Content: ${content.take(100)}..."))
      }
    )

    val ragAgent = new RAGAgent(
      vectorStore = vectorStore,
      ragConfig = ragConfig,
      config = agentConfig
    )

    // Index some documents into the knowledge base
    val documents = List(
      (
        "doc1",
        """Scala is a strong statically typed general-purpose programming language
          |that supports both object-oriented and functional programming.
          |It runs on the JVM and can interoperate with Java libraries.""".stripMargin
      ),
      (
        "doc2",
        """The JVM (Java Virtual Machine) is an abstract computing machine that
          |enables a computer to run Java programs and programs written in other
          |languages that are compiled to Java bytecode.""".stripMargin
      ),
      (
        "doc3",
        """Functional programming is a programming paradigm where programs are
          |constructed by applying and composing functions. It emphasizes immutability,
          |pure functions, and avoiding side effects.""".stripMargin
      ),
      (
        "doc4",
        """Type safety in programming languages means that the compiler or runtime
          |checks that operations are performed on compatible types, preventing many
          |common programming errors at compile time.""".stripMargin
      )
    )

    println("Indexing documents into vector store...")
    ragAgent.indexDocuments(documents)
    println(s"Indexed ${documents.size} documents\n")

    // Ask questions that should be answered using the knowledge base
    val questions = List(
      "What is Scala?",
      "Tell me about functional programming",
      "What does the JVM do?"
    )

    questions.foreach { question =>
      println(s"\nQuestion: $question")
      println("=" * 80)
      val answer = ragAgent.chat(question)
      println(s"Answer: $answer")
      println()
    }

    // Example with vector store operations
    println("\n" + "=" * 80)
    println("Vector Store Operations:")
    println("=" * 80)

    // Direct search example
    val searchQuery = "programming languages and type systems"
    println(s"\nSearching for: '$searchQuery'")

    // Create a dummy embedding for demonstration (in real use, call the embedding API)
    // For this example, we'll use the agent's internal method by asking it to chat
    val searchResults = vectorStore.search(
      queryEmbedding = List.fill(1536)(scala.util.Random.nextDouble()),
      topK = 2
    )

    println(s"\nTop ${searchResults.size} results:")
    searchResults.foreach { scored =>
      println(s"\nDocument ID: ${scored.document.id}")
      println(f"Score: ${scored.score}%.4f")
      println(s"Content: ${scored.document.content.take(100)}...")
    }
  }
}

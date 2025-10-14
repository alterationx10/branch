package hollywood.rag

import dev.alteration.branch.hollywood.RagAgent
import dev.alteration.branch.hollywood.clients.embeddings.EmbeddingClient
import dev.alteration.branch.hollywood.rag.*

/** Example showing how to use RagAgent for document-based question answering.
  *
  * RAG (Retrieval-Augmented Generation) combines document retrieval with LLM
  * generation to answer questions based on a knowledge base. This example
  * demonstrates:
  *   - Creating and indexing a vector store
  *   - Using DocumentIndexer for automatic embedding
  *   - Chunking long documents with DocumentChunker
  *   - Querying the RAG agent
  *   - Different chunking strategies
  *
  * This example assumes you have an OpenAI-compatible LLM server running:
  *   - Default URL: http://localhost:8080
  *   - Or set LLAMA_SERVER_URL environment variable
  *   - Server must support both chat completions and embeddings
  *
  * To run: sbt "examples/runMain hollywood.rag.RagAgentExample"
  */
object RagAgentExample {

  def main(args: Array[String]): Unit = {
    println("=== Hollywood RAG Agent Example ===\n")

    // Initialize components
    val vectorStore     = new InMemoryVectorStore()
    val embeddingClient = new EmbeddingClient()
    val documentIndexer = new DocumentIndexer(embeddingClient, vectorStore)

    // Example 1: Basic RAG with simple documents
    println("--- Example 1: Basic Knowledge Base ---")

    val knowledgeBase = List(
      (
        "scala-intro",
        """Scala is a strong statically typed high-level general-purpose programming language
          |that supports both object-oriented programming and functional programming.
          |Designed to be concise, many of Scala's design decisions are aimed to address
          |criticisms of Java. Scala runs on the Java platform (JVM) and is compatible
          |with existing Java programs.""".stripMargin
      ),
      (
        "scala-features",
        """Scala has full support for functional programming including:
          |- Immutable data structures
          |- Pattern matching
          |- Higher-order functions
          |- Type inference
          |- Lazy evaluation
          |The language also has strong static type system with type inference.""".stripMargin
      ),
      (
        "scala-ecosystem",
        """Popular Scala frameworks and libraries include Akka for concurrent and
          |distributed systems, Play Framework for web applications, Spark for big data
          |processing, and Cats for functional programming. The Scala ecosystem is rich
          |and continues to grow.""".stripMargin
      )
    )

    // Index the documents
    println("Indexing knowledge base...")
    documentIndexer.indexDocuments(knowledgeBase)
    println(s"Indexed ${knowledgeBase.size} documents\n")

    // Create RAG agent
    val ragAgent = new RagAgent(
      embeddingClient = embeddingClient,
      vectorStore = vectorStore,
      topK = 2 // Retrieve top 2 most relevant documents
    )

    // Ask questions
    val questions = List(
      "What is Scala?",
      "What functional programming features does Scala have?",
      "What are some popular Scala frameworks?"
    )

    questions.foreach { question =>
      println(s"Q: $question")
      val answer = ragAgent.chat(question)
      println(s"A: $answer\n")
    }

    // Example 2: Document chunking for long texts
    println("--- Example 2: Chunking Long Documents ---")

    val longDocument =
      """The Branch library is a comprehensive Scala toolkit for building applications.
        |It provides multiple modules for different concerns. The Hollywood module enables
        |building AI agents with tools and multi-agent systems. The Friday module handles
        |JSON encoding and decoding with type-safe schemas. The Spider module provides
        |HTTP client and server capabilities. The Piggy module offers type-safe database
        |access with connection pooling. The Keanu module implements the actor model for
        |concurrent programming. The Veil module manages configuration and secrets.
        |Each module is designed to work independently or together as needed.""".stripMargin

    // Configure chunking by sentence
    val sentenceConfig = ChunkConfig(
      strategy = ChunkStrategy.Sentence,
      chunkSize = 3, // 3 sentences per chunk
      overlap = 1,   // 1 sentence overlap between chunks
      minChunkSize = 50
    )

    println("Chunking document by sentence...")
    DocumentChunker.chunk(longDocument, sentenceConfig) match {
      case Right(result) =>
        println(s"Created ${result.totalChunks} chunks")
        println(
          s"Avg size: ${result.avgSize} chars, Min: ${result.minSize}, Max: ${result.maxSize}\n"
        )

        // Index each chunk
        result.chunks.foreach { chunk =>
          val embedding = embeddingClient.getEmbedding(chunk.content)
          vectorStore.add(
            s"branch-doc-${chunk.index}",
            chunk.content,
            embedding
          )
        }

        println("Sample chunk:")
        println(s"${result.chunks.head.content}\n")

      case Left(error) =>
        println(s"Chunking failed: $error\n")
    }

    // Query about the chunked document
    println("Querying about chunked content...")
    val chunkQuery = "What module deals with JSON?"
    println(s"Q: $chunkQuery")
    val chunkAnswer = ragAgent.chat(chunkQuery)
    println(s"A: $chunkAnswer\n")

    // Example 3: Different chunking strategies
    println("--- Example 3: Chunking Strategies Comparison ---")

    val sampleText =
      """Retrieval-Augmented Generation (RAG) is a technique that combines
        |information retrieval with text generation. First, relevant documents
        |are retrieved from a knowledge base. Then, these documents are provided
        |as context to a language model. The model uses this context to generate
        |more accurate and grounded responses. RAG is particularly useful when
        |you need the model to reference specific information.""".stripMargin

    val strategies = List(
      (
        "Paragraph",
        ChunkConfig(
          strategy = ChunkStrategy.Paragraph,
          chunkSize = 1,
          overlap = 0,
          minChunkSize = 20
        )
      ),
      (
        "Character (200)",
        ChunkConfig(
          strategy = ChunkStrategy.Character,
          chunkSize = 200,
          overlap = 50,
          minChunkSize = 50
        )
      ),
      (
        "Token (50)",
        ChunkConfig(
          strategy = ChunkStrategy.Token,
          chunkSize = 50, // ~50 tokens = ~200 chars
          overlap = 10,
          minChunkSize = 50
        )
      )
    )

    strategies.foreach { case (name, config) =>
      DocumentChunker.chunk(sampleText, config) match {
        case Right(result) =>
          println(
            f"$name%-20s: ${result.totalChunks} chunks, avg ${result.avgSize} chars"
          )
        case Left(error)   =>
          println(s"$name: Error - $error")
      }
    }
    println()

    // Example 4: Demonstrating retrieval relevance
    println("--- Example 4: Retrieval Relevance ---")

    // Clear store and add topic-specific documents
    vectorStore.clear()

    val topicDocs = List(
      (
        "functional-programming",
        """Functional programming emphasizes immutability, pure functions, and
          |declarative code. Programs are built by composing functions without
          |side effects.""".stripMargin
      ),
      (
        "object-oriented",
        """Object-oriented programming organizes code into objects that combine
          |data and behavior. It emphasizes encapsulation, inheritance, and
          |polymorphism.""".stripMargin
      ),
      (
        "concurrent-programming",
        """Concurrent programming deals with multiple tasks executing
          |simultaneously. It requires careful handling of shared state and
          |synchronization.""".stripMargin
      )
    )

    documentIndexer.indexDocuments(topicDocs)

    // Create RAG agent with higher topK to see what it retrieves
    val demoAgent = new RagAgent(
      embeddingClient = embeddingClient,
      vectorStore = vectorStore,
      topK = 3
    )

    println("Query: Tell me about functional programming")
    println("(The agent will retrieve the most relevant documents)")
    val fpAnswer = demoAgent.chat("Tell me about functional programming")
    println(s"Answer: $fpAnswer\n")

    // Example 5: Handling queries with no relevant context
    println("--- Example 5: Queries Without Relevant Context ---")

    val irrelevantQuery = "What is quantum computing?"
    println(s"Q: $irrelevantQuery")
    println("(No documents about quantum computing in the knowledge base)")
    val noContextAnswer = demoAgent.chat(irrelevantQuery)
    println(s"A: $noContextAnswer\n")

    // Example 6: Vector store operations
    println("--- Example 6: Vector Store Operations ---")

    println(s"Total documents in store: ${topicDocs.size}")

    // Get specific document
    vectorStore.get("functional-programming") match {
      case Some(doc) =>
        println(s"Retrieved document: ${doc.id}")
        println(s"Content preview: ${doc.content.take(50)}...")
      case None      =>
        println("Document not found")
    }
    println()

    // Search by embedding
    val searchQuery     = "What is FP?"
    val searchEmbedding = embeddingClient.getEmbedding(searchQuery)
    val searchResults   = vectorStore.search(searchEmbedding, topK = 2)

    println(s"Search results for: '$searchQuery'")
    searchResults.foreach { scored =>
      println(
        f"- ${scored.document.id}: ${scored.score * 100}%.1f%% relevance"
      )
    }
    println()

    // Remove a document
    vectorStore.remove("object-oriented")
    println("Removed 'object-oriented' document")

    val remainingCount =
      List("functional-programming", "concurrent-programming")
        .count(id => vectorStore.get(id).isDefined)
    println(s"Remaining documents: $remainingCount\n")

    println("=== Example Complete ===")
    println("\nKey Takeaways:")
    println(
      "- RagAgent retrieves relevant documents and uses them as context"
    )
    println(
      "- DocumentIndexer automatically generates embeddings for documents"
    )
    println(
      "- DocumentChunker splits long texts into manageable pieces"
    )
    println("- InMemoryVectorStore provides fast similarity search")
    println(
      "- topK parameter controls how many documents are retrieved"
    )
  }
}

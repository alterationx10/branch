package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.clients.embeddings.EmbeddingClient
import dev.alteration.branch.hollywood.rag.{
  ChunkConfig,
  ChunkStrategy,
  DocumentChunker,
  DocumentIndexer,
  InMemoryVectorStore
}
import dev.alteration.branch.testkit.fixtures.LlamaServerFixture

class RagAgentSpec extends LlamaServerFixture {

  test("RagAgent should answer questions using indexed documents") {
    val vectorStore     = new InMemoryVectorStore()
    val embeddingClient = new EmbeddingClient()
    val documentIndexer = new DocumentIndexer(embeddingClient, vectorStore)

    val ragAgent = new RagAgent(
      embeddingClient = embeddingClient,
      vectorStore = vectorStore,
      topK = 3,
      maxTurns = 10
    )

    // Index documents
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
      )
    )

    documentIndexer.indexDocuments(documents)

    val answer = ragAgent.chat("What is Scala?")
    assert(answer.nonEmpty)
    assert(
      answer.toLowerCase.contains("scala") || answer.toLowerCase
        .contains("programming")
    )
  }

  test("RagAgent should retrieve relevant context for queries") {
    val vectorStore     = new InMemoryVectorStore()
    val embeddingClient = new EmbeddingClient()
    val documentIndexer = new DocumentIndexer(embeddingClient, vectorStore)

    val ragAgent = new RagAgent(
      embeddingClient = embeddingClient,
      vectorStore = vectorStore,
      topK = 2,
      maxTurns = 10
    )

    // Index documents with distinct topics
    val documents = List(
      (
        "doc1",
        """Functional programming is a programming paradigm where programs are
          |constructed by applying and composing functions. It emphasizes immutability,
          |pure functions, and avoiding side effects.""".stripMargin
      ),
      (
        "doc2",
        """Type safety in programming languages means that the compiler or runtime
          |checks that operations are performed on compatible types, preventing many
          |common programming errors at compile time.""".stripMargin
      )
    )

    documentIndexer.indexDocuments(documents)

    val answer = ragAgent.chat("Tell me about functional programming")
    assert(answer.nonEmpty)
  }

  test("DocumentIndexer should successfully index multiple documents") {
    val vectorStore     = new InMemoryVectorStore()
    val embeddingClient = new EmbeddingClient()
    val documentIndexer = new DocumentIndexer(embeddingClient, vectorStore)

    val documents = List(
      ("doc1", "First document content"),
      ("doc2", "Second document content"),
      ("doc3", "Third document content")
    )

    documentIndexer.indexDocuments(documents)

    // Verify documents are in vector store
    val doc1 = vectorStore.get("doc1")
    assert(doc1.isDefined)
    assert(doc1.get.content == "First document content")

    val doc2 = vectorStore.get("doc2")
    assert(doc2.isDefined)

    val doc3 = vectorStore.get("doc3")
    assert(doc3.isDefined)
  }

  test("InMemoryVectorStore should support search operations") {
    val vectorStore     = new InMemoryVectorStore()
    val embeddingClient = new EmbeddingClient()
    val documentIndexer = new DocumentIndexer(embeddingClient, vectorStore)

    val documents = List(
      ("doc1", "Scala programming language"),
      ("doc2", "Java programming language"),
      ("doc3", "Python programming language")
    )

    documentIndexer.indexDocuments(documents)

    // Get embedding for a query
    val queryEmbedding = embeddingClient.getEmbedding("programming languages")

    // Search should return results
    val results = vectorStore.search(queryEmbedding, topK = 2)
    assert(results.size <= 2)
    assert(results.nonEmpty)
  }

  test("DocumentChunker should integrate with RAG pipeline for long documents") {
    val vectorStore     = new InMemoryVectorStore()
    val embeddingClient = new EmbeddingClient()

    val ragAgent = new RagAgent(
      embeddingClient = embeddingClient,
      vectorStore = vectorStore,
      topK = 3,
      maxTurns = 10
    )

    // Long document about different topics
    val longDocument = """Scala is a powerful programming language that combines object-oriented
      |and functional programming paradigms. It was designed by Martin Odersky and first
      |released in 2003. Scala runs on the Java Virtual Machine and provides seamless
      |interoperability with Java libraries.
      |
      |The functional programming features in Scala include immutable data structures,
      |higher-order functions, pattern matching, and algebraic data types. These features
      |enable developers to write concise, maintainable code that is less prone to bugs.
      |
      |Scala's type system is one of its strongest features. It includes type inference,
      |generics, variance annotations, and implicit conversions. The compiler performs
      |sophisticated type checking at compile time, catching many errors before runtime.""".stripMargin

    // Configure chunker for paragraph-based chunking
    val chunkConfig = ChunkConfig(
      strategy = ChunkStrategy.Paragraph,
      chunkSize = 1,
      overlap = 0,
      minChunkSize = 50
    )

    // Chunk the document
    val chunkResult = DocumentChunker.chunk(longDocument, chunkConfig)

    // Index each chunk separately with unique IDs
    chunkResult.chunks.foreach { chunk =>
      val embedding = embeddingClient.getEmbedding(chunk.content)
      vectorStore.add(
        s"scala-doc-chunk-${chunk.index}",
        chunk.content,
        embedding
      )
    }

    // Verify chunks were created
    assert(chunkResult.totalChunks == 3, s"Expected 3 chunks, got ${chunkResult.totalChunks}")

    // Query for specific information from different chunks
    val answer = ragAgent.chat("What are Scala's functional programming features?")
    assert(answer.nonEmpty)

    // Verify the chunked document can be retrieved
    val chunk0 = vectorStore.get("scala-doc-chunk-0")
    assert(chunk0.isDefined)
    assert(chunk0.get.content.contains("Scala"))
  }
}

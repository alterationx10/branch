package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.clients.embeddings.EmbeddingClient
import dev.alteration.branch.hollywood.rag.{
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
}

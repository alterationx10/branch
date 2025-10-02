package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.rag.InMemoryVectorStore

/** Example demonstrating RAG (Retrieval-Augmented Generation) agent usage
  *
  * Prerequisites:
  *   - llama-server must be running with embeddings support enabled
  *   - Example: `llama-server --embeddings -m model.gguf`
  */
object RagExample {

  def main(args: Array[String]): Unit = {

    // Create an in-memory vector store
    val vectorStore = new InMemoryVectorStore()

    val ragAgent = new RagAgent(
      vectorStore = vectorStore,
      onTurn = Some { (turn, message) =>
        println(s"Turn $turn: ${message.role}")
        message.content.foreach(content =>
          println(s"  Content: ${content.take(100)}...")
        )
      },
      topK = 3,
      maxTurns = 10
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
    ).map(_ + " A one line summary will do.")

    questions.foreach { question =>
      println(s"\nQuestion: $question")
      println("=" * 80)
      val answer = ragAgent.chat(question)
      println(s"Answer: $answer")
      println()
    }

    // Example: Check vector store contents
    println("\n" + "=" * 80)
    println("Vector Store Summary:")
    println("=" * 80)
    println(s"Total documents indexed: ${documents.size}")
    documents.foreach { case (id, _) =>
      val doc = vectorStore.get(id)
      doc.foreach { d =>
        println(s"\nDocument '$id':")
        println(
          s"  Content preview: ${d.content.take(80).replaceAll("\n", " ")}..."
        )
        println(s"  Embedding dimensions: ${d.embedding.size}")
      }
    }
    println("\nRAG agent successfully demonstrated!")
  }
}

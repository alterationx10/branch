package dev.alteration.branch.hollywood.rag

import dev.alteration.branch.hollywood.clients.EmbeddingClient
import dev.alteration.branch.hollywood.rag.VectorStore

class DocumentIndexer(
    embeddingClient: EmbeddingClient,
    vectorStore: VectorStore
) {

  /** Index documents into the vector store */
  def indexDocuments(documents: List[(String, String)]): Unit = {
    documents.foreach { case (id, content) =>
      val embedding = embeddingClient.getEmbedding(content)
      vectorStore.add(id, content, embedding)
    }
  }
}

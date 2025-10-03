package dev.alteration.branch.hollywood.rag

/** Simple in-memory vector store implementation */
class InMemoryVectorStore extends VectorStore {

  private var documents: Map[String, Document] = Map.empty

  override def add(
      id: String,
      content: String,
      embedding: List[Double]
  ): Unit = {
    documents = documents + (id -> Document(id, content, embedding))
  }

  override def addAll(docs: List[(String, String, List[Double])]): Unit = {
    docs.foreach { case (id, content, embedding) =>
      add(id, content, embedding)
    }
  }

  override def search(
      queryEmbedding: List[Double],
      topK: Int = 5
  ): List[ScoredDocument] = {
    documents.values
      .map { doc =>
        val score = VectorStore.cosineSimilarity(queryEmbedding, doc.embedding)
        ScoredDocument(doc, score)
      }
      .toList
      .sortBy(-_.score)
      .take(topK)
  }

  override def get(id: String): Option[Document] = {
    documents.get(id)
  }

  override def remove(id: String): Unit = {
    documents = documents - id
  }

  override def clear(): Unit = {
    documents = Map.empty
  }
}

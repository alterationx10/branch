package dev.alteration.branch.hollywood

trait VectorStore {

  /** Add a document with its embedding to the store */
  def add(id: String, content: String, embedding: List[Double]): Unit

  /** Add multiple documents with their embeddings */
  def addAll(documents: List[(String, String, List[Double])]): Unit

  /** Search for similar documents using cosine similarity */
  def search(queryEmbedding: List[Double], topK: Int = 5): List[ScoredDocument]

  /** Get a document by ID */
  def get(id: String): Option[Document]

  /** Remove a document by ID */
  def remove(id: String): Unit

  /** Clear all documents */
  def clear(): Unit
}

case class Document(id: String, content: String, embedding: List[Double])

case class ScoredDocument(document: Document, score: Double)

object VectorStore {

  /** Calculate cosine similarity between two vectors */
  def cosineSimilarity(a: List[Double], b: List[Double]): Double = {
    require(a.length == b.length, "Vectors must have the same dimension")

    val dotProduct = a.zip(b).map { case (x, y) => x * y }.sum
    val magnitudeA = math.sqrt(a.map(x => x * x).sum)
    val magnitudeB = math.sqrt(b.map(x => x * x).sum)

    if (magnitudeA == 0.0 || magnitudeB == 0.0) 0.0
    else dotProduct / (magnitudeA * magnitudeB)
  }
}

/** Simple in-memory vector store implementation */
class InMemoryVectorStore extends VectorStore {

  private var documents: Map[String, Document] = Map.empty

  override def add(id: String, content: String, embedding: List[Double]): Unit = {
    documents = documents + (id -> Document(id, content, embedding))
  }

  override def addAll(docs: List[(String, String, List[Double])]): Unit = {
    docs.foreach { case (id, content, embedding) =>
      add(id, content, embedding)
    }
  }

  override def search(queryEmbedding: List[Double], topK: Int = 5): List[ScoredDocument] = {
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

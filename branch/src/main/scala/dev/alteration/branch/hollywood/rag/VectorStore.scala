package dev.alteration.branch.hollywood.rag

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

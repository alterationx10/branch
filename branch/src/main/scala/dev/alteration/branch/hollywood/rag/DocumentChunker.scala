package dev.alteration.branch.hollywood.rag

/** Chunk strategies for document splitting */
enum ChunkStrategy {
  case Sentence  // Split by sentence boundaries
  case Paragraph // Split by paragraph boundaries
  case Character // Split by character count
  case Token     // Split by approximate token count (1 token ≈ 4 chars)
}

/** Configuration for document chunking */
case class ChunkConfig(
    strategy: ChunkStrategy,
    chunkSize: Int,
    overlap: Int = 0,
    minChunkSize: Int = 1
)

/** A single chunk with metadata */
case class Chunk(
    index: Int,
    content: String,
    size: Int
)

/** Result of chunking operation with statistics */
case class ChunkResult(
    chunks: List[Chunk],
    totalChunks: Int,
    avgSize: Int,
    minSize: Int,
    maxSize: Int
)

object ChunkResult {
  def apply(chunks: List[Chunk]): ChunkResult = {
    if (chunks.isEmpty) {
      ChunkResult(chunks, 0, 0, 0, 0)
    } else {
      val sizes = chunks.map(_.size)
      ChunkResult(
        chunks = chunks,
        totalChunks = chunks.size,
        avgSize = sizes.sum / chunks.size,
        minSize = sizes.min,
        maxSize = sizes.max
      )
    }
  }
}

/** Service for intelligently splitting documents into chunks for RAG indexing
  */
object DocumentChunker {

  /** Chunk a document according to the given configuration
    *
    * @param text
    *   The document text to chunk
    * @param config
    *   Chunking configuration with the following requirements:
    *   - chunkSize must be positive
    *   - overlap must be non-negative
    *   - overlap must be less than chunkSize
    *   - minChunkSize must be positive
    * @return
    *   Right(ChunkResult) on success, Left(error message) on validation failure
    */
  def chunk(text: String, config: ChunkConfig): Either[String, ChunkResult] = {
    validateConfig(config) match {
      case Some(error) => Left(error)
      case None        =>
        val rawChunks = config.strategy match {
          case ChunkStrategy.Sentence  => chunkBySentence(text, config)
          case ChunkStrategy.Paragraph => chunkByParagraph(text, config)
          case ChunkStrategy.Character => chunkByCharacter(text, config)
          case ChunkStrategy.Token     => chunkByToken(text, config)
        }

        val filtered = rawChunks.filter(_.size >= config.minChunkSize)
        val indexed  = filtered.zipWithIndex.map { case (chunk, idx) =>
          chunk.copy(index = idx)
        }

        Right(ChunkResult(indexed))
    }
  }

  /** Validate chunk configuration
    *
    * Validates the following requirements:
    *   - chunkSize must be positive
    *   - overlap must be non-negative
    *   - overlap must be less than chunkSize
    *   - minChunkSize must be positive
    *
    * @param config
    *   The configuration to validate
    * @return
    *   Some(error message) if invalid, None if valid
    */
  private def validateConfig(config: ChunkConfig): Option[String] = {
    if (config.chunkSize <= 0)
      Some("chunkSize must be positive")
    else if (config.overlap < 0)
      Some("overlap must be non-negative")
    else if (config.overlap >= config.chunkSize)
      Some("overlap must be less than chunkSize")
    else if (config.minChunkSize <= 0)
      Some("minChunkSize must be positive")
    else
      None
  }

  // Split by sentence boundaries
  private def chunkBySentence(
      text: String,
      config: ChunkConfig
  ): List[Chunk] = {
    val sentences = splitIntoSentences(text)
    groupByCount(sentences, config.chunkSize, config.overlap)
  }

  // Split by paragraph boundaries
  private def chunkByParagraph(
      text: String,
      config: ChunkConfig
  ): List[Chunk] = {
    val paragraphs =
      text.split("\\n\\s*\\n").toList.map(_.trim).filter(_.nonEmpty)
    groupByCount(paragraphs, config.chunkSize, config.overlap)
  }

  // Split by character count
  private def chunkByCharacter(
      text: String,
      config: ChunkConfig
  ): List[Chunk] = {
    slidingWindow(text, config.chunkSize, config.overlap)
  }

  // Split by approximate token count (1 token ≈ 4 characters)
  private def chunkByToken(text: String, config: ChunkConfig): List[Chunk] = {
    val charSize    = config.chunkSize * 4
    val charOverlap = config.overlap * 4
    slidingWindow(text, charSize, charOverlap)
  }

  // Split text into sentences using basic sentence boundary detection
  private def splitIntoSentences(text: String): List[String] = {
    val sentencePattern = "([.!?]+)(?=\\s+[A-Z]|\\s*$)"
    val parts           = text.split(sentencePattern)

    val sentences = scala.collection.mutable.ListBuffer[String]()
    var i         = 0
    while (i < parts.length) {
      if (i + 1 < parts.length && parts(i + 1).matches("[.!?]+")) {
        sentences += (parts(i) + parts(i + 1)).trim
        i += 2
      } else {
        val trimmed = parts(i).trim
        if (trimmed.nonEmpty) {
          sentences += trimmed
        }
        i += 1
      }
    }

    sentences.toList.filter(_.nonEmpty)
  }

  // Group units (sentences/paragraphs) into chunks by count
  private def groupByCount(
      units: List[String],
      count: Int,
      overlap: Int
  ): List[Chunk] = {
    if (units.isEmpty) return List.empty

    val chunks = scala.collection.mutable.ListBuffer[Chunk]()
    var i      = 0

    while (i < units.length) {
      val endIdx  = math.min(i + count, units.length)
      val content = units.slice(i, endIdx).mkString(" ")

      chunks += Chunk(
        index = 0, // Will be set later
        content = content,
        size = content.length
      )

      i += math.max(1, count - overlap)
    }

    chunks.toList
  }

  // Create sliding window chunks by character count
  private def slidingWindow(
      text: String,
      size: Int,
      overlap: Int
  ): List[Chunk] = {
    if (text.isEmpty) return List.empty

    val chunks = scala.collection.mutable.ListBuffer[Chunk]()
    val step   = size - overlap
    var i      = 0

    while (i < text.length) {
      val endIdx  = math.min(i + size, text.length)
      val content = text.substring(i, endIdx).trim

      if (content.nonEmpty) {
        chunks += Chunk(
          index = 0, // Will be set later
          content = content,
          size = content.length
        )
      }

      i += step

      // Prevent infinite loop if step is 0
      if (step <= 0) {
        i = text.length
      }
    }

    chunks.toList
  }
}

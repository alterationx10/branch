package dev.alteration.branch.hollywood.rag

import munit.FunSuite

class DocumentChunkerSpec extends FunSuite {

  val sampleText =
    """This is the first sentence. This is the second sentence. This is the third sentence.
This is the fourth sentence. This is the fifth sentence. This is the sixth sentence."""

  val sampleParagraphs = """This is the first paragraph.
It has multiple sentences. It provides context.

This is the second paragraph.
It also has multiple sentences. It continues the document.

This is the third paragraph.
Final thoughts go here. The end."""

  test("DocumentChunker should chunk by sentence") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Sentence,
      chunkSize = 2
    )
    val result = DocumentChunker.chunk(sampleText, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assert(r.totalChunks > 0, "Should create chunks")
      assert(
        r.chunks.forall(_.content.nonEmpty),
        "All chunks should have content"
      )
    }
  }

  test("DocumentChunker should chunk by sentence with overlap") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Sentence,
      chunkSize = 3,
      overlap = 1
    )
    val result = DocumentChunker.chunk(sampleText, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assert(r.totalChunks > 0, "Should create chunks with overlap")
    }
  }

  test("DocumentChunker should chunk by paragraph") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Paragraph,
      chunkSize = 1
    )
    val result = DocumentChunker.chunk(sampleParagraphs, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assertEquals(r.totalChunks, 3, "Should create 3 paragraph chunks")
    }
  }

  test("DocumentChunker should chunk by paragraph with overlap") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Paragraph,
      chunkSize = 2,
      overlap = 1
    )
    val result = DocumentChunker.chunk(sampleParagraphs, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assert(r.totalChunks > 0, "Should create chunks with overlap")
    }
  }

  test("DocumentChunker should chunk by character count") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Character,
      chunkSize = 100
    )
    val result = DocumentChunker.chunk("A" * 1000, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assert(r.totalChunks > 0, "Should create character-based chunks")
      assert(r.maxSize <= 100, "Chunks should respect size limit")
    }
  }

  test("DocumentChunker should chunk by character count with overlap") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Character,
      chunkSize = 100,
      overlap = 20
    )
    val result = DocumentChunker.chunk("A" * 500, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assert(r.totalChunks > 0, "Should create chunks with overlap")
    }
  }

  test("DocumentChunker should chunk by token count") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Token,
      chunkSize = 10 // 10 tokens ≈ 40 characters
    )
    val result = DocumentChunker.chunk(sampleText, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assert(r.totalChunks > 0, "Should create token-based chunks")
    }
  }

  test("DocumentChunker should chunk by token count with overlap") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Token,
      chunkSize = 15,
      overlap = 3
    )
    val result = DocumentChunker.chunk(sampleText, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assert(r.totalChunks > 0, "Should create chunks with overlap")
    }
  }

  test("DocumentChunker should respect minimum chunk size") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Sentence,
      chunkSize = 1,
      minChunkSize = 20
    )
    val result = DocumentChunker.chunk("Short. Text. Here.", config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assert(
        r.chunks.forall(_.size >= 20),
        "All chunks should meet minimum size"
      )
    }
  }

  test("DocumentChunker should handle empty text") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Sentence,
      chunkSize = 2
    )
    val result = DocumentChunker.chunk("", config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assertEquals(r.totalChunks, 0, "Should produce no chunks for empty text")
    }
  }

  test("DocumentChunker should handle whitespace-only text") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Paragraph,
      chunkSize = 1
    )
    val result = DocumentChunker.chunk("   \n\n   \t  ", config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assertEquals(r.totalChunks, 0, "Should produce no chunks for whitespace")
    }
  }

  test("DocumentChunker should fail with negative chunk size") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Sentence,
      chunkSize = -1
    )
    val result = DocumentChunker.chunk("test", config)

    assert(result.isLeft, "Should fail validation")
    result.left.foreach { error =>
      assert(error.contains("chunkSize must be positive"), s"Got error: $error")
    }
  }

  test("DocumentChunker should fail with zero chunk size") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Sentence,
      chunkSize = 0
    )
    val result = DocumentChunker.chunk("test", config)

    assert(result.isLeft, "Should fail validation")
    result.left.foreach { error =>
      assert(error.contains("chunkSize must be positive"), s"Got error: $error")
    }
  }

  test("DocumentChunker should fail with negative overlap") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Sentence,
      chunkSize = 5,
      overlap = -1
    )
    val result = DocumentChunker.chunk("test", config)

    assert(result.isLeft, "Should fail validation")
    result.left.foreach { error =>
      assert(
        error.contains("overlap must be non-negative"),
        s"Got error: $error"
      )
    }
  }

  test("DocumentChunker should fail with overlap >= chunk size") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Sentence,
      chunkSize = 5,
      overlap = 5
    )
    val result = DocumentChunker.chunk("test", config)

    assert(result.isLeft, "Should fail validation")
    result.left.foreach { error =>
      assert(
        error.contains("overlap must be less than chunkSize"),
        s"Got error: $error"
      )
    }
  }

  test("DocumentChunker should handle single sentence") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Sentence,
      chunkSize = 2
    )
    val result = DocumentChunker.chunk("This is a single sentence.", config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assertEquals(r.totalChunks, 1, "Should create 1 chunk")
    }
  }

  test("DocumentChunker should handle text without sentence terminators") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Sentence,
      chunkSize = 1
    )
    val result =
      DocumentChunker.chunk("This is text without proper punctuation", config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assert(r.totalChunks >= 0, "Should handle text without terminators")
    }
  }

  test("DocumentChunker should detect multiple sentence types") {
    val mixedText =
      "First sentence. Second sentence! Third sentence? Fourth sentence."
    val config    = ChunkConfig(
      strategy = ChunkStrategy.Sentence,
      chunkSize = 1
    )
    val result    = DocumentChunker.chunk(mixedText, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assert(r.totalChunks >= 3, "Should detect all sentence types")
    }
  }

  test("DocumentChunker should provide chunk statistics") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Character,
      chunkSize = 50
    )
    val result = DocumentChunker.chunk(sampleText, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assert(r.avgSize > 0, "Should calculate average size")
      assert(r.minSize > 0, "Should calculate min size")
      assert(r.maxSize > 0, "Should calculate max size")
      assert(r.minSize <= r.avgSize, "Min should be <= avg")
      assert(r.avgSize <= r.maxSize, "Avg should be <= max")
    }
  }

  test("DocumentChunker should index chunks sequentially") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Sentence,
      chunkSize = 2
    )
    val result = DocumentChunker.chunk(sampleText, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      val indices = r.chunks.map(_.index)
      assertEquals(
        indices,
        (0 until r.totalChunks).toList,
        "Chunks should be indexed 0, 1, 2, ..."
      )
    }
  }

  test("DocumentChunker should handle real-world document") {
    val document =
      """Artificial intelligence (AI) is intelligence demonstrated by machines,
in contrast to the natural intelligence displayed by humans and animals. Leading AI
textbooks define the field as the study of "intelligent agents": any device that
perceives its environment and takes actions that maximize its chance of successfully
achieving its goals.

Colloquially, the term "artificial intelligence" is often used to describe machines
that mimic "cognitive" functions that humans associate with the human mind, such as
"learning" and "problem solving". As machines become increasingly capable, tasks
considered to require "intelligence" are often removed from the definition of AI,
a phenomenon known as the AI effect.

Modern machine learning algorithms are widely used in various applications including
email filtering, computer vision, and natural language processing. These algorithms
can be trained on large amounts of data to recognize patterns and make predictions."""

    val config = ChunkConfig(
      strategy = ChunkStrategy.Paragraph,
      chunkSize = 1,
      overlap = 0,
      minChunkSize = 50
    )
    val result = DocumentChunker.chunk(document, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assertEquals(r.totalChunks, 3, "Should create 3 paragraph chunks")
    }
  }

  test("DocumentChunker should handle document with varied sentence lengths") {
    val variedText = """Short. This is a medium length sentence with more words.
This is a very long sentence that contains many words and phrases to demonstrate
how the chunker handles sentences of varying lengths and complexities."""

    val config = ChunkConfig(
      strategy = ChunkStrategy.Sentence,
      chunkSize = 1
    )
    val result = DocumentChunker.chunk(variedText, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      assert(r.maxSize > r.minSize, "Should show size variation")
    }
  }

  test("DocumentChunker chunks should have correct size property") {
    val config = ChunkConfig(
      strategy = ChunkStrategy.Character,
      chunkSize = 50
    )
    val result = DocumentChunker.chunk(sampleText, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      r.chunks.foreach { chunk =>
        assertEquals(
          chunk.size,
          chunk.content.length,
          "Chunk size should match content length"
        )
      }
    }
  }

  test("DocumentChunker should work with DocumentIndexer pattern") {
    // Simulating a pipeline use case
    val config = ChunkConfig(
      strategy = ChunkStrategy.Paragraph,
      chunkSize = 1
    )
    val result = DocumentChunker.chunk(sampleParagraphs, config)

    assert(result.isRight, "Should succeed")
    result.foreach { r =>
      // Each chunk could be indexed separately
      val documentsToIndex = r.chunks.map { chunk =>
        (s"doc-${chunk.index}", chunk.content)
      }

      assertEquals(
        documentsToIndex.size,
        r.totalChunks,
        "Should create indexable documents"
      )
      assert(
        documentsToIndex.forall(_._2.nonEmpty),
        "All documents should have content"
      )
    }
  }

  test("ChunkResult statistics should be zero for empty result") {
    val emptyResult = ChunkResult(List.empty)

    assertEquals(emptyResult.totalChunks, 0)
    assertEquals(emptyResult.avgSize, 0)
    assertEquals(emptyResult.minSize, 0)
    assertEquals(emptyResult.maxSize, 0)
  }

  test("ChunkResult should calculate correct statistics") {
    val chunks = List(
      Chunk(0, "A" * 10, 10),
      Chunk(1, "B" * 20, 20),
      Chunk(2, "C" * 30, 30)
    )
    val result = ChunkResult(chunks)

    assertEquals(result.totalChunks, 3)
    assertEquals(result.avgSize, 20)
    assertEquals(result.minSize, 10)
    assertEquals(result.maxSize, 30)
  }
}

package dev.alteration.branch.testkit.fixtures

import dev.alteration.branch.veil.Veil
import munit.FunSuite

import scala.language.postfixOps
import scala.sys.process.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.{CountDownLatch, TimeUnit}

trait LlamaServerFixture extends FunSuite {

  // Override if you don't want this ENV controlled
  override def munitIgnore: Boolean = Veil.get("LLAMA_SERVER_TEST").isEmpty

  // Override this in your test if you dont want in ENV controlled
  val shouldStartLlamaServer: Boolean = Veil.get("LLAMA_SERVER_START").isDefined

  val cmd: Array[String] =
    "llama-server -hf ggml-org/gpt-oss-20b-GGUF --ctx-size 8192 --jinja -ub 2048 -b 2048 --embeddings --pooling mean"
      .split(" ")

  val processBuilder: ProcessBuilder = Process(cmd.head, cmd.tail)
  var process: Process               = null

  private val readyLatch = new CountDownLatch(1)

  override def beforeAll(): Unit = {
    val processIO = new ProcessIO(
      stdin => stdin.close(),
      stdout => {
        val reader = new BufferedReader(new InputStreamReader(stdout))
        var line   = reader.readLine()
        while (line != null) {
          println(s"[llama-server] $line")
          if (line.contains("all slots are idle")) {
            readyLatch.countDown()
          }
          line = reader.readLine()
        }
        reader.close()
      },
      stderr => { // Don't know why yet, but output is coming over stderr
        val reader = new BufferedReader(new InputStreamReader(stderr))
        var line   = reader.readLine()
        while (line != null) {
          System.err.println(s"[llama-server] $line")
          if (line.contains("all slots are idle")) {
            readyLatch.countDown()
          }
          line = reader.readLine()
        }
        reader.close()
      }
    )

    if shouldStartLlamaServer then {
      process = processBuilder.run(processIO)

      // Wait for server to be ready
      readyLatch.await(30, TimeUnit.SECONDS)
    } else {
      readyLatch.countDown()
    }

  }

  override def afterAll(): Unit = {
    if shouldStartLlamaServer then process.destroy()
  }

}

package examples

import dev.alteration.branch.hollywood.tools.{ToolPolicy, ToolRegistry}
import dev.alteration.branch.hollywood.tools.provided.{FileSystemTool, HttpClientTool}
import java.nio.file.Paths
import scala.util.Try

object RestrictedToolExample {

  def main(args: Array[String]): Unit = {
    // Example 1: FileSystem with built-in policy
    val fsPolicy = dev.alteration.branch.hollywood.tools.FileSystemPolicy.strict(
      Paths.get("/tmp")
    )
    val registry1 = ToolRegistry().registerFileSystem(fsPolicy)
    println("Registered FileSystemTool with strict policy")

    // Example 2: HttpClient with custom policy - only allow GET to example.com
    val httpPolicy = ToolPolicy.fromValidator[HttpClientTool] { tool =>
      if (tool.method.toLowerCase != "get") {
        Try(throw new SecurityException("Only GET requests allowed"))
      } else if (!tool.url.contains("example.com")) {
        Try(throw new SecurityException("Only example.com domain allowed"))
      } else {
        Try(())
      }
    }
    val registry2 = ToolRegistry().registerWithPolicy(httpPolicy)
    println("Registered HttpClientTool with custom GET-only policy")

    // Example 3: Custom policy with transformation
    val sanitizingPolicy = ToolPolicy.custom[HttpClientTool](
      validator = { tool =>
        // Validate HTTPS only
        if (!tool.url.startsWith("https://")) {
          Try(throw new SecurityException("Only HTTPS URLs allowed"))
        } else {
          Try(())
        }
      },
      transformer = { args =>
        // Could add default headers, sanitize inputs, etc.
        args
      }
    )
    val registry3 = ToolRegistry().registerWithPolicy(sanitizingPolicy)
    println("Registered HttpClientTool with HTTPS-only policy")

    // Example 4: denyAll - completely block a tool
    val blockedPolicy = ToolPolicy.denyAll[HttpClientTool](
      "External HTTP requests disabled in production"
    )
    val registry4 = ToolRegistry().registerWithPolicy(blockedPolicy)
    println("Registered HttpClientTool with denyAll policy (completely blocked)")

    // Example 5: allowAll - no restrictions (development mode)
    val permissivePolicy = ToolPolicy.allowAll[HttpClientTool]
    val registry5 = ToolRegistry().registerWithPolicy(permissivePolicy)
    println("Registered HttpClientTool with allowAll policy (no restrictions)")
  }
}

package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.JsonDecoder
import dev.alteration.branch.hollywood.tools.provided.FileSystemTool

/** A ToolExecutor wrapper that enforces FileSystemPolicy on FileSystemTool
  * operations
  *
  * This is a type alias for RestrictedExecutor specialized for FileSystemTool.
  * Kept for backward compatibility.
  *
  * @param delegate
  *   The underlying FileSystemTool executor
  * @param policy
  *   The security policy to enforce
  */
class SafeFileSystemExecutor(
    delegate: ToolExecutor[FileSystemTool],
    policy: FileSystemPolicy
)(using decoder: JsonDecoder[FileSystemTool])
    extends RestrictedExecutor[FileSystemTool](delegate, policy)

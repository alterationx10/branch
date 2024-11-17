package dev.wishingtree.branch.keanu.actors

import java.util.concurrent.{BlockingQueue, CompletableFuture}

case class ActorRef(
    private[actors] val mailBox: BlockingQueue[Any],
    private[actors] val actorFuture: CompletableFuture[Any]
) {

  def tell(msg: Any): Unit = mailBox.put(msg)

  private[actors] def restart(actorFuture: CompletableFuture[Any]): ActorRef =
    ActorRef(mailBox, actorFuture)
}

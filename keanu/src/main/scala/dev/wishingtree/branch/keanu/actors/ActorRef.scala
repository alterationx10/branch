package dev.wishingtree.branch.keanu.actors

import java.util.concurrent.{BlockingQueue, CompletableFuture}

case class ActorRef(
    private[actors] val mailBox: BlockingQueue[Any]
) {

  def tell(msg: Any): Unit = mailBox.put(msg)

}

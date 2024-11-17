package dev.wishingtree.branch.keanu.actors

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}


trait Actor {
  def onMsg: PartialFunction[Any, Any]
}

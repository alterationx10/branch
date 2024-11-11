package app.wishingtree

import dev.wishingtree.branch.blammo.JsonConsoleLogger

import java.util.logging.Level
import scala.util.*

object BlammoExample extends JsonConsoleLogger {
  
  def main(args: Array[String]): Unit = {
    
    logger.info("Just a message!")

    Try(1 / 0) match {
      case Failure(exception) => logger.log(Level.SEVERE, "oh no", exception)
      case _ => ()
    }
    
  }

}

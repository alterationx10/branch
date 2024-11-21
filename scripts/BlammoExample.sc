import dev.wishingtree.branch.blammo.JsonConsoleLogger

import java.util.logging.Level
import scala.util.*

object BlammoExample extends JsonConsoleLogger
BlammoExample.logger.info("Just a message!")

Try(1 / 0) match {
  case Failure(exception) =>
    BlammoExample.logger.log(Level.SEVERE, "oh no", exception)
  case _                  => ()
}

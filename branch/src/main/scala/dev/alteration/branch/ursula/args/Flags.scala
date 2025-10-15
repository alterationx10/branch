package dev.alteration.branch.ursula.args

import java.nio.file.Path

/** Factory methods for creating common flag types with less boilerplate.
  *
  * Example usage:
  * {{{
  * val PortFlag = Flags.int("port", "p", "Server port", default = 8080)
  * val WatchFlag = Flags.boolean("watch", "w", "Watch for changes")
  * val DirFlag = Flags.path("dir", "d", "Directory path", default = Path.of("./"))
  * }}}
  */
object Flags {

  /** Create a String flag
    * @param name
    *   The long flag name (used as --name)
    * @param shortKey
    *   The short flag key (used as -shortKey)
    * @param description
    *   Description for help text
    * @param default
    *   Optional default value
    * @param required
    *   Whether this flag is required
    * @param hidden
    *   Whether to hide from help
    * @param options
    *   Optional set of allowed values
    * @return
    *   A StringFlag instance
    */
  def string(
      name: String,
      shortKey: String,
      description: String,
      default: Option[String] = None,
      required: Boolean = false,
      hidden: Boolean = false,
      options: Option[Set[String]] = None
  ): StringFlag = {
    val _name = name
    val _shortKey = shortKey
    val _description = description
    val _default = default
    val _required = required
    val _hidden = hidden
    val _options = options

    new StringFlag {
      override val name: String                = _name
      override val shortKey: String            = _shortKey
      override val description: String         = _description
      override val default: Option[String]     = _default
      override val required: Boolean           = _required
      override val hidden: Boolean             = _hidden
      override val options: Option[Set[String]] = _options
    }
  }

  /** Create an Int flag
    * @param name
    *   The long flag name (used as --name)
    * @param shortKey
    *   The short flag key (used as -shortKey)
    * @param description
    *   Description for help text
    * @param default
    *   Optional default value
    * @param required
    *   Whether this flag is required
    * @param hidden
    *   Whether to hide from help
    * @return
    *   An IntFlag instance
    */
  def int(
      name: String,
      shortKey: String,
      description: String,
      default: Option[Int] = None,
      required: Boolean = false,
      hidden: Boolean = false
  ): IntFlag = {
    val _name = name
    val _shortKey = shortKey
    val _description = description
    val _default = default
    val _required = required
    val _hidden = hidden

    new IntFlag {
      override val name: String         = _name
      override val shortKey: String     = _shortKey
      override val description: String  = _description
      override val default: Option[Int] = _default
      override val required: Boolean    = _required
      override val hidden: Boolean      = _hidden
    }
  }

  /** Create a Boolean flag (presence/absence flag)
    * @param name
    *   The long flag name (used as --name)
    * @param shortKey
    *   The short flag key (used as -shortKey)
    * @param description
    *   Description for help text
    * @param hidden
    *   Whether to hide from help
    * @return
    *   A BooleanFlag instance
    */
  def boolean(
      name: String,
      shortKey: String,
      description: String,
      hidden: Boolean = false
  ): BooleanFlag = {
    val _name = name
    val _shortKey = shortKey
    val _description = description
    val _hidden = hidden

    new BooleanFlag {
      override val name: String        = _name
      override val shortKey: String    = _shortKey
      override val description: String = _description
      override val hidden: Boolean     = _hidden
    }
  }

  /** Create a Path flag
    * @param name
    *   The long flag name (used as --name)
    * @param shortKey
    *   The short flag key (used as -shortKey)
    * @param description
    *   Description for help text
    * @param default
    *   Optional default path
    * @param required
    *   Whether this flag is required
    * @param hidden
    *   Whether to hide from help
    * @param parser
    *   Custom parser for converting String to Path. Defaults to Path.of(_)
    * @return
    *   A Flag[Path] instance
    */
  def path(
      name: String,
      shortKey: String,
      description: String,
      default: Option[Path] = None,
      required: Boolean = false,
      hidden: Boolean = false,
      parser: String => Path = s => Path.of(s)
  ): Flag[Path] = {
    val _name = name
    val _shortKey = shortKey
    val _description = description
    val _default = default
    val _required = required
    val _hidden = hidden
    val _parser = parser

    new Flag[Path] {
      override val name: String          = _name
      override val shortKey: String      = _shortKey
      override val description: String   = _description
      override val default: Option[Path] = _default
      override val required: Boolean     = _required
      override val hidden: Boolean       = _hidden
      override def parse: PartialFunction[String, Path] = { case s => _parser(s) }
    }
  }

  /** Create a custom flag with a provided parser
    * @param name
    *   The long flag name (used as --name)
    * @param shortKey
    *   The short flag key (used as -shortKey)
    * @param description
    *   Description for help text
    * @param parser
    *   Function to parse String to type R
    * @param default
    *   Optional default value
    * @param required
    *   Whether this flag is required
    * @param hidden
    *   Whether to hide from help
    * @param options
    *   Optional set of allowed values
    * @return
    *   A Flag[R] instance
    */
  def custom[R](
      name: String,
      shortKey: String,
      description: String,
      parser: String => R,
      default: Option[R] = None,
      required: Boolean = false,
      hidden: Boolean = false,
      options: Option[Set[R]] = None
  ): Flag[R] = {
    val _name = name
    val _shortKey = shortKey
    val _description = description
    val _parser = parser
    val _default = default
    val _required = required
    val _hidden = hidden
    val _options = options

    new Flag[R] {
      override val name: String            = _name
      override val shortKey: String        = _shortKey
      override val description: String     = _description
      override val default: Option[R]      = _default
      override val required: Boolean       = _required
      override val hidden: Boolean         = _hidden
      override val options: Option[Set[R]] = _options
      override def parse: PartialFunction[String, R] = { case s => _parser(s) }
    }
  }
}

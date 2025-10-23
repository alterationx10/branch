package dev.alteration.branch.ursula.args

import java.nio.file.Path

/** Factory methods for creating common argument types with less boilerplate.
  *
  * Example usage:
  * {{{
  * val PortArg = Arguments.int("port", "Server port", default = Some(8080))
  * val NameArg = Arguments.string("name", "User name", required = true)
  * val DirArg = Arguments.path("directory", "Directory path", default = Some(Path.of("./")))
  * }}}
  */
object Arguments {

  /** Create a String argument
    * @param name
    *   The argument name (for help text)
    * @param description
    *   Description for help text
    * @param default
    *   Optional default value
    * @param required
    *   Whether this argument is required
    * @param hidden
    *   Whether to hide from help
    * @param options
    *   Optional set of allowed values
    * @return
    *   An Argument[String] instance
    */
  def string(
      name: String,
      description: String,
      default: Option[String] = None,
      required: Boolean = false,
      hidden: Boolean = false,
      options: Option[Set[String]] = None
  ): Argument[String] = {
    val _name        = name
    val _description = description
    val _default     = default
    val _required    = required
    val _hidden      = hidden
    val _options     = options

    new Argument[String] {
      override val name: String                           = _name
      override val description: String                    = _description
      override val default: Option[String]                = _default
      override val required: Boolean                      = _required
      override val hidden: Boolean                        = _hidden
      override val options: Option[Set[String]]           = _options
      override def parse: PartialFunction[String, String] = { case s => s }
    }
  }

  /** Create an Int argument
    * @param name
    *   The argument name (for help text)
    * @param description
    *   Description for help text
    * @param default
    *   Optional default value
    * @param required
    *   Whether this argument is required
    * @param hidden
    *   Whether to hide from help
    * @param options
    *   Optional set of allowed values
    * @return
    *   An Argument[Int] instance
    */
  def int(
      name: String,
      description: String,
      default: Option[Int] = None,
      required: Boolean = false,
      hidden: Boolean = false,
      options: Option[Set[Int]] = None
  ): Argument[Int] = {
    val _name        = name
    val _description = description
    val _default     = default
    val _required    = required
    val _hidden      = hidden
    val _options     = options

    new Argument[Int] {
      override val name: String                        = _name
      override val description: String                 = _description
      override val default: Option[Int]                = _default
      override val required: Boolean                   = _required
      override val hidden: Boolean                     = _hidden
      override val options: Option[Set[Int]]           = _options
      override def parse: PartialFunction[String, Int] = { case s => s.toInt }
    }
  }

  /** Create a Double argument
    * @param name
    *   The argument name (for help text)
    * @param description
    *   Description for help text
    * @param default
    *   Optional default value
    * @param required
    *   Whether this argument is required
    * @param hidden
    *   Whether to hide from help
    * @param options
    *   Optional set of allowed values
    * @return
    *   An Argument[Double] instance
    */
  def double(
      name: String,
      description: String,
      default: Option[Double] = None,
      required: Boolean = false,
      hidden: Boolean = false,
      options: Option[Set[Double]] = None
  ): Argument[Double] = {
    val _name        = name
    val _description = description
    val _default     = default
    val _required    = required
    val _hidden      = hidden
    val _options     = options

    new Argument[Double] {
      override val name: String                           = _name
      override val description: String                    = _description
      override val default: Option[Double]                = _default
      override val required: Boolean                      = _required
      override val hidden: Boolean                        = _hidden
      override val options: Option[Set[Double]]           = _options
      override def parse: PartialFunction[String, Double] = { case s =>
        s.toDouble
      }
    }
  }

  /** Create a Path argument
    * @param name
    *   The argument name (for help text)
    * @param description
    *   Description for help text
    * @param default
    *   Optional default path
    * @param required
    *   Whether this argument is required
    * @param hidden
    *   Whether to hide from help
    * @param parser
    *   Custom parser for converting String to Path. Defaults to Path.of(_)
    * @return
    *   An Argument[Path] instance
    */
  def path(
      name: String,
      description: String,
      default: Option[Path] = None,
      required: Boolean = false,
      hidden: Boolean = false,
      parser: String => Path = s => Path.of(s)
  ): Argument[Path] = {
    val _name        = name
    val _description = description
    val _default     = default
    val _required    = required
    val _hidden      = hidden
    val _parser      = parser

    new Argument[Path] {
      override val name: String                         = _name
      override val description: String                  = _description
      override val default: Option[Path]                = _default
      override val required: Boolean                    = _required
      override val hidden: Boolean                      = _hidden
      override def parse: PartialFunction[String, Path] = { case s =>
        _parser(s)
      }
    }
  }

  /** Create a custom argument with a provided parser
    * @param name
    *   The argument name (for help text)
    * @param description
    *   Description for help text
    * @param parser
    *   Function to parse String to type R
    * @param default
    *   Optional default value
    * @param required
    *   Whether this argument is required
    * @param hidden
    *   Whether to hide from help
    * @param options
    *   Optional set of allowed values
    * @return
    *   An Argument[R] instance
    */
  def custom[R](
      name: String,
      description: String,
      parser: String => R,
      default: Option[R] = None,
      required: Boolean = false,
      hidden: Boolean = false,
      options: Option[Set[R]] = None
  ): Argument[R] = {
    val _name        = name
    val _description = description
    val _parser      = parser
    val _default     = default
    val _required    = required
    val _hidden      = hidden
    val _options     = options

    new Argument[R] {
      override val name: String                      = _name
      override val description: String               = _description
      override val default: Option[R]                = _default
      override val required: Boolean                 = _required
      override val hidden: Boolean                   = _hidden
      override val options: Option[Set[R]]           = _options
      override def parse: PartialFunction[String, R] = { case s => _parser(s) }
    }
  }
}

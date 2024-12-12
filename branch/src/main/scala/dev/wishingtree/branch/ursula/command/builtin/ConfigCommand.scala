package dev.wishingtree.branch.ursula.command.builtin

import dev.wishingtree.branch.ursula.AppConfig
import dev.wishingtree.branch.ursula.args.{Argument, BooleanFlag, Flag}
import dev.wishingtree.branch.ursula.command.Command

case object SetFlag extends BooleanFlag {

  override val shortKey: String = "s"

  override val name: String = "set"

  override val description: String = "Set the config value by key"

  override val exclusive: Option[Seq[Flag[?]]] = Option(
    Seq(
      GetFlag,
      DeleteFlag
    )
  )
}

case object GetFlag extends BooleanFlag {

  override val shortKey: String = "g"

  override val name: String = "get"

  override val description: String = "Get the config value by key"

  override val exclusive: Option[Seq[Flag[?]]] = Option(
    Seq(
      SetFlag,
      DeleteFlag
    )
  )
}

case object DeleteFlag extends BooleanFlag {

  override val shortKey: String = "d"

  override val name: String = "delete"

  override val description: String = "Remove a config value by key"

  override val exclusive: Option[Seq[Flag[?]]] = Option(
    Seq(
      GetFlag,
      SetFlag
    )
  )

}

case object KeyArg extends Argument[String] {

  override def parse: PartialFunction[String, String] = identity(_)

  override val name: String = "key"

  override val description: String = "The config key"

  override val required: Boolean = true

}

case object ValueArg extends Argument[String] {

  override def parse: PartialFunction[String, String] = identity(_)

  override val name: String = "value"

  override val description: String = "The config value"

}

object ConfigCommand extends Command {

  override val description: String = "Interact with the CLI config file"

  override val trigger: String = "config"

  override def action(args: Seq[String]): Unit = {
    val _args = stripFlags(args)

    if GetFlag.isPresent(args) then {
      _args.headOption.flatMap(a => AppConfig.get(a)) match {
        case Some(v) => println(v)
        case None    => println(s"key not set!")
      }
      return
    }

    if SetFlag.isPresent(args) then {
      AppConfig.set(_args.head, _args.last)
      return
    }

    if DeleteFlag.isPresent(args) then {
      AppConfig.delete(_args.head)
      return
    }

  }

  override val examples: Seq[String] = Seq(
    "config --set key value",
    "config --get key",
    "config --delete key"
  )

  override val flags: Seq[Flag[?]] = Seq(GetFlag, SetFlag, DeleteFlag)

  override val usage: String = "config [flag] [key] [?value]"

  override val arguments: Seq[Argument[?]] = Seq(KeyArg, ValueArg)

}

package dev.wishingtree.branch.ursula.command.builtin

import dev.wishingtree.branch.ursula.args.{Argument, BooleanFlag, Flag}

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

}

case object ValueArg extends Argument[String] {

  override def parse: PartialFunction[String, String] = identity(_)

  override val name: String = "value"

  override val description: String = "The config value"

}

//object ConfigCommand extends Command {
//
//  override val description: String = "Interact with the CLI config file"
//
//  override val trigger: String = "config"
//
//  override def action(
//                       args: Chunk[String]
//                     ): ZIO[UrsulaServices, Throwable, Unit] = for {
//    _args <- ZIO.attempt(stripFlags(args))
//    _     <- CliConfig
//      .get(_args.head)
//      .flatMap {
//        case Some(v) => Console.printLine(v)
//        case None    => Console.printLine(s"${_args.head} not set!")
//      }
//      .when(GetFlag.isPresent(args))
//    _     <- CliConfig
//      .set(_args.head, _args.last)
//      .when(SetFlag.isPresent(args))
//    _     <-
//      CliConfig.delete(_args.head).when(DeleteFlag.isPresent(args))
//  } yield ()
//
//  override val examples: Seq[String] = Seq(
//    "config --set key value",
//    "config --get key",
//    "config --delete key"
//  )
//
//  override val flags: Seq[Flag[?]] = Seq(GetFlag, SetFlag, DeleteFlag)
//
//  override val usage: String = "config [flag] [key] [?value]"
//
//  override val arguments: Seq[Argument[?]] = Seq(KeyArg, ValueArg)
//
//}

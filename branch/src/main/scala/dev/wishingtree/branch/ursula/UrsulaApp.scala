package dev.wishingtree.branch.ursula

import dev.wishingtree.branch.ursula.command.Command

trait UrsulaApp {

  /** A unique identifier, primarily used for the configuration directory. It
    * should be unique per app; CLI apps with the same appIdentifier will end up
    * sharing the same configuration.
    */
  val appIdentifier: String

  /** The directory where the configuration file will be stored. Defaults to
    * "~/.appIdentifier"
    */
  lazy val configDirectory: String =
    s"${java.lang.System.getProperty("user.home")}/.$appIdentifier"

  /** The name of the configuration file to be used. Defaults to "config.json"
    */
  lazy val configFile: String = "config.json"

  /** This setting determines whether the built in HelpCommand is the default
    * command. Defaults true, override to false if you want to use a different
    * Command as default.
    */
  val defaultHelp: Boolean = true

  /** This is a Seq of your Command implementations that you want your CLI to
    * have access to.
    */
  val commands: Seq[Command]

//  private lazy val builtInCommands: Seq[Command] = Seq(
//    HelpCommand(commands = commands :+ ConfigCommand, isDefault = defaultHelp),
//    ConfigCommand
//  )
//
//  private lazy val commandLayer: ZLayer[Any, Nothing, Seq[Command]] =
//    ZLayer.succeed(builtInCommands ++ commands)
//
//  /** Given the injected Seq[Command], parse out a Map keyed by the Command
//   * trigger. Warns if multiple commands use the same trigger.
//   */
//  private val commandMap: RIO[Seq[Command], Map[String, Command]] =
//    for {
//      map <- ZIO.serviceWith[Seq[Command]](_.groupBy(_.trigger))
//      _   <- ZIO.foreachDiscard(map.filter(_._2.size > 1).toList) { kv =>
//        ZIO.logWarning(s"""
//                Multiple commands injected with the same trigger - using first found:
//                  ${kv._1} =>
//                    ${kv._2.map(_.getClass.getSimpleName).mkString(", ")}
//                """)
//      }
//    } yield map.map { case (a, b) => (a, b.head) }
//
//  /** Given the injected Seq[Command], parse out the trigger key-words
//   */
//  private val triggerList: RIO[Seq[Command], Seq[String]] =
//    ZIO.serviceWith[Seq[Command]](_.map(_.trigger))
//
//  /** Given the injected Seq[Command], find the one flagged as default (if
//   * present). Warns if multiple Commands have been set as default.
//   */
//  private val findDefaultCommand: RIO[Seq[Command], Option[Command]] =
//    for {
//      default <- ZIO.serviceWith[Seq[Command]](_.filter(_.isDefaultCommand))
//      _       <- ZIO.when(default.size > 1) {
//        ZIO.logWarning(s"""
//          Multiple commands injected with isDefaultCommand=true - using the first:
//            ${default.map(_.getClass.getSimpleName).mkString(", ")}
//          """)
//      }
//    } yield default.headOption
//
//  /** The "main program" of Ursula, which wires everything together, and runs
//   * Commands based on the arguments passed in
//   */
//  private final val program: ZIO[
//    Seq[Command] & CliConfig & ZIOAppArgs,
//    Throwable,
//    ExitCode
//  ] =
//    for {
//      args       <- ZIOAppArgs.getArgs
//      trigger     = args.headOption
//      cmpMap     <- commandMap
//      drop1Ref   <- Ref.make[Boolean](true)
//      cmd        <-
//        ZIO
//          .fromOption(trigger.flatMap(t => cmpMap.get(t)))
//          .catchAll(_ =>
//            drop1Ref.set(false) *>
//              findDefaultCommand.someOrFail(
//                new Exception(
//                  "Could not find command from argument, and no default command provided"
//                )
//              )
//          )
//      shouldDrop <- drop1Ref.get
//      _          <- cmd
//        .processedAction(if (shouldDrop) args.tail else args)
//    } yield ExitCode.success
//
//  /** The entry point to the CLI, pre-wired
//   */
//  override final def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
//    program
//      .provideSome[ZIOAppArgs & Scope](
//        commandLayer ++ CliConfigLive.live(configDirectory, configFile)
//      )

}

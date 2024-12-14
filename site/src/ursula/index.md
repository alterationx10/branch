# Ursula

A slim framework to make Scala CLI apps.

## Anatomy of the Framework

Here is a general overview if of the pieces fit together.

### How it works: UrsulaApp

You only need to make an object that extends the `UrsulaApp` trait, and provide
a `Seq[Command]`, which are your actions you wish to be available in your
app. `UrsulaApp` has a final `main` method entrypoint, and does some processing automatically.
It parses the arguments passed, and uses that to pull out the `Command` provided, and runs
accordingly, passing on the arguments to it.

There are some [built in commands](#built-in-commands) provided, currently only the
`HelpCommand`, that are also automatically injected. This
means that even if you only have:

```scala
object App extends UrsulaApp {
  override val commands: Seq[Command] = Seq.empty
}
```

you already have a functioning cli app that has a `help` command that prints all
the available commands accepted (as little as they are so far).

At this point, you need only implement some `Command`s that wrap the
functionality you desire, and add them to the `commands: Seq`.

### Commands

There is a `trait Command` to extend, and the essence of this the
implementation of

```scala
def action(args: Seq[String]): Unit
```

You consolidate all of your logic you want to run in this method.
`Commands` are meant to be a one-off calls from the main entry point,
and generally not composed with other `Command`s, so the return type is `Unit`.

There are a few other items to implement, such as

```scala
val trigger: String
val description: String
val usage: String
val examples: Seq[String]
```

`trigger` is the String that should be used at the start of your CLI arguments
to call that particular command. The others are simple informational strings
about your command - and those are automatically used by the built-in help
command to print documentation!

Two other important things to declare are

```scala
val flags: Seq[Flag[?]]
val arguments: Seq[Argument[?]]
```

[Flags](#flags) and [Arguments](#arguments) are discussed below, but know that
they are simple traits to extend that help you parse/provide values for the
`args` passed in - and they too have some simple Strings to implement that
provide auto documentation for your app. At the end of the day, you can just
parse the `args` on your own in your ZIO logic - but usage of the `Flag`s
and`Arguments` should hopefully simplify things for your and your apps users.

#### Built-In Commands

- HelpCommand - handles the printing of documentation

### Flags

Flags (`trait Flag[R]`) are non-positional arguments passed to the command.
Flags can be generally used as either an argument flag, which expects the next
element in the command arguments to be parsed as type `R`, or boolean flags
which do not (i.e. present/not present).

Some general highlights are that it has things built in to

- parse argument(s) that can then be used in you `Command`
- declare conflicts with other flags
- declare requirements of other flags
- provide defaults, of ENV variables to be used

### Arguments

Arguments (`trait Argument[R]`) are _positional_ arguments passed to the
command, and are to be parsed to type `R`

Some general highlights are that you can encode the parsing logic.

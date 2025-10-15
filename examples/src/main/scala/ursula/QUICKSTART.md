# Ursula CLI - Quick Start Guide

Try these examples right now to see Ursula in action!

## 1. Modern Greeting CLI

```bash
# Basic greeting
sbt "examples/runMain ursula.advanced.ModernCliExample greet -n Alice"
# Output: 1. Hello, Alice!

# Loud greeting
sbt "examples/runMain ursula.advanced.ModernCliExample greet -n Bob --loud"
# Output: 1. HELLO, BOB!

# Repeat greeting
sbt "examples/runMain ursula.advanced.ModernCliExample greet -n Charlie -r 3"
# Output:
# 1. Hello, Charlie!
# 2. Hello, Charlie!
# 3. Hello, Charlie!
```

## 2. Math Operations

```bash
# Add numbers
sbt "examples/runMain ursula.advanced.ModernCliExample math --add 5 10 15"
# Output: Result: 30.0

# Multiply numbers (verbose mode)
sbt "examples/runMain ursula.advanced.ModernCliExample math -m 2 3 4 --verbose"
# Output:
# Multiplying: 2.0 × 3.0 × 4.0
# Result: 24.0
```

## 3. File Operations

```bash
# List files in current directory
sbt "examples/runMain ursula.filetools.FileToolsExample list"

# List files recursively
sbt "examples/runMain ursula.filetools.FileToolsExample list -d examples/src --recursive"

# Count Scala files
sbt "examples/runMain ursula.filetools.FileToolsExample count -d branch/src --type scala"

# Find files matching pattern
sbt "examples/runMain ursula.filetools.FileToolsExample find -p *Example*.scala --verbose"
```

## 4. Getting Help

Every command has built-in help:

```bash
# General help
sbt "examples/runMain ursula.advanced.ModernCliExample help"

# Command-specific help
sbt "examples/runMain ursula.advanced.ModernCliExample greet -h"
```

## What's Happening Under the Hood?

All these examples use:
- **CommandContext** for type-safe flag access (no `.get` calls!)
- **Flags factories** for concise flag definitions
- **Automatic validation** for required flags and options
- **Built-in help** generation from your command definitions

Compare the [basic example](basic/BasicCliExample.scala) (old style) with the [modern example](advanced/ModernCliExample.scala) (new style) to see the difference!

## Next Steps

1. Read the [README](README.md) for detailed documentation
2. Check out the example source code to see implementation details
3. Try modifying the examples to learn the API
4. Build your own CLI tool!

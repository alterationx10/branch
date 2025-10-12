# Veil Examples

Examples for the `veil` module, which provides configuration management for Scala applications.

## What is Veil?

Veil provides two main features:

1. **Environment Variable Management**: Load variables from `.env` files based on runtime environment
2. **Type-Safe Configuration**: Load JSON configuration files with automatic type derivation

## Examples

### Basic - Environment Variables

**Location**: `veil/basic/VeilBasicExample.scala`

Demonstrates how to:
- Read environment variables from `.env` files
- Switch between different environments (DEV, TEST, STAGING, PROD)
- Use `getFirst` to check multiple variable names
- Fall back to system environment variables

**Run**:
```bash
# Copy example env file
cp examples/.env.example .env

# Run the example
sbt "examples/runMain veil.basic.VeilBasicExample"

# Try different environments
export SCALA_ENV=TEST
cp examples/.env.test.example .env.test
sbt "examples/runMain veil.basic.VeilBasicExample"
```

### Config - Type-Safe JSON Configuration

**Location**: `veil/config/ConfigExample.scala`

Demonstrates how to:
- Define configuration case classes
- Load configuration from JSON files
- Automatically derive JSON decoders
- Load from files or resources

**Run**:
```bash
# Copy example config
cp examples/config.json.example config.json

# Run the example
sbt "examples/runMain veil.config.ConfigExample"
```

## Runtime Environments

Veil supports four runtime environments, controlled by the `SCALA_ENV` environment variable:

- **DEV** (default): Loads `.env`
- **TEST**: Loads `.env.test`
- **STAGING**: Loads `.env.staging`
- **PROD**: Loads `.env.prod`

## Directory Configuration

By default, Veil looks for `.env` files in the current working directory. You can override this with the `VEIL_ENV_DIR` environment variable:

```bash
export VEIL_ENV_DIR=/path/to/env/files
```

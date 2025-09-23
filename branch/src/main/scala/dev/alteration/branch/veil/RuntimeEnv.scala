package dev.alteration.branch.veil

/** An enum representing the level of the runtime environment.
  *
  * This enum defines three possible runtime environments:
  *   - `DEV`: Development environment
  *   - `TEST`: Testing environment
  *   - `PROD`: Production environment
  */
enum RuntimeEnv {

  /** Development environment */
  case DEV

  /** Testing environment */
  case TEST

  /** Production environment */
  case PROD
}

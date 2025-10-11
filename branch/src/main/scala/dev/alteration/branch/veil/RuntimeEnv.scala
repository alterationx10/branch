package dev.alteration.branch.veil

/** An enum representing the level of the runtime environment.
  *
  * This enum defines four possible runtime environments:
  *   - `DEV`: Development environment
  *   - `TEST`: Testing environment
  *   - `STAGING`: Staging environment
  *   - `PROD`: Production environment
  */
enum RuntimeEnv {

  /** Development environment */
  case DEV

  /** Testing environment */
  case TEST

  /** Staging environment */
  case STAGING

  /** Production environment */
  case PROD
}

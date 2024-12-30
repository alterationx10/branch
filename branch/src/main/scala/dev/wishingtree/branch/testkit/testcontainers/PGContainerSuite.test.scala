//> using target.scope test
package dev.wishingtree.branch.testkit.testcontainers

import dev.wishingtree.branch.macaroni.poolers.ResourcePool
import munit.*
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy

import java.sql.Connection
import java.time.Duration
import javax.sql.DataSource

class PGContainerSuite extends munit.FunSuite {

  /** If true, a new container will be created for each test. If false, then a
    * single container will be created for all tests.
    */
  val containerPerTest = true

  case class PgConnectionPool(ds: DataSource) extends ResourcePool[Connection] {

    override def acquire: Connection = {
      ds.getConnection()
    }

    override def release(resource: Connection): Unit = {
      resource.close()
    }

    override def test(resource: Connection): Boolean =
      resource.isValid(5)
  }

  case class PGTestContainer() extends GenericContainer("postgres:latest") {
    withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
    withExposedPorts(5432)
  }

  var container: PGTestContainer = null

  override def beforeEach(context: BeforeEach): Unit = {
    if (containerPerTest) {
      container = PGTestContainer()
      // TODO Find a better way to wait for the pg to be ready
      // https://java.testcontainers.org/features/startup_and_waits/#wait-strategies
      container.withStartupCheckStrategy(
        new MinimumDurationRunningStartupCheckStrategy(Duration.ofSeconds(2))
      )
      container.start()
    }
  }

  override def afterEach(context: AfterEach): Unit = {
    if (containerPerTest) {
      container.stop()
    }
  }

  override def beforeAll(): Unit = {
    if (!containerPerTest) {
      container = PGTestContainer()
      container.start()
    }
  }

  override def afterAll(): Unit = {
    if (containerPerTest) {
      container.stop()
    }
  }

  /** Returns a DataSource for the test container.
    * @return
    */
  def ds: PGSimpleDataSource = {
    val _ds = new PGSimpleDataSource()
    _ds.setURL(
      s"jdbc:postgresql://${container.getHost}:${container.getMappedPort(5432)}/postgres"
    )
    _ds.setUser("postgres")
    _ds
  }

  def pgPool: PgConnectionPool =
    PgConnectionPool(ds)
}

package dev.alteration.branch.testkit.testcontainers

import dev.alteration.branch.macaroni.poolers.ResourcePool
import munit.FunSuite
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy

import java.sql.Connection
import java.time.Duration
import javax.sql.DataSource

trait PGContainerSuite extends FunSuite {

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

  private val waitStrategy = new LogMessageWaitStrategy()
    .withRegEx(".*database system is ready to accept connections.*\\s")
    .withTimes(2)
    .withStartupTimeout(Duration.ofSeconds(5))

  override def beforeEach(context: BeforeEach): Unit = {
    if (containerPerTest) {
      container = PGTestContainer()
      // https://java.testcontainers.org/features/startup_and_waits/#wait-strategies
      // https://github.com/testcontainers/testcontainers-java/blob/main/modules/postgresql/src/main/java/org/testcontainers/containers/PostgreSQLContainer.java
      container.setWaitStrategy(waitStrategy)
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
      container.setWaitStrategy(waitStrategy)
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

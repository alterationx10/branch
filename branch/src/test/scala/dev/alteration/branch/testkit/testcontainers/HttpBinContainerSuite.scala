package dev.alteration.branch.testkit.testcontainers

import munit.FunSuite
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy

trait HttpBinContainerSuite extends FunSuite {

  // Booting worker with pid
  case class HttpBinContainer()
      extends GenericContainer("kennethreitz/httpbin:latest") {
    withExposedPorts(80)
  }

  var container: HttpBinContainer = null

  def getContainerUrl = s"http://localhost:${container.getMappedPort(80)}"

  override def beforeAll(): Unit = {
    container = HttpBinContainer()
    container.setWaitStrategy(
      new HttpWaitStrategy()
        .forPort(80)
        .forStatusCode(200)
    )
    container.start()
  }

  override def afterAll(): Unit = {
    if (container != null) {
      container.stop()
    }
  }

}

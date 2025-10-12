package blammo.metrics

import dev.alteration.branch.blammo.Metrics

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
import scala.util.Random

/** An example showing how to expose application metrics via JMX.
  *
  * The Metrics API provides:
  * - Gauges: Values that can go up or down (current connections, queue size)
  * - Counters: Monotonically increasing values (total requests, errors)
  * - Histograms: Distribution/summary data (average response time, percentiles)
  *
  * Metrics are exposed via JMX and can be accessed by monitoring tools like:
  * - JConsole (ships with JDK)
  * - VisualVM
  * - JMX exporters for Prometheus
  *
  * To run this example:
  * 1. Run: sbt "examples/runMain blammo.metrics.MetricsExample"
  * 2. In another terminal, run: jconsole
  * 3. Connect to the running process
  * 4. Navigate to MBeans -> com.example.myapp -> type=ApiService -> Metrics
  * 5. Watch the metrics update in real-time
  *
  * You can also query metrics programmatically from within your app.
  */
object MetricsExample extends App {

  println("=== JMX Metrics Example ===\n")

  // Simulate some application state
  val activeConnections = new AtomicInteger(0)
  val requestCounter = new AtomicLong(0)
  val errorCounter = new AtomicLong(0)
  val responseTimes = scala.collection.mutable.Queue[Double]()
  val queueSize = new AtomicInteger(0)

  // Define metrics for our application
  val metrics = Metrics("ApiService", domain = "com.example.myapp")
    .gauge("ActiveConnections") { activeConnections.get() }
    .gauge("QueueSize") { queueSize.get() }
    .counter("TotalRequests") { requestCounter.get() }
    .counter("TotalErrors") { errorCounter.get() }
    .histogram("AvgResponseTime") {
      if (responseTimes.isEmpty) 0.0
      else responseTimes.sum / responseTimes.size
    }
  

  metrics.register() match {
    case scala.util.Success(_) =>
      println("✓ Metrics registered successfully!")
      println(
        "  JMX Name: com.example.myapp:type=ApiService,name=Metrics\n"
      )
    case scala.util.Failure(ex) =>
      println(s"✗ Failed to register metrics: ${ex.getMessage}")
      sys.exit(1)
  }

  println("Simulating application activity...")
  println("(Open JConsole to see metrics update in real-time)\n")

  // Simulate application activity
  val random = new Random()
  for (i <- 1 to 20) {
    // Simulate a request
    activeConnections.incrementAndGet()
    queueSize.set(random.nextInt(50))
    requestCounter.incrementAndGet()

    // Simulate processing time
    val responseTime = 50 + random.nextDouble() * 200
    responseTimes.enqueue(responseTime)
    if (responseTimes.size > 100) responseTimes.dequeue()

    // Simulate occasional errors
    if (random.nextDouble() < 0.1) {
      errorCounter.incrementAndGet()
    }

    Thread.sleep(500)
    activeConnections.decrementAndGet()

    // Print current metrics every 5 iterations
    if (i % 5 == 0) {
      println(s"--- Iteration $i ---")
      metrics.getCounter("TotalRequests").foreach(c =>
        println(s"  Total Requests: $c")
      )
      metrics.getCounter("TotalErrors").foreach(e =>
        println(s"  Total Errors: $e")
      )
      metrics.getHistogram("AvgResponseTime").foreach(rt =>
        println(f"  Avg Response Time: $rt%.2f ms")
      )
      metrics.getGauge("QueueSize").foreach(qs =>
        println(s"  Queue Size: $qs")
      )
      println()
    }
  }

  // You can also read all metrics at once
  println("=== Final Metrics Summary ===")
  metrics.getAllCounters.foreach { counters =>
    counters.foreach { case (name, value) =>
      println(s"  $name: $value")
    }
  }
  metrics.getAllHistograms.foreach { histograms =>
    histograms.foreach { case (name, value) =>
      println(f"  $name: $value%.2f")
    }
  }
  metrics.getAllGauges.foreach { gauges =>
    gauges.foreach { case (name, value) =>
      println(s"  $name: $value")
    }
  }

  println("\nKeeping process alive for 30 seconds...")
  println("(Open JConsole now to inspect metrics)")
  Thread.sleep(30000)

  // Clean up
  metrics.unregister()
  println("\n=== Example Complete ===")
}

package dev.alteration.branch.blammo

import java.lang.management.ManagementFactory
import javax.management.*
import scala.collection.mutable
import scala.util.Try

/** A builder for defining metrics that are exposed via JMX.
  *
  * Example usage:
  * {{{
  * val metrics = Metrics("Spider")
  *   .gauge("ActiveConnections") { connectionPool.size }
  *   .counter("RequestCount") { requestCounter.get() }
  *   .histogram("ResponseTime") { avgResponseTime }
  *   .register()
  * }}}
  *
  * Metrics will be available at:
  * `dev.alteration.branch:type=<module>,name=Metrics`
  */
case class Metrics(module: String) {
  private val gauges     = mutable.Map.empty[String, () => Any]
  private val counters   = mutable.Map.empty[String, () => Long]
  private val histograms = mutable.Map.empty[String, () => Double]

  private val mbs  = ManagementFactory.getPlatformMBeanServer
  private val name = new ObjectName(s"dev.alteration.branch:type=$module,name=Metrics")

  /** Add a gauge metric - a value that can go up or down
    * @param name
    *   The metric name
    * @param fn
    *   Function to retrieve the current value
    */
  def gauge(name: String)(fn: => Any): Metrics = {
    gauges(name) = () => fn
    this
  }

  /** Add a counter metric - a monotonically increasing value
    * @param name
    *   The metric name
    * @param fn
    *   Function to retrieve the current count
    */
  def counter(name: String)(fn: => Long): Metrics = {
    counters(name) = () => fn
    this
  }

  /** Add a histogram/summary metric - typically for timing/distribution data
    * @param name
    *   The metric name
    * @param fn
    *   Function to retrieve the current aggregated value
    */
  def histogram(name: String)(fn: => Double): Metrics = {
    histograms(name) = () => fn
    this
  }

  /** Register this metrics object with the platform MBean server
    * @return
    *   Success or Failure of registration
    */
  def register(): Try[Unit] = Try {
    // Unregister if already exists (useful for tests/reloading)
    if (mbs.isRegistered(name)) {
      mbs.unregisterMBean(name)
    }

    val mbean = new DynamicMBean {
      override def getAttribute(attribute: String): Object = {
        gauges.get(attribute).map(_()).orElse(
          counters.get(attribute).map(_())
        ).orElse(
          histograms.get(attribute).map(_())
        ).map(_.asInstanceOf[Object])
          .getOrElse(
            throw new AttributeNotFoundException(s"Attribute $attribute not found")
          )
      }

      override def getAttributes(attributes: Array[String]): AttributeList = {
        val list = new AttributeList()
        attributes.foreach { attr =>
          try {
            list.add(new Attribute(attr, getAttribute(attr)))
          } catch {
            case _: AttributeNotFoundException => // Skip missing attributes
          }
        }
        list
      }

      override def getMBeanInfo: MBeanInfo = {
        val allAttrs = (
          gauges.keys.map(name => new MBeanAttributeInfo(
            name,
            "java.lang.Object",
            s"Gauge: $name",
            true,  // readable
            false, // not writable
            false  // not is(...)
          )) ++
          counters.keys.map(name => new MBeanAttributeInfo(
            name,
            "java.lang.Long",
            s"Counter: $name",
            true,
            false,
            false
          )) ++
          histograms.keys.map(name => new MBeanAttributeInfo(
            name,
            "java.lang.Double",
            s"Histogram: $name",
            true,
            false,
            false
          ))
        ).toArray

        new MBeanInfo(
          this.getClass.getName,
          s"Metrics for $module",
          allAttrs,
          Array.empty, // no constructors
          Array.empty, // no operations
          Array.empty  // no notifications
        )
      }

      override def setAttribute(attribute: Attribute): Unit =
        throw new AttributeNotFoundException("No attributes are writable")

      override def setAttributes(attributes: AttributeList): AttributeList =
        new AttributeList()

      override def invoke(
          actionName: String,
          params: Array[Object],
          signature: Array[String]
      ): Object =
        throw new ReflectionException(
          new NoSuchMethodException(actionName),
          "No operations available"
        )
    }

    mbs.registerMBean(mbean, name)
  }

  /** Unregister this metrics object from the platform MBean server */
  def unregister(): Try[Unit] = Try {
    if (mbs.isRegistered(name)) {
      mbs.unregisterMBean(name)
    }
  }
}

package dev.alteration.branch.gooey.components.panels

import SlidingDirection.*

import java.awt.*
import java.awt.event.*
import javax.swing.*

/** A panel that can slide in and out of view with animation.
  *
  * @param direction
  *   The direction from which the panel slides in
  * @param panelSize
  *   The size of the panel (width for EAST/WEST, height for NORTH/SOUTH)
  */
class SlidingPanel(
    val direction: SlidingDirection,
    val panelSize: Int,
    initiallyExtended: Boolean = false,
    val animationDurationMs: Int = 250,
    val animationSteps: Int = 60
) extends JPanel {

  private var isPanelVisible        = initiallyExtended
  def isExtended: Boolean           = isPanelVisible
  private var animationTimer: Timer = scala.compiletime.uninitialized
  private def isAnimating: Boolean  =
    animationTimer != null && animationTimer.isRunning
  private val stepDelay             = animationDurationMs / animationSteps

  // Parent container resize listener
  private val parentResizeListener = new ComponentAdapter {
    override def componentResized(e: ComponentEvent): Unit = {
      setPreferredSize(Dimension(panelSize, panelSize)) // One will be ignored
    }
  }

  // Set minimum size to zero to allow complete collapse
  setMinimumSize(new Dimension(0, 0))

  // Set initial size and visibility
  setPreferredSize(Dimension(panelSize, panelSize))

  // Set up container hierarchy listener to attach resize handler
  private val hListener = new HierarchyListener {
    override def hierarchyChanged(e: HierarchyEvent): Unit = {
      if ((e.getChangeFlags & HierarchyEvent.PARENT_CHANGED) != 0) {
        val oldParent = e.getChangedParent
        val newParent = getParent

        // Remove listener from old parent if it exists
        if (oldParent != null) {
          oldParent.removeComponentListener(parentResizeListener)
        }

        // Add listener to new parent if it exists
        if (newParent != null) {
          newParent.addComponentListener(parentResizeListener)
          setPreferredSize(
            Dimension(panelSize, panelSize)
          ) // One will be ignored
        }
      }
    }
  }

  // Add the hierarchy listener to handle parent changes
  addHierarchyListener(hListener)

  override def setPreferredSize(preferredSize: Dimension): Unit = {
    Option(getParent) match {
      case Some(parent) =>
        direction match {
          case Horizontal => {
            val width =
              if (isAnimating || isPanelVisible) {
                preferredSize.width
              } else {
                0
              }
            super.setPreferredSize(new Dimension(width, parent.getHeight))
          }
          case Vertical   => {
            val height =
              if (isAnimating || isPanelVisible) {
                preferredSize.height
              } else {
                0
              }
            super.setPreferredSize(new Dimension(parent.getWidth, height))
          }
        }
      case None         => super.setPreferredSize(preferredSize)
    }
  }

  /** Slides the panel into view with animation.
    */
  def slideIn(): Unit = {
    if (isPanelVisible) return

    if (animationTimer != null) {
      animationTimer.stop()
    }

    // Update to use parent's dimensions
    val parent = getParent
    if (parent == null) return

    val targetSize = direction match {
      case Horizontal =>
        new Dimension(panelSize, parent.getHeight)
      case Vertical   =>
        new Dimension(parent.getWidth, panelSize)
    }

    val currentSize = getPreferredSize
    val stepSize    = direction match {
      case Horizontal =>
        (targetSize.width - currentSize.width) / animationSteps.toDouble
      case Vertical   =>
        (targetSize.height - currentSize.height) / animationSteps.toDouble
    }

    var step = 0

    animationTimer = new Timer(
      stepDelay,
      new ActionListener {
        override def actionPerformed(e: ActionEvent): Unit = {
          step += 1

          // Ensure parent is still available
          val parent = getParent
          if (parent == null) {
            animationTimer.stop()
            return
          }

          direction match {
            case Horizontal =>
              val newWidth = Math.min(
                targetSize.width,
                currentSize.width + (step * stepSize).toInt
              )
              setPreferredSize(new Dimension(newWidth, parent.getHeight))
            case Vertical   =>
              val newHeight = Math.min(
                targetSize.height,
                currentSize.height + (step * stepSize).toInt
              )
              setPreferredSize(new Dimension(parent.getWidth, newHeight))
          }

          parent.revalidate()

          if (step >= animationSteps) {
            animationTimer.stop()
            isPanelVisible = true
            setPreferredSize(
              Dimension(panelSize, panelSize)
            ) // One will be ignored
          }
        }
      }
    )

    animationTimer.start()
  }

  /** Slides the panel out of view with animation.
    */
  def slideOut(): Unit = {
    if (!isPanelVisible) return

    if (animationTimer != null) {
      animationTimer.stop()
    }

    // Update to use parent's dimensions
    val parent = getParent
    if (parent == null) return

    val currentSize = getPreferredSize
    val stepSize    = direction match {
      case Horizontal =>
        currentSize.width / animationSteps.toDouble
      case Vertical   =>
        currentSize.height / animationSteps.toDouble
    }

    var step = 0

    animationTimer = new Timer(
      stepDelay,
      new ActionListener {
        override def actionPerformed(e: ActionEvent): Unit = {
          step += 1

          // Ensure parent is still available
          val parent = getParent
          if (parent == null) {
            animationTimer.stop()
            return
          }

          direction match {
            case Horizontal =>
              val newWidth =
                Math.max(0, currentSize.width - (step * stepSize).toInt)
              setPreferredSize(new Dimension(newWidth, parent.getHeight))
            case Vertical   =>
              val newHeight =
                Math.max(0, currentSize.height - (step * stepSize).toInt)
              setPreferredSize(new Dimension(parent.getWidth, newHeight))
          }

          parent.revalidate()

          if (step >= animationSteps) {
            animationTimer.stop()
            isPanelVisible = false
            setPreferredSize(
              Dimension(panelSize, panelSize)
            ) // One will be ignored
          }
        }
      }
    )

    animationTimer.start()
  }

  /** Toggles the panel's visibility with animation.
    */
  def toggle(): Unit = {
    if (isPanelVisible) slideOut() else slideIn()
  }

}

package dev.wishingtree.branch.gooey

import java.awt.*
import java.awt.event.*
import javax.swing.*

/** A panel that can slide in and out of view with animation.
  *
  * @param direction
  *   The direction from which the panel slides in
  * @param content
  *   The content to display in the panel
  * @param panelSize
  *   The size of the panel (width for EAST/WEST, height for NORTH/SOUTH)
  */
class SlidingPanel(
    val direction: String,
    val panelSize: Int,
    initiallyVisible: Boolean = false
) extends JPanel {

  private var isPanelVisible        = initiallyVisible
  private var animationTimer: Timer = scala.compiletime.uninitialized
  private val animationDuration     = 250 // milliseconds
  private val animationSteps        = 60
  private val stepDelay             = animationDuration / animationSteps

  // Parent container resize listener
  private val parentResizeListener = new ComponentAdapter {
    override def componentResized(e: ComponentEvent): Unit = {
      setPreferredSize(Dimension(panelSize, panelSize)) // One will be ignored
    }
  }

  // Set minimum size to zero to allow complete collapse
  setMinimumSize(new Dimension(0, 0))

  // Set initial size and visibility
  direction match {
    case BorderLayout.WEST | BorderLayout.EAST   =>
      setPreferredSize(new Dimension(0, 0))
    case BorderLayout.NORTH | BorderLayout.SOUTH =>
      setPreferredSize(new Dimension(0, 0))
    case _                                       =>
      throw new IllegalArgumentException(s"Unsupported direction: $direction")
  }

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
          case BorderLayout.WEST | BorderLayout.EAST   => {
            if (isPanelVisible) {
              super.setPreferredSize(
                new Dimension(preferredSize.width, parent.getHeight)
              )
            } else {
              super.setPreferredSize(new Dimension(0, parent.getHeight))
            }
          }
          case BorderLayout.NORTH | BorderLayout.SOUTH => {
            if (isPanelVisible) {
              super.setPreferredSize(
                new Dimension(parent.getWidth, preferredSize.height)
              )
            } else {
              super.setPreferredSize(new Dimension(parent.getWidth, 0))
            }
          }
          case _                                       => super.setPreferredSize(Dimension(0, 0))
        }
      case None         => super.setPreferredSize(Dimension(0, 0))
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
      case BorderLayout.WEST | BorderLayout.EAST   =>
        new Dimension(panelSize, parent.getHeight)
      case BorderLayout.NORTH | BorderLayout.SOUTH =>
        new Dimension(parent.getWidth, panelSize)
    }

    val currentSize = getPreferredSize()
    val stepSize    = direction match {
      case BorderLayout.WEST | BorderLayout.EAST   =>
        (targetSize.width - currentSize.width) / animationSteps.toDouble
      case BorderLayout.NORTH | BorderLayout.SOUTH =>
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
            case BorderLayout.WEST | BorderLayout.EAST   =>
              val newWidth = Math.min(
                targetSize.width,
                currentSize.width + (step * stepSize).toInt
              )
              setPreferredSize(new Dimension(newWidth, parent.getHeight))
            case BorderLayout.NORTH | BorderLayout.SOUTH =>
              val newHeight = Math.min(
                targetSize.height,
                currentSize.height + (step * stepSize).toInt
              )
              setPreferredSize(new Dimension(parent.getWidth, newHeight))
          }

          parent.revalidate()
          parent.repaint()

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

    val targetSize = direction match {
      case BorderLayout.WEST | BorderLayout.EAST   =>
        new Dimension(0, parent.getHeight)
      case BorderLayout.NORTH | BorderLayout.SOUTH =>
        new Dimension(parent.getWidth, 0)
    }

    val currentSize = getPreferredSize
    val stepSize    = direction match {
      case BorderLayout.WEST | BorderLayout.EAST   =>
        currentSize.width / animationSteps.toDouble
      case BorderLayout.NORTH | BorderLayout.SOUTH =>
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
            case BorderLayout.WEST | BorderLayout.EAST   =>
              val newWidth =
                Math.max(0, currentSize.width - (step * stepSize).toInt)
              setPreferredSize(new Dimension(newWidth, parent.getHeight))
            case BorderLayout.NORTH | BorderLayout.SOUTH =>
              val newHeight =
                Math.max(0, currentSize.height - (step * stepSize).toInt)
              setPreferredSize(new Dimension(parent.getWidth, newHeight))
          }

          parent.revalidate()
          parent.repaint()

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

  /** Returns whether the panel is currently visible.
    */
  def isSlideVisible: Boolean = isPanelVisible

  /** Sets the panel visible or hidden immediately without animation
    */
  def setSlideVisible(visible: Boolean): Unit = {
    if (animationTimer != null) {
      animationTimer.stop()
    }

    isPanelVisible = visible
    setPreferredSize(Dimension(panelSize, panelSize)) // One will be ignored

    val parent = getParent
    if (parent != null) {
      parent.revalidate()
      parent.repaint()
    }
  }
}

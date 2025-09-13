package dev.wishingtree.branch.gooey

import java.awt.*
import java.awt.event.*
import javax.swing.*

/**
 * A panel that can slide in and out of view with animation.
 * 
 * @param direction The direction from which the panel slides in
 * @param content The content to display in the panel
 * @param panelWidth The width of the panel (for EAST/WEST)
 * @param panelHeight The height of the panel (for NORTH/SOUTH)
 */
class SlidingPanel(
    val direction: String,
    content: Component,
    val panelWidth: Int = 250,
    val panelHeight: Int = 150
) extends JPanel {

  private val contentPanel = new JPanel(new BorderLayout())
  contentPanel.add(content, BorderLayout.CENTER)

  // Add a border to make the panel more visually distinct
  contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))

  private var isPanelVisible = false
  private var animationTimer: Timer = scala.compiletime.uninitialized
  private val animationDuration = 250 // milliseconds
  private val animationSteps = 60
  private val stepDelay = animationDuration / animationSteps

  // Parent container resize listener
  private val parentResizeListener = new ComponentAdapter {
    override def componentResized(e: ComponentEvent): Unit = {
      updatePanelSize()
    }
  }

  setLayout(new BorderLayout())
  add(contentPanel, BorderLayout.CENTER)

  // Set initial size and visibility
  direction match {
    case BorderLayout.WEST | BorderLayout.EAST =>
      setPreferredSize(new Dimension(0, 0))
    case BorderLayout.NORTH | BorderLayout.SOUTH =>
      setPreferredSize(new Dimension(0, 0))
    case _ =>
      throw new IllegalArgumentException(s"Unsupported direction: $direction")
  }

  // Set up container hierarchy listener to attach resize handler
  val hListener = new HierarchyListener {
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
          updatePanelSize()
        }
      }
    }
  }

  // Add the hierarchy listener to handle parent changes
  addHierarchyListener(hListener)

  /**
   * Updates the panel size to match parent container
   */
  def updatePanelSize(): Unit = {
    val parent = getParent
    if (parent != null) {
      direction match {
        case BorderLayout.WEST | BorderLayout.EAST =>
          if (isPanelVisible) {
            setPreferredSize(new Dimension(panelWidth, parent.getHeight))
          } else {
            setPreferredSize(new Dimension(0, parent.getHeight))
          }
        case BorderLayout.NORTH | BorderLayout.SOUTH =>
          if (isPanelVisible) {
            setPreferredSize(new Dimension(parent.getWidth, panelHeight))
          } else {
            setPreferredSize(new Dimension(parent.getWidth, 0))
          }
      }

      // Set minimum size to zero to allow complete collapse
      setMinimumSize(new Dimension(0, 0))
    }
  }

  /**
   * Slides the panel into view with animation.
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
      case BorderLayout.WEST | BorderLayout.EAST => new Dimension(panelWidth, parent.getHeight)
      case BorderLayout.NORTH | BorderLayout.SOUTH => new Dimension(parent.getWidth, panelHeight)
    }

    val currentSize = getPreferredSize()
    val stepSize = direction match {
      case BorderLayout.WEST | BorderLayout.EAST => 
        (targetSize.width - currentSize.width) / animationSteps.toDouble
      case BorderLayout.NORTH | BorderLayout.SOUTH => 
        (targetSize.height - currentSize.height) / animationSteps.toDouble
    }

    var step = 0

    animationTimer = new Timer(stepDelay, new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit = {
        step += 1

        // Ensure parent is still available
        val parent = getParent
        if (parent == null) {
          animationTimer.stop()
          return
        }

        direction match {
          case BorderLayout.WEST | BorderLayout.EAST =>
            val newWidth = Math.min(targetSize.width, currentSize.width + (step * stepSize).toInt)
            setPreferredSize(new Dimension(newWidth, parent.getHeight))
          case BorderLayout.NORTH | BorderLayout.SOUTH =>
            val newHeight = Math.min(targetSize.height, currentSize.height + (step * stepSize).toInt)
            setPreferredSize(new Dimension(parent.getWidth, newHeight))
        }

        parent.revalidate()
        parent.repaint()

        if (step >= animationSteps) {
          animationTimer.stop()
          isPanelVisible = true
          updatePanelSize() // Final size update
        }
      }
    })

    animationTimer.start()
  }

  /**
   * Slides the panel out of view with animation.
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
      case BorderLayout.WEST | BorderLayout.EAST => new Dimension(0, parent.getHeight)
      case BorderLayout.NORTH | BorderLayout.SOUTH => new Dimension(parent.getWidth, 0)
    }

    val currentSize = getPreferredSize()
    val stepSize = direction match {
      case BorderLayout.WEST | BorderLayout.EAST => 
        currentSize.width / animationSteps.toDouble
      case BorderLayout.NORTH | BorderLayout.SOUTH => 
        currentSize.height / animationSteps.toDouble
    }

    var step = 0

    animationTimer = new Timer(stepDelay, new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit = {
        step += 1

        // Ensure parent is still available
        val parent = getParent
        if (parent == null) {
          animationTimer.stop()
          return
        }

        direction match {
          case BorderLayout.WEST | BorderLayout.EAST =>
            val newWidth = Math.max(0, currentSize.width - (step * stepSize).toInt)
            setPreferredSize(new Dimension(newWidth, parent.getHeight))
          case BorderLayout.NORTH | BorderLayout.SOUTH =>
            val newHeight = Math.max(0, currentSize.height - (step * stepSize).toInt)
            setPreferredSize(new Dimension(parent.getWidth, newHeight))
        }

        parent.revalidate()
        parent.repaint()

        if (step >= animationSteps) {
          animationTimer.stop()
          isPanelVisible = false
          updatePanelSize() // Final size update
        }
      }
    })

    animationTimer.start()
  }

  /**
   * Toggles the panel's visibility with animation.
   */
  def toggle(): Unit = {
    if (isPanelVisible) slideOut() else slideIn()
  }

  /**
   * Returns whether the panel is currently visible.
   */
  def isSlideVisible: Boolean = isPanelVisible

  /**
   * Sets the panel visible or hidden immediately without animation
   */
  def setSlideVisible(visible: Boolean): Unit = {
    if (animationTimer != null) {
      animationTimer.stop()
    }

    isPanelVisible = visible
    updatePanelSize()

    val parent = getParent
    if (parent != null) {
      parent.revalidate()
      parent.repaint()
    }
  }
}

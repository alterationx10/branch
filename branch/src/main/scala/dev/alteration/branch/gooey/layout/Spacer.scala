package dev.alteration.branch.gooey.layout

import javax.swing.{Box, JPanel}
import java.awt.Dimension

/** Spacer component for adding flexible or fixed space between components.
  *
  * When both width and height are None, creates a flexible "glue" spacer that
  * expands to fill available space. When specific dimensions are provided,
  * creates a rigid spacer of that size.
  *
  * @param width
  *   Optional fixed width in pixels
  * @param height
  *   Optional fixed height in pixels
  */
case class Spacer(width: Option[Int] = None, height: Option[Int] = None)
    extends Component {

  override def render(): RenderedComponent = {
    val spacer = (width, height) match {
      case (None, None) =>
        // Flexible glue spacer - wrap in JPanel since Box.Filler is not JComponent
        val panel = new JPanel()
        panel.setOpaque(false)
        panel.add(Box.createGlue())
        panel

      case (Some(w), None) =>
        // Horizontal strut - wrap in JPanel
        val panel = new JPanel()
        panel.setOpaque(false)
        panel.add(Box.createHorizontalStrut(w))
        panel

      case (None, Some(h)) =>
        // Vertical strut - wrap in JPanel
        val panel = new JPanel()
        panel.setOpaque(false)
        panel.add(Box.createVerticalStrut(h))
        panel

      case (Some(w), Some(h)) =>
        // Rigid area - wrap in JPanel
        val panel = new JPanel()
        panel.setOpaque(false)
        panel.setPreferredSize(new Dimension(w, h))
        panel
    }

    RenderedComponent(spacer)
  }

}

object Spacer {

  /** Creates a flexible spacer that expands to fill available space. */
  def flexible: Spacer = Spacer()

  /** Creates a horizontal spacer with the specified width.
    *
    * @param width
    *   Width in pixels
    */
  def horizontal(width: Int): Spacer = Spacer(width = Some(width))

  /** Creates a vertical spacer with the specified height.
    *
    * @param height
    *   Height in pixels
    */
  def vertical(height: Int): Spacer = Spacer(height = Some(height))

  /** Creates a rigid spacer with both width and height.
    *
    * @param width
    *   Width in pixels
    * @param height
    *   Height in pixels
    */
  def rigid(width: Int, height: Int): Spacer =
    Spacer(Some(width), Some(height))

}

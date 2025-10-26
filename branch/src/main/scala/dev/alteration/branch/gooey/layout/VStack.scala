package dev.alteration.branch.gooey.layout

import dev.alteration.branch.gooey.styling.Alignment

import javax.swing.{Box, BoxLayout, JPanel}

/** Vertical stack layout container.
  *
  * Arranges child components vertically with optional spacing and alignment.
  *
  * @param spacing
  *   Space between children in pixels (default: 0)
  * @param alignment
  *   Horizontal alignment of children (default: Leading)
  * @param children
  *   Child components to arrange vertically
  */
case class VStack(
    spacing: Int = 0,
    alignment: Alignment = Alignment.Leading,
    children: Component*
) extends Component {

  override def render(): RenderedComponent = {
    val panel = new JPanel()
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS))
    panel.setOpaque(false)

    children.zipWithIndex.foreach { case (child, idx) =>
      val rendered = child.render()
      val comp     = rendered.asJComponent

      // Set alignment
      comp.setAlignmentX(alignment.toAlignmentX)

      panel.add(comp)

      // Add spacing (except after last child)
      if (idx < children.size - 1 && spacing > 0) {
        panel.add(Box.createVerticalStrut(spacing))
      }
    }

    RenderedComponent(panel)
  }

}

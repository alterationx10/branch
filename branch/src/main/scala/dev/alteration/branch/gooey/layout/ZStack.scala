package dev.alteration.branch.gooey.layout

import dev.alteration.branch.gooey.styling.Alignment

import javax.swing.{JPanel, OverlayLayout}

/** Layered stack layout container.
  *
  * Stacks children on top of each other (Z-axis), with the first child at the
  * bottom and the last child on top.
  *
  * @param alignment
  *   Alignment of children within the stack (default: Center)
  * @param children
  *   Child components to layer
  */
case class ZStack(alignment: Alignment = Alignment.Center, children: Component*)
    extends Component {

  override def render(): RenderedComponent = {
    val panel = new JPanel()
    panel.setLayout(new OverlayLayout(panel))
    panel.setOpaque(false)

    // Add children in reverse order so first child is at bottom
    children.reverse.foreach { child =>
      val rendered = child.render()
      val comp     = rendered.asJComponent

      // Set alignment for overlay layout
      comp.setAlignmentX(alignment.toAlignmentX)
      comp.setAlignmentY(alignment.toAlignmentY)

      panel.add(comp)
    }

    RenderedComponent(panel)
  }

}

package dev.alteration.branch.gooey.modifiers

import dev.alteration.branch.gooey.layout.{Component, RenderedComponent}

import java.awt.Dimension

/** Sets explicit width and/or height constraints on a component.
  *
  * @param child
  *   The component to constrain
  * @param width
  *   Optional fixed width in pixels
  * @param height
  *   Optional fixed height in pixels
  */
case class FrameModifier(
    child: Component,
    width: Option[Int],
    height: Option[Int]
) extends Component {

  override def render(): RenderedComponent = {
    val childRendered = child.render()
    val component     = childRendered.asJComponent

    (width, height) match {
      case (Some(w), Some(h)) =>
        val size = new Dimension(w, h)
        component.setPreferredSize(size)
        component.setMinimumSize(size)
        component.setMaximumSize(size)

      case (Some(w), None) =>
        val currentHeight = component.getPreferredSize.height
        val size          = new Dimension(w, currentHeight)
        component.setPreferredSize(size)
        component.setMaximumSize(new Dimension(w, Int.MaxValue))

      case (None, Some(h)) =>
        val currentWidth = component.getPreferredSize.width
        val size         = new Dimension(currentWidth, h)
        component.setPreferredSize(size)
        component.setMaximumSize(new Dimension(Int.MaxValue, h))

      case (None, None) =>
      // No-op
    }

    childRendered
  }

}

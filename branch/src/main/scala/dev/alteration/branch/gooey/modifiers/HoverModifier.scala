package dev.alteration.branch.gooey.modifiers

import dev.alteration.branch.gooey.layout.{Component, RenderedComponent}

import java.awt.event.{MouseAdapter, MouseEvent}

/** Adds hover state tracking to a component.
  *
  * @param child
  *   The component to track hover for
  * @param handler
  *   The function to call with hover state (true = entered, false = exited)
  */
case class HoverModifier(child: Component, handler: Boolean => Unit)
    extends Component {

  override def render(): RenderedComponent = {
    val childRendered = child.render()
    val component     = childRendered.asJComponent

    // Add mouse listener for hover events
    component.addMouseListener(new MouseAdapter {
      override def mouseEntered(e: MouseEvent): Unit = {
        handler(true)
      }

      override def mouseExited(e: MouseEvent): Unit = {
        handler(false)
      }
    })

    childRendered
  }

}

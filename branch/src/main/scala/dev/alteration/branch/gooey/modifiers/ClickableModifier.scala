package dev.alteration.branch.gooey.modifiers

import dev.alteration.branch.gooey.layout.{Component, RenderedComponent}

import java.awt.Cursor
import java.awt.event.{MouseAdapter, MouseEvent}

/** Makes a component clickable with a click handler.
  *
  * @param child
  *   The component to make clickable
  * @param handler
  *   The function to call when clicked
  */
case class ClickableModifier(child: Component, handler: () => Unit)
    extends Component {

  override def render(): RenderedComponent = {
    val childRendered = child.render()
    val component     = childRendered.asJComponent

    // Set hand cursor to indicate clickability
    component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))

    // Add mouse listener
    component.addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit = {
        handler()
      }
    })

    childRendered
  }

}

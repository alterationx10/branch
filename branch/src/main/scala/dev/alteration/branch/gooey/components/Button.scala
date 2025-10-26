package dev.alteration.branch.gooey.components

import dev.alteration.branch.gooey.layout.{Component, RenderedComponent}

import javax.swing.JButton
import java.awt.event.ActionListener

/** Button component.
  *
  * A clickable button with text and an action handler.
  *
  * @param text
  *   The button text
  * @param action
  *   The function to call when the button is clicked
  */
case class Button(text: String, action: () => Unit) extends Component {

  override def render(): RenderedComponent = {
    val button = new JButton(text)

    button.addActionListener(new ActionListener {
      override def actionPerformed(e: java.awt.event.ActionEvent): Unit = {
        action()
      }
    })

    RenderedComponent(button)
  }

}

// No companion object needed - Button is already a case class

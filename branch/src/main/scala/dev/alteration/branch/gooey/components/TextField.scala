package dev.alteration.branch.gooey.components

import dev.alteration.branch.gooey.layout.{Component, RenderedComponent}

import javax.swing.{JPasswordField, JTextField}
import javax.swing.event.{DocumentEvent, DocumentListener}

/** Text field component for user input.
  *
  * Provides a single-line text input field with optional placeholder and change
  * handler.
  *
  * @param placeholder
  *   Placeholder text to display when empty (default: "")
  * @param value
  *   Initial value of the text field (default: "")
  * @param onChange
  *   Optional function to call when the text changes
  * @param isPassword
  *   If true, displays input as password (dots/asterisks)
  */
case class TextField(
    placeholder: String = "",
    value: String = "",
    onChange: Option[String => Unit] = None,
    isPassword: Boolean = false
) extends Component {

  override def render(): RenderedComponent = {
    val field =
      if (isPassword) new JPasswordField(value, 20)
      else new JTextField(value, 20)

    // Set placeholder (simple approach using text when empty)
    if (value.isEmpty && placeholder.nonEmpty) {
      // Note: Real placeholder would require custom rendering or third-party lib
      // For now, just set initial text
      field.setText(placeholder)
    }

    // Add document listener for changes
    onChange.foreach { handler =>
      field.getDocument.addDocumentListener(new DocumentListener {
        override def insertUpdate(e: DocumentEvent): Unit = {
          handler(field.getText)
        }

        override def removeUpdate(e: DocumentEvent): Unit = {
          handler(field.getText)
        }

        override def changedUpdate(e: DocumentEvent): Unit = {
          handler(field.getText)
        }
      })
    }

    RenderedComponent(field)
  }

  /** Creates a new TextField with the specified onChange handler.
    *
    * @param handler
    *   Function to call when text changes
    */
  def onChange(handler: String => Unit): TextField =
    copy(onChange = Some(handler))

  /** Creates a password field version of this TextField. */
  def password: TextField = copy(isPassword = true)

}

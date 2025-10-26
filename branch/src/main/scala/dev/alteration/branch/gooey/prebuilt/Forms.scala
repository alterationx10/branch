package dev.alteration.branch.gooey.prebuilt

import dev.alteration.branch.gooey.layout.{Component, HStack, VStack}
import dev.alteration.branch.gooey.components.{Text, TextField}
import dev.alteration.branch.gooey.modifiers.*
import dev.alteration.branch.gooey.styling.{bold, Alignment, Colors, Fonts}

/** Pre-built form components for user input. */
object Forms {

  /** Creates a form field with a label and input component.
    *
    * @param label
    *   The field label text
    * @param input
    *   The input component (usually a TextField)
    * @return
    *   A labeled form field
    */
  def formField(label: String, input: Component): Component =
    VStack(
      spacing = 8,
      alignment = Alignment.leading,
      Text(label)
        .font(Fonts.caption.bold)
        .color(Colors.gray(0.5f)),
      input
    )

  /** Creates a styled text field suitable for forms.
    *
    * @param placeholder
    *   Placeholder text
    * @param value
    *   Initial value (default: "")
    * @param onChange
    *   Optional change handler
    * @return
    *   A styled text field
    */
  def textField(
      placeholder: String = "",
      value: String = "",
      onChange: Option[String => Unit] = None
  ): Component =
    TextField(placeholder, value, onChange)
      .padding(horizontal = 12, vertical = 8)
      .background(Colors.white)
      .border(Colors.gray(0.7f), width = 1)
      .cornerRadius(4)

  /** Creates a password field suitable for forms.
    *
    * @param placeholder
    *   Placeholder text
    * @param value
    *   Initial value (default: "")
    * @param onChange
    *   Optional change handler
    * @return
    *   A styled password field
    */
  def passwordField(
      placeholder: String = "",
      value: String = "",
      onChange: Option[String => Unit] = None
  ): Component =
    TextField(placeholder, value, onChange, isPassword = true)
      .padding(horizontal = 12, vertical = 8)
      .background(Colors.white)
      .border(Colors.gray(0.7f), width = 1)
      .cornerRadius(4)

  /** Creates a complete form with title, fields, and action buttons.
    *
    * @param title
    *   The form title
    * @param fields
    *   Sequence of form fields
    * @param submitLabel
    *   Label for the submit button (default: "Submit")
    * @param onSubmit
    *   Action to perform on submit
    * @return
    *   A complete form component wrapped in a card
    */
  def form(
      title: String,
      fields: Seq[Component],
      submitLabel: String = "Submit"
  )(onSubmit: => Unit): Component =
    Cards.card(
      VStack(
        spacing = 16,
        alignment = Alignment.leading,
        (Seq(
          Text(title).font(Fonts.title)
        ) ++ fields ++ Seq(
          HStack(
            spacing = 8,
            alignment = Alignment.center,
            Buttons.button("Cancel", ButtonStyle.Secondary) { /* close */ },
            Buttons.button(submitLabel, ButtonStyle.Primary) { onSubmit }
          )
        ))*
      )
    )

}

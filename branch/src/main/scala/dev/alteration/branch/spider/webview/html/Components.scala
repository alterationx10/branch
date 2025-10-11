package dev.alteration.branch.spider.webview.html

import Html._

/** Reusable UI component helpers for common patterns.
  *
  * This module provides higher-level abstractions for frequently-used UI
  * patterns like forms, inputs, buttons, and lists.
  *
  * == Philosophy ==
  *
  *   - '''Accessible by default''' - Proper ARIA labels and roles
  *   - '''Mobile-friendly''' - Sensible defaults for touch interfaces
  *   - '''Composable''' - Can be combined and customized
  *   - '''Not magic''' - Just functions returning Html
  *
  * == Usage ==
  *
  * {{{
  * import dev.alteration.branch.spider.webview.html._
  * import dev.alteration.branch.spider.webview.html.Components._
  *
  * // Simple form
  * div()(
  *   textInput("username", state.username, "update-username",
  *     placeholder = Some("Enter your username")),
  *   emailInput("email", state.email, "update-email"),
  *   submitButton("Save")
  * )
  * }}}
  */
object Components {

  // === Form Input Helpers ===

  /** Create a text input field with WebView integration.
    *
    * @param name
    *   The input name
    * @param currentValue
    *   The current value
    * @param changeEvent
    *   The WebView event to fire on change
    * @param placeholder
    *   Optional placeholder text
    * @param extraAttrs
    *   Additional attributes to add
    */
  def textInput(
      name: String,
      currentValue: String,
      changeEvent: String,
      placeholder: Option[String] = None,
      extraAttrs: Attr*
  ): Html = {
    val baseAttrs = List(
      Attributes.tpe := "text",
      Attributes.name := name,
      Attributes.id := name,
      Attributes.value := currentValue,
      WebViewAttributes.wvChange := changeEvent
    )

    val placeholderAttr = placeholder.map(p => Attributes.placeholder := p).toList
    val allAttrs = baseAttrs ++ placeholderAttr ++ extraAttrs

    Element("input", allAttrs, Nil)
  }

  /** Create an email input field.
    *
    * @param name
    *   The input name
    * @param currentValue
    *   The current value
    * @param changeEvent
    *   The WebView event to fire on change
    * @param placeholder
    *   Optional placeholder text
    * @param extraAttrs
    *   Additional attributes to add
    */
  def emailInput(
      name: String,
      currentValue: String,
      changeEvent: String,
      placeholder: Option[String] = None,
      extraAttrs: Attr*
  ): Html = {
    val baseAttrs = List(
      Attributes.tpe := "email",
      Attributes.name := name,
      Attributes.id := name,
      Attributes.value := currentValue,
      WebViewAttributes.wvChange := changeEvent
    )

    val placeholderAttr = placeholder.map(p => Attributes.placeholder := p).toList
    val allAttrs = baseAttrs ++ placeholderAttr ++ extraAttrs

    Element("input", allAttrs, Nil)
  }

  /** Create a password input field.
    *
    * @param name
    *   The input name
    * @param currentValue
    *   The current value
    * @param changeEvent
    *   The WebView event to fire on change
    * @param placeholder
    *   Optional placeholder text
    * @param extraAttrs
    *   Additional attributes to add
    */
  def passwordInput(
      name: String,
      currentValue: String,
      changeEvent: String,
      placeholder: Option[String] = None,
      extraAttrs: Attr*
  ): Html = {
    val baseAttrs = List(
      Attributes.tpe := "password",
      Attributes.name := name,
      Attributes.id := name,
      Attributes.value := currentValue,
      WebViewAttributes.wvChange := changeEvent
    )

    val placeholderAttr = placeholder.map(p => Attributes.placeholder := p).toList
    val allAttrs = baseAttrs ++ placeholderAttr ++ extraAttrs

    Element("input", allAttrs, Nil)
  }

  /** Create a number input field.
    *
    * @param name
    *   The input name
    * @param currentValue
    *   The current value
    * @param changeEvent
    *   The WebView event to fire on change
    * @param min
    *   Optional minimum value
    * @param max
    *   Optional maximum value
    * @param step
    *   Optional step value
    * @param extraAttrs
    *   Additional attributes to add
    */
  def numberInput(
      name: String,
      currentValue: String,
      changeEvent: String,
      min: Option[Int] = None,
      max: Option[Int] = None,
      step: Option[Int] = None,
      extraAttrs: Attr*
  ): Html = {
    val baseAttrs = List(
      Attributes.tpe := "number",
      Attributes.name := name,
      Attributes.id := name,
      Attributes.value := currentValue,
      WebViewAttributes.wvChange := changeEvent
    )

    val rangeAttrs = List(
      min.map(m => Attributes.attr("min") := m.toString),
      max.map(m => Attributes.attr("max") := m.toString),
      step.map(s => Attributes.attr("step") := s.toString)
    ).flatten

    val allAttrs = baseAttrs ++ rangeAttrs ++ extraAttrs

    Element("input", allAttrs, Nil)
  }

  /** Create a textarea field.
    *
    * @param name
    *   The textarea name
    * @param currentValue
    *   The current value
    * @param changeEvent
    *   The WebView event to fire on change
    * @param rows
    *   Number of visible rows
    * @param placeholder
    *   Optional placeholder text
    * @param extraAttrs
    *   Additional attributes to add
    */
  def textArea(
      name: String,
      currentValue: String,
      changeEvent: String,
      rows: Int = 3,
      placeholder: Option[String] = None,
      extraAttrs: Attr*
  ): Html = {
    val baseAttrs = List(
      Attributes.name := name,
      Attributes.id := name,
      Attributes.attr("rows") := rows.toString,
      WebViewAttributes.wvChange := changeEvent
    )

    val placeholderAttr = placeholder.map(p => Attributes.placeholder := p).toList
    val allAttrs = baseAttrs ++ placeholderAttr ++ extraAttrs

    Element("textarea", allAttrs, List(Text(currentValue)))
  }

  /** Create a checkbox input.
    *
    * @param name
    *   The checkbox name
    * @param isChecked
    *   Whether the checkbox is checked
    * @param clickEvent
    *   The WebView event to fire on click
    * @param labelText
    *   Optional label text
    * @param extraAttrs
    *   Additional attributes to add
    */
  def checkbox(
      name: String,
      isChecked: Boolean,
      clickEvent: String,
      labelText: Option[String] = None,
      extraAttrs: Attr*
  ): Html = {
    val checkboxAttrs = List(
      Attributes.tpe := "checkbox",
      Attributes.name := name,
      Attributes.id := name,
      Attributes.checked := isChecked,
      WebViewAttributes.wvClick := clickEvent
    ) ++ extraAttrs

    val checkboxElem = Element("input", checkboxAttrs, Nil)

    labelText match {
      case Some(label) =>
        Element(
          "label",
          List(Attributes.attr("for") := name),
          List(checkboxElem, Text(" "), Text(label))
        )
      case None =>
        checkboxElem
    }
  }

  /** Create a radio button.
    *
    * @param name
    *   The radio button group name
    * @param value
    *   The value of this radio button
    * @param isSelected
    *   Whether this radio button is selected
    * @param clickEvent
    *   The WebView event to fire on click
    * @param labelText
    *   Optional label text
    * @param extraAttrs
    *   Additional attributes to add
    */
  def radio(
      name: String,
      value: String,
      isSelected: Boolean,
      clickEvent: String,
      labelText: Option[String] = None,
      extraAttrs: Attr*
  ): Html = {
    val radioId = s"$name-$value"
    val radioAttrs = List(
      Attributes.tpe := "radio",
      Attributes.name := name,
      Attributes.id := radioId,
      Attributes.value := value,
      Attributes.checked := isSelected,
      WebViewAttributes.wvClick := clickEvent
    ) ++ extraAttrs

    val radioElem = Element("input", radioAttrs, Nil)

    labelText match {
      case Some(label) =>
        Element(
          "label",
          List(Attributes.attr("for") := radioId),
          List(radioElem, Text(" "), Text(label))
        )
      case None =>
        radioElem
    }
  }

  /** Create a select dropdown.
    *
    * @param name
    *   The select name
    * @param options
    *   List of (value, label) tuples
    * @param selectedValue
    *   The currently selected value
    * @param changeEvent
    *   The WebView event to fire on change
    * @param extraAttrs
    *   Additional attributes to add
    */
  def selectDropdown(
      name: String,
      options: List[(String, String)],
      selectedValue: String,
      changeEvent: String,
      extraAttrs: Attr*
  ): Html = {
    val selectAttrs = List(
      Attributes.name := name,
      Attributes.id := name,
      WebViewAttributes.wvChange := changeEvent
    ) ++ extraAttrs

    val optionElements = options.map { case (value, label) =>
      val isSelected = value == selectedValue
      val optionAttrs = List(
        Attributes.value := value,
        Attributes.selected := isSelected
      )
      Element("option", optionAttrs, List(Text(label)))
    }

    Element("select", selectAttrs, optionElements)
  }

  // === Button Helpers ===

  /** Create a submit button.
    *
    * @param label
    *   The button label
    * @param extraAttrs
    *   Additional attributes to add
    */
  def submitButton(label: String, extraAttrs: Attr*): Html = {
    val attrs = List(
      Attributes.tpe := "submit"
    ) ++ extraAttrs

    Element("button", attrs, List(Text(label)))
  }

  /** Create a button with a click event.
    *
    * @param label
    *   The button label
    * @param clickEvent
    *   The WebView event to fire on click
    * @param extraAttrs
    *   Additional attributes to add
    */
  def clickButton(label: String, clickEvent: String, extraAttrs: Attr*): Html = {
    val attrs = List(
      Attributes.tpe := "button",
      WebViewAttributes.wvClick := clickEvent
    ) ++ extraAttrs

    Element("button", attrs, List(Text(label)))
  }

  /** Create a button with a click event and a target value.
    *
    * Useful for buttons in lists where you need to know which item was clicked.
    *
    * @param label
    *   The button label
    * @param clickEvent
    *   The WebView event to fire on click
    * @param targetValue
    *   The target value to send with the event
    * @param extraAttrs
    *   Additional attributes to add
    */
  def targetButton(
      label: String,
      clickEvent: String,
      targetValue: String,
      extraAttrs: Attr*
  ): Html = {
    val attrs = List(
      Attributes.tpe := "button",
      WebViewAttributes.wvClick := clickEvent,
      WebViewAttributes.wvTarget := targetValue
    ) ++ extraAttrs

    Element("button", attrs, List(Text(label)))
  }

  // === List Helpers ===

  /** Render a keyed list of items.
    *
    * The key is used for efficient DOM updates (though not currently
    * implemented in the WebView morphing logic).
    *
    * @param items
    *   The list of items to render
    * @param renderItem
    *   Function to render each item (receives item and index)
    * @param containerTag
    *   The container tag (default: "div")
    * @param containerAttrs
    *   Attributes for the container
    */
  def keyedList[A](
      items: List[A],
      renderItem: (A, Int) => Html,
      containerTag: String = "div",
      containerAttrs: Attr*
  ): Html = {
    val children = items.zipWithIndex.map { case (item, index) =>
      renderItem(item, index)
    }

    Element(containerTag, containerAttrs.toList, children)
  }

  /** Render an unordered list (ul) with items.
    *
    * @param items
    *   The list of items to render
    * @param renderItem
    *   Function to render each item as an li
    * @param extraAttrs
    *   Additional attributes for the ul
    */
  def unorderedList[A](
      items: List[A],
      renderItem: A => Html,
      extraAttrs: Attr*
  ): Html = {
    val liElements = items.map { item =>
      Element("li", Nil, List(renderItem(item)))
    }

    Element("ul", extraAttrs.toList, liElements)
  }

  /** Render an ordered list (ol) with items.
    *
    * @param items
    *   The list of items to render
    * @param renderItem
    *   Function to render each item as an li
    * @param extraAttrs
    *   Additional attributes for the ol
    */
  def orderedList[A](
      items: List[A],
      renderItem: A => Html,
      extraAttrs: Attr*
  ): Html = {
    val liElements = items.map { item =>
      Element("li", Nil, List(renderItem(item)))
    }

    Element("ol", extraAttrs.toList, liElements)
  }

  // === Layout Helpers ===

  /** Create a container div with common styling patterns.
    *
    * @param children
    *   The child elements
    * @param maxWidth
    *   Optional max-width (e.g., "600px")
    * @param padding
    *   Optional padding (e.g., "20px")
    * @param extraAttrs
    *   Additional attributes
    */
  def container(
      children: Html*
  )(maxWidth: Option[String] = None, padding: Option[String] = None, extraAttrs: Attr*): Html = {
    val styleProps = List(
      maxWidth.map("max-width" -> _),
      padding.map("padding" -> _),
      Some("margin" -> "0 auto")
    ).flatten

    val attrs = if (styleProps.nonEmpty) {
      List(Attributes.styles(styleProps*)) ++ extraAttrs
    } else {
      extraAttrs.toList
    }

    Element("div", attrs, children.toList)
  }

  /** Create a flexbox container.
    *
    * @param children
    *   The child elements
    * @param direction
    *   Flex direction (row, column, etc.)
    * @param gap
    *   Gap between items (e.g., "10px")
    * @param justifyContent
    *   Justify content value
    * @param alignItems
    *   Align items value
    * @param extraAttrs
    *   Additional attributes
    */
  def flexContainer(
      children: Html*
  )(
      direction: String = "row",
      gap: Option[String] = None,
      justifyContent: Option[String] = None,
      alignItems: Option[String] = None,
      extraAttrs: Attr*
  ): Html = {
    val styleProps = List(
      Some("display" -> "flex"),
      Some("flex-direction" -> direction),
      gap.map("gap" -> _),
      justifyContent.map("justify-content" -> _),
      alignItems.map("align-items" -> _)
    ).flatten

    val attrs = List(Attributes.styles(styleProps*)) ++ extraAttrs

    Element("div", attrs, children.toList)
  }

  // Note: when() and cond() are available from Tags, no need to duplicate here
}

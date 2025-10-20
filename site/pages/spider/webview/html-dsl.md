---
title: WebView HTML DSL
description: Type-safe HTML construction with tags, attributes, and components
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - webview
  - html
  - dsl
---

# HTML DSL

WebView includes a comprehensive type-safe HTML DSL inspired by Scalatags.

## Basic HTML Construction

```scala
import dev.alteration.branch.spider.webview.html._
import dev.alteration.branch.spider.webview.html.Tags._
import dev.alteration.branch.spider.webview.html.Attributes._

override def render(state: CounterState): String = {
  div(cls := "container")(
    h1("Counter"),
    div(cls := "count-display")(
      text(state.count)
    ),
    div(cls := "buttons")(
      button(wvClick := Increment, cls := "btn")("Increment"),
      button(wvClick := Decrement, cls := "btn")("Decrement")
    )
  ).render
}
```

## Available Tags

The DSL provides all standard HTML5 tags:

**Container elements**
```
div, span, section, article, header, footer, main, aside, nav
```

**Text elements**
```
h1, h2, h3, h4, h5, h6, p, strong, em, code, pre, br, hr
```

**Lists**
```
ul, ol, li, dl, dt, dd
```

**Forms**
```
form, input, textarea, button, label, select, option, fieldset, legend
```

**Tables**
```
table, thead, tbody, tfoot, tr, th, td
```

**Links & Media**
```
a, img, video, audio, source
```

**Semantic**
```
figure, figcaption, time, mark, blockquote
```

## Attributes

Standard HTML attributes with type-safe builders:

```scala
// Common attributes
cls := "my-class" // class attribute
id := "my-id"
style := "color: red"
title := "Tooltip text"

// Form attributes
name := "username"
value := state.username
placeholder := "Enter name"
tpe := "text" // type attribute

// Link attributes
href := "/path"
target := "_blank"
rel := "noopener"

// Boolean attributes
disabled := true
checked := state.isChecked
readonly := false
required := true

// ARIA attributes
ariaLabel := "Close button"
ariaHidden := false
role := "button"

// Data attributes
data("user-id") := "123" // renders: data-user-id="123"

// Custom attributes
attr("my-custom") := "value"
```

## Conditional Rendering

```scala
// Conditional HTML
when(state.showMessage)(
  div(cls := "alert")("Hello!")
)

// If-else conditional
cond(state.isLoggedIn)(
  div("Welcome, user!")
)(
  div("Please log in")
)

// Conditional attributes
div(
  attrWhen(state.isActive, cls := "active"),
  classWhen(
    "loading" -> state.isLoading,
    "error" -> state.hasError
  )
)

// Conditional styles
div(
  styleWhen(
    ("color", "red", state.isError),
    ("display", "none", state.isHidden)
  )
)
```

## WebView Attributes

WebView attributes (`wv-*`) enable reactive event handling:

### Event Attributes

```scala
// Click events
button(wvClick := "submit")("Submit")
button(wvClick := SaveForm)("Save") // Typed event

// Form events
input(wvChange := "update", value := state.text)
input(wvInput := "search") // Fires on every keystroke
form(wvSubmit := "save-form")

// Focus events
input(wvFocus := "field-focused", wvBlur := "field-blurred")

// Keyboard events
input(wvKeydown := "handle-key", wvKeyup := "key-released")

// Mouse events
div(wvMouseenter := "show-tooltip", wvMouseleave := "hide-tooltip")
```

### Event Modifiers

```scala
// Debounce - wait for user to stop typing
input(
  wvInput := "search",
  wvDebounce := "300" // Wait 300ms after last keystroke
)

// Throttle - rate-limit events
div(
  wvClick := "track-click",
  wvThrottle := "1000" // Max once per second
)

// Attach values to events
button(
  wvClick := "delete-item",
  wvTarget := item.id // Include item.id in event payload
)

button(
  wvClick := "set-filter",
  wvValue := "active"
)

// Prevent DOM updates for specific elements
input(
  wvChange := "update",
  wvIgnore := true // Preserve focus/scroll position
)
```

### Helper Functions

```scala
// Click with target value
button(wvClickTarget("delete", todo.id) *)("Delete")

// Click with custom value
button(wvClickValue("filter", "active") *)("Active")

// Debounced input
input(wvDebounceInput("search", 300) *)

// Throttled click
div(wvThrottleClick("track", 1000) *)
```

## Component Library

WebView includes built-in components for common UI patterns:

```scala
import dev.alteration.branch.spider.webview.html.Components._

// Form inputs
textInput("username", state.username, "UpdateUsername",
  placeholder = Some("Enter username"))

emailInput("email", state.email, "UpdateEmail")

passwordInput("password", state.password, "UpdatePassword")

numberInput("age", state.age.toString, "UpdateAge",
  min = Some(0), max = Some(120))

textArea("bio", state.bio, "UpdateBio", rows = 5)

// Checkboxes and radio buttons
checkbox("terms", state.acceptedTerms, "ToggleTerms",
  labelText = Some("I accept the terms"))

radio("color", "red", state.color == "red", "SelectColor",
  labelText = Some("Red"))

// Select dropdowns
selectDropdown("country",
  options = List("US" -> "United States", "UK" -> "United Kingdom"),
  selectedValue = state.country,
  changeEvent = "SelectCountry"
)

// Buttons
clickButton("Save", "SaveForm", extraAttrs = Seq(cls := "btn-primary"))

targetButton("Delete", "DeleteItem", state.itemId,
  extraAttrs = Seq(cls := "btn-danger"))

// Lists
keyedList(state.items,
  renderItem = (item, index) => div(cls := "item")(text(item.name)),
  containerAttrs = Seq(cls := "item-list")
)

unorderedList(state.items,
  renderItem = item => text(item.name))

// Layout helpers
container(
  h1("My App"),
  p("Content here")
)(maxWidth = Some("600px"), padding = Some("20px"))

flexContainer(
  div("Left"),
  div("Right")
)(direction = "row", gap = Some("10px"), justifyContent = Some("space-between"))
```

## Example Components

Branch provides example implementations of advanced UI components in the examples directory. These can serve as starting points you can copy and adapt:

**Advanced Components Examples** (`examples/src/main/scala/spider/webview/components/AdvancedComponents.scala`):

- **Data Tables** with sortable columns and custom renderers
- **Modals** with customizable headers, footers, and overlay behavior
- **Dropdowns** with trigger elements and item lists
- **Tabs** with navigation and panel content switching
- **Accordions** with collapsible sections
- **Cards** with variants (default, primary, success, warning, danger)
- **Badges** for status indicators
- **Alerts** with dismissible notifications
- **Progress Bars** with dynamic values and variants
- **Pagination** controls for multi-page navigation

To use these in your application, simply copy the components you need from the examples directory and adapt them to your requirements.

## Complete Example

```scala
import dev.alteration.branch.spider.webview.html._
import dev.alteration.branch.spider.webview.html.Tags._
import dev.alteration.branch.spider.webview.html.Attributes._
import dev.alteration.branch.spider.webview.html.Components._

override def render(state: TodoState): String = {
  div(cls := "container")(
    h1()("Todo List"),

    // Input form
    div(cls := "form")(
      textInput("todo-input", state.inputValue, "UpdateInput",
        placeholder = Some("What needs to be done?")),
      clickButton("Add", "AddTodo")
    ),

    // Todo list
    ul(cls := "todo-list")(
      state.todos.map { todo =>
        li(cls := "todo-item", classWhen("completed" -> todo.completed))(
          checkbox(todo.id, todo.completed, s"ToggleTodo"),
          span()(text(todo.text)),
          targetButton("×", "DeleteTodo", todo.id)
        )
      } *
    ),

    // Footer
    div(cls := "footer")(
      span()(text(s"${state.todos.count(!_.completed)} items left")),
      when(state.todos.exists(_.completed))(
        clickButton("Clear completed", "ClearCompleted")
      )
    )
  ).render
}
```

## Next Steps

- Learn about [Styling](/spider/webview/styling) for CSS-in-Scala
- Explore [Advanced Topics](/spider/webview/advanced) for lifecycle hooks and actor integration
- Return to [WebView Overview](/spider/webview)

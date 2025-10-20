---
title: WebView Styling
description: CSS-in-Scala with StyleSheet and CSS utilities
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - webview
  - css
  - styling
---

# Styling

WebView provides CSS-in-Scala via the `StyleSheet` abstraction with automatic scoping.

## StyleSheet

Define your styles as a Scala object:

```scala
import dev.alteration.branch.spider.webview.styling._

object TodoStyles extends StyleSheet {
  val container = style(
    "max-width" -> "600px",
    "margin" -> "0 auto",
    "padding" -> "20px",
    "font-family" -> "sans-serif"
  )

  val todoItem = style(
    "padding" -> "10px",
    "border-bottom" -> "1px solid #ccc",
    "display" -> "flex",
    "justify-content" -> "space-between"
  )

  val completed = style(
    "text-decoration" -> "line-through",
    "opacity" -> "0.6"
  )
}
```

### Using Styles

Apply styles in your render method:

```scala
override def render(state: TodoState): String = {
  div(cls := TodoStyles.container)(
    h1("Todos"),
    ul()(
      state.todos.map { todo =>
        li(cls := TodoStyles.todoItem + " " + (if (todo.completed) TodoStyles.completed else ""))(
          span()(text(todo.text)),
          button(wvClick := ToggleTodo(todo.id))("Toggle")
        )
      } *
    ),
    // Include styles in output
    raw(TodoStyles.toStyleTag)
  ).render
}
```

## CSS Utilities

WebView provides utility functions and constants for common CSS patterns:

```scala
import dev.alteration.branch.spider.webview.styling.CSSUtils._
```

### Color Palette

```scala
Colors.primary   // "#667eea"
Colors.secondary // "#764ba2"
Colors.success   // "#48bb78"
Colors.warning   // "#ed8936"
Colors.danger    // "#f56565"
Colors.info      // "#4299e1"
Colors.light     // "#f7fafc"
Colors.dark      // "#2d3748"
```

### Spacing

```scala
Spacing.xs  // "4px"
Spacing.sm  // "8px"
Spacing.md  // "16px"
Spacing.lg  // "24px"
Spacing.xl  // "32px"
```

### Border Radius

```scala
Radius.sm   // "4px"
Radius.md   // "8px"
Radius.lg   // "12px"
Radius.full // "9999px"
```

### Shadows

```scala
Shadows.sm // "0 1px 3px rgba(0,0,0,0.12)"
Shadows.md // "0 4px 6px rgba(0,0,0,0.1)"
Shadows.lg // "0 10px 15px rgba(0,0,0,0.1)"
Shadows.xl // "0 20px 25px rgba(0,0,0,0.1)"
```

### Helper Functions

#### Flexbox

```scala
val flexStyle = style(
  flex(
    direction = "row",
    justify = "space-between",
    align = "center",
    gap = "10px"
  ) *
)
```

Parameters:
- `direction`: "row" | "column" | "row-reverse" | "column-reverse"
- `justify`: "flex-start" | "flex-end" | "center" | "space-between" | "space-around" | "space-evenly"
- `align`: "flex-start" | "flex-end" | "center" | "stretch" | "baseline"
- `wrap`: "nowrap" | "wrap" | "wrap-reverse"
- `gap`: String (e.g., "10px")

#### Grid

```scala
val gridStyle = style(
  grid(
    columns = "repeat(3, 1fr)",
    rows = "auto",
    gap = "20px"
  ) *
)
```

Parameters:
- `columns`: String (e.g., "repeat(3, 1fr)", "200px 1fr 200px")
- `rows`: String (e.g., "auto", "100px 200px")
- `gap`: String (e.g., "20px")
- `columnGap`: String
- `rowGap`: String

#### Positioning

```scala
val absoluteStyle = style(
  absolute(
    top = Some("10px"),
    right = Some("10px"),
    zIndex = Some("100")
  ) *
)

val relativeStyle = style(
  relative(
    top = Some("5px"),
    left = Some("5px")
  ) *
)

val fixedStyle = style(
  fixed(
    bottom = Some("0"),
    right = Some("0")
  ) *
)

val stickyStyle = style(
  sticky(
    top = Some("0")
  ) *
)
```

## Example: Complete Styled Component

```scala
import dev.alteration.branch.spider.webview.styling._
import dev.alteration.branch.spider.webview.styling.CSSUtils._

object CardStyles extends StyleSheet {
  val card = style(
    "background" -> Colors.light,
    "border-radius" -> Radius.lg,
    "box-shadow" -> Shadows.md,
    "padding" -> Spacing.lg,
    "max-width" -> "400px"
  )

  val header = style(
    flex(
      direction = "row",
      justify = "space-between",
      align = "center"
    ) *,
    "margin-bottom" -> Spacing.md,
    "padding-bottom" -> Spacing.sm,
    "border-bottom" -> s"1px solid ${Colors.dark}"
  )

  val title = style(
    "font-size" -> "1.5rem",
    "font-weight" -> "bold",
    "color" -> Colors.dark
  )

  val button = style(
    "background" -> Colors.primary,
    "color" -> "white",
    "border" -> "none",
    "border-radius" -> Radius.md,
    "padding" -> s"${Spacing.sm} ${Spacing.md}",
    "cursor" -> "pointer",
    "transition" -> "all 0.2s",
    ":hover" -> s"background: ${Colors.secondary}"
  )

  val grid = style(
    grid(
      columns = "repeat(2, 1fr)",
      gap = Spacing.md
    ) *
  )
}

// Use in render
override def render(state: State): String = {
  div(cls := CardStyles.card)(
    div(cls := CardStyles.header)(
      h2(cls := CardStyles.title)("My Card"),
      button(cls := CardStyles.button, wvClick := CloseCard)("×")
    ),
    div(cls := CardStyles.grid)(
      div()("Item 1"),
      div()("Item 2"),
      div()("Item 3"),
      div()("Item 4")
    ),
    raw(CardStyles.toStyleTag)
  ).render
}
```

## Responsive Design

You can include media queries in your styles:

```scala
object ResponsiveStyles extends StyleSheet {
  val container = style(
    "max-width" -> "1200px",
    "margin" -> "0 auto",
    "padding" -> Spacing.lg,
    "@media (max-width: 768px)" -> """
      padding: 8px;
      max-width: 100%;
    """
  )

  val grid = style(
    grid(
      columns = "repeat(3, 1fr)",
      gap = Spacing.md
    ) *,
    "@media (max-width: 768px)" -> """
      grid-template-columns: 1fr;
    """
  )
}
```

## Pseudo-classes and Pseudo-elements

```scala
val button = style(
  "background" -> Colors.primary,
  "color" -> "white",
  "transition" -> "all 0.2s",

  // Pseudo-classes
  ":hover" -> s"background: ${Colors.secondary}",
  ":active" -> "transform: scale(0.98)",
  ":focus" -> s"outline: 2px solid ${Colors.primary}",
  ":disabled" -> "opacity: 0.5; cursor: not-allowed",

  // Pseudo-elements
  "::before" -> """
    content: "→ ";
    margin-right: 4px;
  """,
  "::after" -> """
    content: "";
    display: block;
    height: 2px;
    background: currentColor;
  """
)
```

## Dynamic Styling

You can dynamically apply styles based on state:

```scala
override def render(state: State): String = {
  val buttonClass = if (state.isActive) {
    s"${Styles.button} ${Styles.active}"
  } else {
    Styles.button
  }

  div()(
    button(cls := buttonClass, wvClick := Toggle)("Toggle"),
    raw(Styles.toStyleTag)
  ).render
}
```

## Next Steps

- Learn about the [HTML DSL](html-dsl.md) for building UIs
- Explore [Advanced Topics](advanced.md) for lifecycle hooks and actor integration
- Return to [WebView Overview](index.md)

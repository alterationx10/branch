# Gooey DSL Examples

This directory contains examples demonstrating the **Gooey** Swing DSL for Scala 3 - a modern, declarative approach to building Swing UIs.

## What is Gooey?

Gooey is a zero-dependency Swing DSL that reduces boilerplate by 80-90% compared to traditional Swing code. It provides:

- **Declarative syntax** inspired by SwiftUI, Flutter, and Jetpack Compose
- **Type-safe API** with full Scala 3 support
- **Modifier system** for composable styling
- **Pre-built components** (cards, forms, buttons, etc.)
- **Layout templates** for common patterns

## Running Examples

To run any example:

```bash
sbt "examples/runMain gooey.ExampleName"
```

For example:
```bash
sbt "examples/runMain gooey.HelloWorldExample"
```

## Examples Overview

### Basic Examples

#### HelloWorldExample
**File:** `HelloWorldExample.scala`

A simple introduction to Gooey showing:
- Basic VStack layout
- Text components with styling
- Button with click handler
- Modifier usage (padding, colors, fonts)

**Run:** `sbt "examples/runMain gooey.HelloWorldExample"`

---

#### LoginFormExample
**File:** `LoginFormExample.scala`

Demonstrates the power of Gooey's form components:
- Traditional Swing: **83+ lines** of GridBagLayout code
- With Gooey: **~10 lines** of declarative DSL

Shows:
- Form with text fields
- Password field handling
- Submit button with action
- Input change handlers

**Run:** `sbt "examples/runMain gooey.LoginFormExample"`

---

### Component Showcases

#### ButtonShowcaseExample
**File:** `ButtonShowcaseExample.scala`

Demonstrates all button styles:
- Primary, Secondary, Tertiary
- Success and Danger variants
- Card-based organization
- Console output for interactions

**Run:** `sbt "examples/runMain gooey.ButtonShowcaseExample"`

---

#### CardGalleryExample
**File:** `CardGalleryExample.scala`

Shows various card layouts:
- Basic cards
- Elevated cards (with more shadow)
- Cards with headers
- Grid-based card layouts
- Information cards with progress bars

**Run:** `sbt "examples/runMain gooey.CardGalleryExample"`

---

### Application Templates

#### AppLauncherExample
**File:** `AppLauncherExample.scala`

A clean application launcher UI showing:
- Grid layout for app icons
- Emoji icons
- Click handlers for launching
- Card-based icon tiles

**Run:** `sbt "examples/runMain gooey.AppLauncherExample"`

---

#### DashboardExample
**File:** `DashboardExample.scala`

Widget-based dashboard layout:
- System status widgets
- Progress bars for metrics
- Recent activity list
- Quick actions panel
- Multi-column layout

**Run:** `sbt "examples/runMain gooey.DashboardExample"`

---

#### MasterDetailExample
**File:** `MasterDetailExample.scala`

Email client-style master-detail layout:
- List of items (emails)
- Detail view for selected item
- Split-pane style interface
- Template-based layout

**Run:** `sbt "examples/runMain gooey.MasterDetailExample"`

---

#### SettingsPageExample
**File:** `SettingsPageExample.scala`

Structured settings interface:
- Multiple setting sections
- Form fields for inputs
- Grouped by category
- Clean, organized layout

**Run:** `sbt "examples/runMain gooey.SettingsPageExample"`

---

## Code Comparison

### Traditional Swing (GridBagLayout)

```java
JPanel panel = new JPanel(new GridBagLayout());
GridBagConstraints gbc = new GridBagConstraints();
gbc.gridx = 0;
gbc.gridy = 0;
gbc.anchor = GridBagConstraints.WEST;
gbc.insets = new Insets(5, 5, 5, 5);

JLabel titleLabel = new JLabel("Login");
titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
panel.add(titleLabel, gbc);

gbc.gridy++;
JLabel userLabel = new JLabel("Username:");
userLabel.setFont(new Font("Arial", Font.PLAIN, 12));
panel.add(userLabel, gbc);

gbc.gridy++;
JTextField userField = new JTextField(20);
userField.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createLineBorder(Color.GRAY),
    BorderFactory.createEmptyBorder(8, 12, 8, 12)
));
panel.add(userField, gbc);

// ... 60+ more lines for password field and button
```

### With Gooey DSL

```scala
Forms.form(
  title = "Login",
  fields = Seq(
    Forms.formField("Username",
      Forms.textField(placeholder = "Enter username")),
    Forms.formField("Password",
      Forms.passwordField(placeholder = "Enter password"))
  ),
  submitLabel = "Login"
) {
  authenticate(username, password)
}
```

## Key DSL Features

### Layout Containers

- **VStack** - Vertical stack layout
- **HStack** - Horizontal stack layout
- **ZStack** - Layered (z-axis) layout
- **Grid** - Grid layout with columns
- **Spacer** - Flexible/fixed spacing

### Basic Components

- **Text** - Text display with styling
- **Button** - Clickable buttons
- **TextField** - Text input fields
- **Image** - Image display

### Pre-built Component Library

- **Cards** - Card containers with shadows
- **Buttons** - Styled button variants
- **Forms** - Form fields and inputs
- **Navigation** - Navbar and sidebar
- **Lists** - List items and galleries
- **Overlays** - Modals and toasts
- **Progress** - Progress bars and spinners

### Templates

- **threeColumn()** - Sidebar/content/inspector
- **masterDetail()** - List/detail view
- **dashboard()** - Widget grid
- **settingsPage()** - Settings sections

### Modifiers

Modifiers are chainable methods that style components:

```scala
Text("Hello")
  .font(Fonts.largeTitle)
  .color(Colors.blue)
  .padding(20)
  .background(Colors.white)
  .cornerRadius(8)
  .shadow(offsetY = 2, blur = 8)
  .onClick { println("Clicked!") }
```

Available modifiers:
- `.padding()` - Add padding
- `.background()` - Set background color
- `.border()` - Add borders
- `.cornerRadius()` - Round corners
- `.shadow()` - Add drop shadows
- `.onClick()` - Add click handler
- `.onHover()` - Add hover handler
- `.frame()` - Set size constraints

## Building Your Own Examples

### Basic Pattern

```scala
import dev.alteration.branch.gooey.GooeyApp
import dev.alteration.branch.gooey.layout.VStack
import dev.alteration.branch.gooey.components.Text
import dev.alteration.branch.gooey.styling.{Fonts, Alignment}
import dev.alteration.branch.gooey.modifiers.*
import java.awt.Component

object MyExample extends GooeyApp {
  override val appTitle: String = "My Example"

  override def mainContent: Component = {
    VStack(
      spacing = 16,
      alignment = Alignment.center,
      children = Seq(
        Text("My UI").font(Fonts.largeTitle)
      )*
    )
      .padding(20)
      .render()
      .asJComponent
  }
}
```

### Key Points

1. **Extend GooeyApp** - Provides the main entry point
2. **Set appTitle** - Window title
3. **Implement mainContent** - Return a `Component` that will be rendered
4. **Use `.render().asJComponent`** - Convert DSL to Swing component
5. **Spread children with `*`** - When passing Seq to varargs parameters

## Tips and Best Practices

### 1. Use Named Parameters

Named parameters make the DSL more readable:

```scala
// Good
VStack(spacing = 16, alignment = Alignment.center)

// Less clear
VStack(16, Alignment.center)
```

### 2. Organize with Variables

Extract complex components:

```scala
val header = Text("Title").font(Fonts.headline)
val content = VStack(/* ... */)

VStack(children = Seq(header, content)*)
```

### 3. Use Pre-built Components

Leverage the pre-built library instead of building from scratch:

```scala
// Use this
Forms.formField("Email", Forms.textField())

// Instead of manual layout
VStack(
  Text("Email"),
  TextField(/* ... */)
)
```

### 4. Modifier Chains

Chain modifiers for clean styling:

```scala
component
  .padding(16)
  .background(Colors.white)
  .cornerRadius(8)
  .shadow()
```

## Architecture

The DSL has a clean layered architecture:

```
gooey/
├── layout/          # Core layout containers (VStack, HStack, Grid)
├── components/      # Basic components (Text, Button, TextField)
├── modifiers/       # Modifier system (padding, background, etc.)
├── prebuilt/        # Pre-built component library (Cards, Forms, etc.)
├── templates/       # Application templates (dashboard, masterDetail)
└── styling/         # Styling utilities (Colors, Fonts, Alignment)
```

## Next Steps

1. **Explore the examples** - Run each one to see the DSL in action
2. **Read the source code** - Each example is well-documented
3. **Build your own** - Start with HelloWorldExample and modify it
4. **Check the main DSL** - Located in `branch/src/main/scala/dev/alteration/branch/gooey/`

## Performance Notes

- **Zero dependencies** - Only uses `java.awt` and `javax.swing`
- **Direct rendering** - Each component maps directly to a JComponent
- **No virtual DOM** - Simple, predictable rendering model
- **Compile-time safety** - Full type checking at compile time

## Contributing

To add a new example:

1. Create a new file in `examples/src/main/scala/gooey/`
2. Extend `GooeyApp`
3. Implement `appTitle` and `mainContent`
4. Add documentation to this README
5. Compile with `sbt examples/compile`
6. Test with `sbt "examples/runMain gooey.YourExample"`

---

**Happy coding with Gooey!** 🎨

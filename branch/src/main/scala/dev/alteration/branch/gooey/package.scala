package dev.alteration.branch

/** Package object providing convenient imports and utilities for the Gooey DSL.
  */
package object gooey {

  // Re-export commonly used types
  export gooey.layout.{Component, Grid, HStack, Spacer, VStack, ZStack}
  export gooey.components.{Button, Image, Text, TextField}
  export gooey.styling.{Alignment, Colors, Fonts}
  // Export extension methods

  // Helper methods for creating layout containers with varargs
  object Layout {

    def vstack(
        spacing: Int = 0,
        alignment: Alignment = Alignment.Leading
    )(children: Component*): VStack =
      VStack(spacing, alignment, children*)

    def hstack(
        spacing: Int = 0,
        alignment: Alignment = Alignment.Center
    )(children: Component*): HStack =
      HStack(spacing, alignment, children*)

    def zstack(alignment: Alignment = Alignment.Center)(
        children: Component*
    ): ZStack =
      ZStack(alignment, children*)

    def grid(columns: Int, spacing: Int = 8)(
        children: Component*
    ): Grid =
      Grid(columns, spacing, children*)

  }

}

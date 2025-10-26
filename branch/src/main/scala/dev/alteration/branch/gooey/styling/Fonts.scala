package dev.alteration.branch.gooey.styling

import java.awt.Font as AWTFont

/** Font utilities for the Gooey DSL. */
object Fonts {

  /** Font weights. */
  enum FontWeight(val awtStyle: Int) {
    case Regular    extends FontWeight(AWTFont.PLAIN)
    case Bold       extends FontWeight(AWTFont.BOLD)
    case Italic     extends FontWeight(AWTFont.ITALIC)
    case BoldItalic extends FontWeight(AWTFont.BOLD | AWTFont.ITALIC)
  }

  /** Predefined font sizes following common design system conventions. */
  object FontSize {
    val caption: Int    = 11
    val body: Int       = 13
    val headline: Int   = 15
    val title: Int      = 18
    val largeTitle: Int = 22
  }

  /** Creates a font with the specified family, size, and weight. */
  def font(
      family: String = "System",
      size: Int = FontSize.body,
      weight: FontWeight = FontWeight.Regular
  ): AWTFont =
    new AWTFont(family, weight.awtStyle, size)

  // Predefined fonts
  lazy val caption: AWTFont    = font(size = FontSize.caption)
  lazy val body: AWTFont       = font(size = FontSize.body)
  lazy val headline: AWTFont   = font(size = FontSize.headline)
  lazy val title: AWTFont      = font(size = FontSize.title)
  lazy val largeTitle: AWTFont = font(size = FontSize.largeTitle)

  lazy val default: AWTFont = body

}

/** Extension methods for java.awt.Font. */
extension (font: AWTFont) {

  /** Creates a bold version of this font. */
  def bold: AWTFont = font.deriveFont(AWTFont.BOLD)

  /** Creates an italic version of this font. */
  def italic: AWTFont = font.deriveFont(AWTFont.ITALIC)

  /** Creates a version of this font with the specified size. */
  def size(size: Int): AWTFont = font.deriveFont(size.toFloat)

  /** Creates a version of this font with the specified size. */
  def size(size: Float): AWTFont = font.deriveFont(size)

}

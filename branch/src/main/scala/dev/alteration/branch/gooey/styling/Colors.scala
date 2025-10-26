package dev.alteration.branch.gooey.styling

import java.awt.Color as AWTColor

/** Color utilities for the Gooey DSL.
  *
  * Provides convenient color creation and manipulation methods.
  */
object Colors {

  /** Creates a color from RGB components (0-255). */
  def rgb(r: Int, g: Int, b: Int): AWTColor =
    new AWTColor(r, g, b)

  /** Creates a color from RGBA components (0-255, alpha 0-255). */
  def rgba(r: Int, g: Int, b: Int, a: Int): AWTColor =
    new AWTColor(r, g, b, a)

  /** Creates a grayscale color (0.0 = black, 1.0 = white). */
  def gray(value: Float): AWTColor = {
    val v = (value * 255).toInt.max(0).min(255)
    new AWTColor(v, v, v)
  }

  /** Creates a grayscale color with alpha. */
  def gray(value: Float, alpha: Float): AWTColor = {
    val v = (value * 255).toInt.max(0).min(255)
    val a = (alpha * 255).toInt.max(0).min(255)
    new AWTColor(v, v, v, a)
  }

  // Standard colors
  val black: AWTColor       = AWTColor.BLACK
  val white: AWTColor       = AWTColor.WHITE
  val red: AWTColor         = AWTColor.RED
  val green: AWTColor       = AWTColor.GREEN
  val blue: AWTColor        = AWTColor.BLUE
  val yellow: AWTColor      = AWTColor.YELLOW
  val orange: AWTColor      = AWTColor.ORANGE
  val cyan: AWTColor        = AWTColor.CYAN
  val magenta: AWTColor     = AWTColor.MAGENTA
  val transparent: AWTColor = new AWTColor(0, 0, 0, 0)

}

/** Extension methods for java.awt.Color. */
extension (color: AWTColor) {

  /** Creates a new color with the specified alpha value (0.0-1.0). */
  def withAlpha(alpha: Float): AWTColor = {
    val a = (alpha * 255).toInt.max(0).min(255)
    new AWTColor(color.getRed, color.getGreen, color.getBlue, a)
  }

  /** Lightens the color by the specified amount (0.0-1.0). */
  def lighten(amount: Float): AWTColor = {
    val r = (color.getRed + (255 - color.getRed) * amount).toInt.min(255)
    val g = (color.getGreen + (255 - color.getGreen) * amount).toInt.min(255)
    val b = (color.getBlue + (255 - color.getBlue) * amount).toInt.min(255)
    new AWTColor(r, g, b, color.getAlpha)
  }

  /** Darkens the color by the specified amount (0.0-1.0). */
  def darken(amount: Float): AWTColor = {
    val r = (color.getRed * (1 - amount)).toInt.max(0)
    val g = (color.getGreen * (1 - amount)).toInt.max(0)
    val b = (color.getBlue * (1 - amount)).toInt.max(0)
    new AWTColor(r, g, b, color.getAlpha)
  }

}

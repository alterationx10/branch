package dev.alteration.branch.gooey.modifiers

import dev.alteration.branch.gooey.layout.Component

import java.awt.Color

/** Extension methods for applying modifiers to components.
  *
  * These extensions provide a fluent API for styling and configuring
  * components.
  */
extension (c: Component) {

  /** Adds padding to all sides of the component.
    *
    * @param all
    *   The padding amount for all sides in pixels
    */
  def padding(all: Int): Component =
    PaddingModifier(c, all, all, all, all)

  /** Adds horizontal and vertical padding to the component.
    *
    * @param horizontal
    *   The padding for left and right sides in pixels
    * @param vertical
    *   The padding for top and bottom sides in pixels
    */
  def padding(horizontal: Int, vertical: Int): Component =
    PaddingModifier(c, vertical, horizontal, vertical, horizontal)

  /** Adds individual padding to each side of the component.
    *
    * @param top
    *   Top padding in pixels
    * @param right
    *   Right padding in pixels
    * @param bottom
    *   Bottom padding in pixels
    * @param left
    *   Left padding in pixels
    */
  def padding(top: Int, right: Int, bottom: Int, left: Int): Component =
    PaddingModifier(c, top, right, bottom, left)

  /** Sets the background color of the component.
    *
    * @param color
    *   The background color
    */
  def background(color: Color): Component =
    BackgroundModifier(c, color)

  /** Adds a border around the component.
    *
    * @param color
    *   The border color
    * @param width
    *   The border width in pixels (default: 1)
    */
  def border(color: Color, width: Int = 1): Component =
    BorderModifier(c, color, width)

  /** Adds rounded corners to the component.
    *
    * @param radius
    *   The corner radius in pixels
    */
  def cornerRadius(radius: Int): Component =
    CornerRadiusModifier(c, radius)

  /** Adds a drop shadow to the component.
    *
    * @param offsetX
    *   Horizontal shadow offset in pixels (default: 5)
    * @param offsetY
    *   Vertical shadow offset in pixels (default: 5)
    * @param blur
    *   Shadow blur radius in pixels (default: 10)
    * @param opacity
    *   Shadow opacity from 0.0 to 1.0 (default: 0.5)
    */
  def shadow(
      offsetX: Int = 5,
      offsetY: Int = 5,
      blur: Int = 10,
      opacity: Float = 0.5f
  ): Component =
    ShadowModifier(c, offsetX, offsetY, blur, opacity)

  /** Makes the component clickable with the given handler.
    *
    * @param handler
    *   The function to call when the component is clicked
    */
  def onClick(handler: => Unit): Component =
    ClickableModifier(c, () => handler)

  /** Adds hover state tracking to the component.
    *
    * @param handler
    *   The function to call with hover state (true = entered, false = exited)
    */
  def onHover(handler: Boolean => Unit): Component =
    HoverModifier(c, handler)

  /** Sets the fixed width and height of the component.
    *
    * @param width
    *   The fixed width in pixels
    * @param height
    *   The fixed height in pixels
    */
  def frame(width: Int, height: Int): Component =
    FrameModifier(c, Some(width), Some(height))

  /** Sets the fixed width of the component (height remains flexible).
    *
    * @param width
    *   The fixed width in pixels
    */
  def frame(width: Int): Component =
    FrameModifier(c, Some(width), None)

  /** Sets the size of the component (alias for frame).
    *
    * @param width
    *   The width in pixels
    * @param height
    *   The height in pixels
    */
  def size(width: Int, height: Int): Component =
    frame(width, height)

}

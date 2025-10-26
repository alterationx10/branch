package dev.alteration.branch.gooey.prebuilt

import dev.alteration.branch.gooey.layout.{Component, Spacer, VStack, ZStack}
import dev.alteration.branch.gooey.components.Text
import dev.alteration.branch.gooey.modifiers.*
import dev.alteration.branch.gooey.styling.{withAlpha, Alignment, Colors, Fonts}
import dev.alteration.branch.gooey.styling.*

/** Pre-built progress indicator components. */
object Progress {

  /** Creates a horizontal progress bar.
    *
    * @param value
    *   Current progress value
    * @param max
    *   Maximum value (default: 1.0)
    * @param width
    *   Width of the progress bar in pixels (default: 200)
    * @return
    *   A progress bar component
    */
  def progressBar(
      value: Double,
      max: Double = 1.0,
      width: Int = 200
  ): Component =
    ZStack(
      alignment = Alignment.leading,
      // Background
      Spacer()
        .frame(width = width, height = 8)
        .background(Colors.gray(0.9f))
        .cornerRadius(4),
      // Fill
      Spacer()
        .frame(width = ((width * (value / max)).toInt).max(0), height = 8)
        .background(Colors.blue)
        .cornerRadius(4)
    )

  /** Creates a loading spinner (placeholder implementation).
    *
    * Note: True animated spinners require more complex rendering. This provides
    * a placeholder.
    *
    * @param size
    *   Size of the spinner in pixels (default: 40)
    * @return
    *   A spinner component
    */
  def spinner(spinnerSize: Int = 40): Component =
    Text("⏳")
      .font(Fonts.body.size(spinnerSize))
      .frame(spinnerSize, spinnerSize)

  /** Creates a full-screen loading overlay.
    *
    * @param message
    *   Loading message (default: "Loading...")
    * @return
    *   A loading overlay component
    */
  def loadingOverlay(message: String = "Loading..."): Component =
    ZStack(
      alignment = Alignment.center,
      Spacer()
        .frame(width = Int.MaxValue, height = Int.MaxValue)
        .background(Colors.white.withAlpha(0.8f)),
      VStack(
        spacing = 16,
        alignment = Alignment.center,
        spinner(spinnerSize = 60),
        Text(message).font(Fonts.body).color(Colors.gray(0.6f))
      )
    )

}

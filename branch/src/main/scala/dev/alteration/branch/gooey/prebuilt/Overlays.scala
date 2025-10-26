package dev.alteration.branch.gooey.prebuilt

import dev.alteration.branch.gooey.layout.{
  Component,
  HStack,
  Spacer,
  VStack,
  ZStack
}
import dev.alteration.branch.gooey.components.Text
import dev.alteration.branch.gooey.modifiers.*
import dev.alteration.branch.gooey.styling.{withAlpha, Alignment, Colors, Fonts}

/** Toast notification types. */
enum ToastType {
  case Info
  case Success
  case Warning
  case Error
}

/** Pre-built overlay and modal components. */
object Overlays {

  /** Creates a modal dialog with backdrop.
    *
    * @param title
    *   Modal title
    * @param content
    *   Modal content
    * @param onClose
    *   Action to perform when closed
    * @return
    *   A modal component with backdrop
    */
  def modal(title: String, content: Component, onClose: => Unit): Component =
    ZStack(
      alignment = Alignment.center,
      // Semi-transparent backdrop
      Spacer()
        .frame(width = Int.MaxValue, height = Int.MaxValue)
        .background(Colors.black.withAlpha(0.5f))
        .onClick(onClose),
      // Modal card
      VStack(
        spacing = 16,
        alignment = Alignment.leading,
        HStack(
          spacing = 16,
          alignment = Alignment.center,
          Text(title).font(Fonts.headline),
          Spacer()
          // Close button would go here
        ),
        content
      )
        .padding(24)
        .background(Colors.white)
        .cornerRadius(12)
        .shadow(offsetY = 20, blur = 40, opacity = 0.3f)
        .frame(width = 400)
    )

  /** Creates a toast notification.
    *
    * @param message
    *   The notification message
    * @param toastType
    *   The type of notification (default: Info)
    * @return
    *   A toast notification component
    */
  def toast(
      message: String,
      toastType: ToastType = ToastType.Info
  ): Component = {
    val bgColor = toastType match {
      case ToastType.Info    => Colors.blue
      case ToastType.Success => Colors.green
      case ToastType.Warning => Colors.orange
      case ToastType.Error   => Colors.red
    }

    Text(message)
      .color(Colors.white)
      .padding(horizontal = 16, vertical = 12)
      .background(bgColor)
      .cornerRadius(8)
      .shadow(offsetY = 2, blur = 8, opacity = 0.15f)
  }

}

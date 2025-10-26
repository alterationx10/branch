package dev.alteration.branch.gooey.styling

import javax.swing.SwingConstants

/** Alignment options for components within containers. */
enum Alignment {

  // Horizontal alignments
  case Leading
  case Center
  case Trailing

  // Vertical alignments
  case Top
  case Middle
  case Bottom

  // Combined alignments (2D)
  case TopLeading
  case TopCenter
  case TopTrailing
  case CenterLeading
  case CenterCenter
  case CenterTrailing
  case BottomLeading
  case BottomCenter
  case BottomTrailing

  /** Converts alignment to Swing horizontal alignment constant. */
  def toSwingHorizontal: Int = this match {
    case Leading | TopLeading | CenterLeading | BottomLeading     =>
      SwingConstants.LEFT
    case Center | TopCenter | CenterCenter | BottomCenter         =>
      SwingConstants.CENTER
    case Trailing | TopTrailing | CenterTrailing | BottomTrailing =>
      SwingConstants.RIGHT
    case Top | Middle | Bottom                                    => SwingConstants.CENTER
  }

  /** Converts alignment to Swing vertical alignment constant. */
  def toSwingVertical: Int = this match {
    case Top | TopLeading | TopCenter | TopTrailing             => SwingConstants.TOP
    case Middle | CenterLeading | CenterCenter | CenterTrailing =>
      SwingConstants.CENTER
    case Bottom | BottomLeading | BottomCenter | BottomTrailing =>
      SwingConstants.BOTTOM
    case Leading | Center | Trailing                            => SwingConstants.CENTER
  }

  /** Gets the X alignment for BoxLayout (0.0 = left, 0.5 = center, 1.0 =
    * right).
    */
  def toAlignmentX: Float = this match {
    case Leading | TopLeading | CenterLeading | BottomLeading     => 0.0f
    case Center | TopCenter | CenterCenter | BottomCenter         => 0.5f
    case Trailing | TopTrailing | CenterTrailing | BottomTrailing => 1.0f
    case Top | Middle | Bottom                                    => 0.5f
  }

  /** Gets the Y alignment for BoxLayout (0.0 = top, 0.5 = middle, 1.0 =
    * bottom).
    */
  def toAlignmentY: Float = this match {
    case Top | TopLeading | TopCenter | TopTrailing             => 0.0f
    case Middle | CenterLeading | CenterCenter | CenterTrailing => 0.5f
    case Bottom | BottomLeading | BottomCenter | BottomTrailing => 1.0f
    case Leading | Center | Trailing                            => 0.5f
  }

}

object Alignment {
  // Convenient aliases matching SwiftUI naming
  val leading: Alignment  = Leading
  val center: Alignment   = Center
  val trailing: Alignment = Trailing
  val top: Alignment      = Top
  val middle: Alignment   = Middle
  val bottom: Alignment   = Bottom
}

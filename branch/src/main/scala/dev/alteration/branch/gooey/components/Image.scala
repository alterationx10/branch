package dev.alteration.branch.gooey.components

import dev.alteration.branch.gooey.layout.{Component, RenderedComponent}

import javax.swing.{ImageIcon, JLabel}
import java.awt.Image as AWTImage
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** Image display component.
  *
  * Displays an image from a file path or BufferedImage.
  *
  * @param source
  *   Either a String file path or a BufferedImage
  * @param width
  *   Optional width to scale to
  * @param height
  *   Optional height to scale to
  */
case class Image(
    source: Either[String, BufferedImage],
    width: Option[Int] = None,
    height: Option[Int] = None
) extends Component {

  override def render(): RenderedComponent = {
    val image: BufferedImage = source match {
      case Left(path)    =>
        try {
          ImageIO.read(new File(path))
        } catch {
          case _: Exception =>
            // Return a placeholder or empty image on error
            new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        }
      case Right(buffer) => buffer
    }

    val scaledImage = (width, height) match {
      case (Some(w), Some(h)) =>
        image.getScaledInstance(w, h, AWTImage.SCALE_SMOOTH)
      case (Some(w), None)    =>
        val aspectRatio = image.getHeight.toDouble / image.getWidth.toDouble
        val h           = (w * aspectRatio).toInt
        image.getScaledInstance(w, h, AWTImage.SCALE_SMOOTH)
      case (None, Some(h))    =>
        val aspectRatio = image.getWidth.toDouble / image.getHeight.toDouble
        val w           = (h * aspectRatio).toInt
        image.getScaledInstance(w, h, AWTImage.SCALE_SMOOTH)
      case (None, None)       => image
    }

    val icon  = new ImageIcon(scaledImage)
    val label = new JLabel(icon)
    label.setOpaque(false)

    RenderedComponent(label)
  }

  /** Creates a new Image with the specified size.
    *
    * @param w
    *   Width in pixels
    * @param h
    *   Height in pixels
    */
  def size(w: Int, h: Int): Image = copy(width = Some(w), height = Some(h))

}

object Image {

  /** Creates an Image from a file path.
    *
    * @param path
    *   Path to the image file
    */
  def apply(path: String): Image = Image(Left(path))

  /** Creates an Image from a BufferedImage.
    *
    * @param buffer
    *   The BufferedImage
    */
  def apply(buffer: BufferedImage): Image = Image(Right(buffer))

}

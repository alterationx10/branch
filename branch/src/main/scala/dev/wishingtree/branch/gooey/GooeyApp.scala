package dev.wishingtree.branch.gooey

import dev.wishingtree.branch.gooey.SlidingDirection.{Horizontal, Vertical}

import javax.swing.*
import java.awt.*
import scala.util.Try

trait GooeyApp {

  def setSystemProperties(): Unit = {

    // Mac
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    System.setProperty(
      "com.apple.mrj.application.apple.menu.about.name",
      "Sliding Panel Demo"
    )

  }

  def setSystemLookAndFeel(): Unit = {

    Try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)
    }
    JFrame.setDefaultLookAndFeelDecorated(true);

  }

  private var _args: Array[String] = Array.empty
  lazy val args: Seq[String]       = _args.toSeq

  def configureRootFrame(rootFrame: JFrame): JFrame = rootFrame

  val appTitle: String

  // Main content panel with BorderLayout
  def mainContent: Component

  lazy val rootFrame: JFrame = configureRootFrame {
    val frame = new JFrame(appTitle)
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    frame.setSize(800, 600)
    frame.setLayout(new BorderLayout())
    // Center the frame on screen
    frame.setLocationRelativeTo(null)
    frame
  }

  final def main(args: Array[String]): Unit = {
    _args = args

    setSystemProperties()
    setSystemLookAndFeel()

    rootFrame.add(mainContent, BorderLayout.CENTER)
    rootFrame.setVisible(true)
  }

}

object GooeyDemo extends GooeyApp {

  override val appTitle: String = "Gooey Demo"

  val mainPanel = new JPanel(new BorderLayout())
  val centerText = new TextArea("Center Text")
  mainPanel.add(centerText, BorderLayout.CENTER)
  val leftPanel = new SlidingPanel(Horizontal, 200)
  leftPanel.setLayout(new BorderLayout())
  leftPanel.add(new TextField("Left Text"), BorderLayout.CENTER)
  mainPanel.add(leftPanel, BorderLayout.WEST)
  val toggleButton = new JButton("Toggle")
  toggleButton.addActionListener(e => leftPanel.toggle())
  mainPanel.add(toggleButton, BorderLayout.SOUTH)


  override def mainContent: Component = mainPanel
}

package dev.wishingtree.branch.gooey

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

  def configureRootFrame(rootFrame: JFrame): JFrame    = rootFrame
  def configureMainContentPanel(panel: JPanel): JPanel = panel

  val appTitle: String

  // Main content panel with BorderLayout
  def mainContent: Component

  // Track the current center content
  private var currentCenterContent: Component = scala.compiletime.uninitialized

  // Method to swap out the center content
  def setMainContent(content: Component): Unit = {
    if (currentCenterContent != null) {
      mainContentPanel.remove(currentCenterContent)
    }
    currentCenterContent = content
    mainContentPanel.add(content, BorderLayout.CENTER)
    mainContentPanel.revalidate()
    mainContentPanel.repaint()
  }

  protected lazy val mainContentPanel = configureMainContentPanel {
    val panel = new JPanel(new BorderLayout())
    // Add center content and track it
    currentCenterContent = mainContent
    panel.add(currentCenterContent, BorderLayout.CENTER)
    // Add sliding panels
    panel.add(westPanel, BorderLayout.WEST)
    panel.add(eastPanel, BorderLayout.EAST)
    panel.add(northPanel, BorderLayout.NORTH)
    panel.add(southPanel, BorderLayout.SOUTH)
    panel
  }
  
  def westContent: Component = new JPanel()
  val westPanelSize: Int     = 150
  final lazy val westPanel   =
    new SlidingPanel(BorderLayout.WEST, westPanelSize)

  def eastContent: Component = new JPanel()
  val eastPanelSize: Int     = 150
  final lazy val eastPanel   =
    new SlidingPanel(BorderLayout.EAST, eastPanelSize)

  def northContent: Component = new JPanel()
  val northPanelSize: Int     = 50
  final lazy val northPanel   =
    new SlidingPanel(BorderLayout.NORTH, northPanelSize)

  def southContent: Component = new JPanel()
  val southPanelSize: Int     = 50
  final lazy val southPanel   =
    new SlidingPanel(BorderLayout.SOUTH, southPanelSize)

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

    rootFrame.setContentPane(mainContentPanel)
    rootFrame.setVisible(true)
  }

}

object GooeyDemo extends GooeyApp {

  override val appTitle: String = "Gooey Demo"

  // Create toggle buttons for sliding panels
  private val westToggleButton  = new JButton("Toggle West")
  private val eastToggleButton  = new JButton("Toggle East")
  private val northToggleButton = new JButton("Toggle North")
  private val southToggleButton = new JButton("Toggle South")
  private val settingsButton    = new JButton("Settings")

  // Setup action listeners for toggle buttons
  westToggleButton.addActionListener(_ => westPanel.toggle())
  eastToggleButton.addActionListener { _ =>
    mainContentPanel.revalidate()
    mainContentPanel.repaint()
  }
  northToggleButton.addActionListener(_ => northPanel.toggle())
  southToggleButton.addActionListener(_ => southPanel.toggle())

  // Settings button to swap content
  settingsButton.addActionListener(_ => setMainContent(createSettingsContent()))

  override val eastPanelSize: Int = 500

  // Create settings content
  private def createSettingsContent(): Component = {
    val panel = new JPanel(new BorderLayout())

    val settingsPanel = new JPanel(new GridLayout(5, 2, 10, 10))
    settingsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20))

    settingsPanel.add(new JLabel("Theme:"))
    settingsPanel.add(new JComboBox(Array("Light", "Dark", "System")))

    settingsPanel.add(new JLabel("Font Size:"))
    settingsPanel.add(new JComboBox(Array("Small", "Medium", "Large")))

    settingsPanel.add(new JLabel("Animation Speed:"))
    settingsPanel.add(new JSlider(SwingConstants.HORIZONTAL, 1, 10, 5))

    settingsPanel.add(new JLabel("Show Tooltips:"))
    settingsPanel.add(new JCheckBox("Enabled", true))

    val backButton = new JButton("Back to Main")
    backButton.addActionListener(_ => setMainContent(createMainContent()))

    panel.add(settingsPanel, BorderLayout.CENTER)
    panel.add(backButton, BorderLayout.SOUTH)

    panel
  }

  // Create the main content (moved from mainContent to a separate method)
  private def createMainContent(): Component = {
    val panel = new JPanel(new BorderLayout())

    // Center content - a scrollable text area with instructions
    val textArea = new JTextArea()
    textArea.setEditable(false)
    textArea.setWrapStyleWord(true)
    textArea.setLineWrap(true)
    textArea.setFont(new Font("SansSerif", Font.PLAIN, 14))
    textArea.setText(
      """Welcome to the Gooey Demo!
        |
        |This demo showcases the sliding panel functionality of the Gooey framework.
        |
        |Use the buttons at the bottom to toggle each panel:
        | - West Panel: Contains a list of sample items
        | - East Panel: Contains control settings
        | - North Panel: Contains a header with app info
        | - South Panel: Contains status information
        |
        |Each panel slides in and out with a smooth animation. Try resizing the window
        |to see how the panels adapt to the new dimensions.
        |
        |The Gooey framework makes it easy to create applications with consistent
        |layouts and behaviors across different platforms.
        |""".stripMargin
    )

    val scrollPane = new JScrollPane(textArea)
    panel.add(scrollPane, BorderLayout.CENTER)

    // Button panel at the bottom
    val buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER))
    buttonPanel.add(westToggleButton)
    buttonPanel.add(eastToggleButton)
//    buttonPanel.add(northToggleButton)
//    buttonPanel.add(southToggleButton)

    // Add button panel to the bottom of the center content
    panel.add(buttonPanel, BorderLayout.SOUTH)

    panel
  }

  // Main content now just returns the created content
  override def mainContent: Component = createMainContent()
}

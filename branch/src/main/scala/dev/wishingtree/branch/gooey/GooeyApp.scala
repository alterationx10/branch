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
  protected lazy val mainContentPanel = new JPanel(new BorderLayout())

  def westContent: Component = new JPanel()
  final lazy val westPanel         = new SlidingPanel(BorderLayout.WEST, westContent)

  def eastContent: Component = new JPanel()
  final lazy val eastPanel         = new SlidingPanel(BorderLayout.EAST, eastContent)

  def northContent: Component = new JPanel()
  final lazy val northPanel         = new SlidingPanel(BorderLayout.NORTH, northContent)

  def southContent: Component = new JPanel()
  final lazy val southPanel         = new SlidingPanel(BorderLayout.SOUTH, southContent)


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

    // Add center content
    mainContentPanel.add(mainContent, BorderLayout.CENTER)

    // Add sliding panels
    mainContentPanel.add(westPanel, BorderLayout.WEST)
    mainContentPanel.add(eastPanel, BorderLayout.EAST)
    mainContentPanel.add(northPanel, BorderLayout.NORTH)
    mainContentPanel.add(southPanel, BorderLayout.SOUTH)


    // Apply any additional configuration
    configureMainContentPanel(mainContentPanel)

    rootFrame.setContentPane(mainContentPanel)
    rootFrame.setVisible(true)
  }

}

object GooeyDemo extends GooeyApp {

  override val appTitle: String = "Gooey Demo"

  // Create toggle buttons for sliding panels
  private val westToggleButton = new JButton("Toggle West")
  private val eastToggleButton = new JButton("Toggle East")
  private val northToggleButton = new JButton("Toggle North")
  private val southToggleButton = new JButton("Toggle South")

  // Setup action listeners for toggle buttons
  westToggleButton.addActionListener(_ => westPanel.toggle())
  eastToggleButton.addActionListener(_ => eastPanel.toggle())
  northToggleButton.addActionListener(_ => northPanel.toggle())
  southToggleButton.addActionListener(_ => southPanel.toggle())

  westPanel.setSize(10,10)
  override def mainContent: Component = {
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
        |""".stripMargin)

    val scrollPane = new JScrollPane(textArea)
    panel.add(scrollPane, BorderLayout.CENTER)

    // Button panel at the bottom
    val buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER))
    buttonPanel.add(westToggleButton)
    buttonPanel.add(eastToggleButton)
    buttonPanel.add(northToggleButton)
    buttonPanel.add(southToggleButton)

    // Add button panel to the bottom of the center content
    panel.add(buttonPanel, BorderLayout.SOUTH)

    panel
  }
//
//  // West panel content - a list of items
//  override def westContent: Component = {
//    val panel = new JPanel(new BorderLayout())
//    panel.setBorder(BorderFactory.createTitledBorder("Navigation"))
//
//    val listModel = new DefaultListModel[String]()
//    for (i <- 1 to 20) {
//      listModel.addElement(s"Item $i")
//    }
//
//    val list = new JList(listModel)
//    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
//    list.setSelectedIndex(0)
//
//    val scrollPane = new JScrollPane(list)
//    panel.add(scrollPane, BorderLayout.CENTER)
//
//    val westControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER))
//    val addButton = new JButton("Add")
//    val removeButton = new JButton("Remove")
//
//    addButton.addActionListener(_ => {
//      val size = listModel.getSize
//      listModel.addElement(s"Item ${size + 1}")
//    })
//
//    removeButton.addActionListener(_ => {
//      val index = list.getSelectedIndex
//      if (index != -1) {
//        listModel.remove(index)
//      }
//    })
//
//    westControlPanel.add(addButton)
//    westControlPanel.add(removeButton)
//    panel.add(westControlPanel, BorderLayout.SOUTH)
//
//    panel
//  }
//
//  // East panel content - settings
//  override def eastContent: Component = {
//    val panel = new JPanel(new BorderLayout())
//    panel.setBorder(BorderFactory.createTitledBorder("Settings"))
//
//    val settingsPanel = new JPanel(new GridLayout(0, 1, 5, 5))
//    settingsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
//
//    // Add some sample settings
//    settingsPanel.add(new JLabel("Theme:"))
//    val themeCombo = new JComboBox(Array("Light", "Dark", "System"))
//    themeCombo.setSelectedIndex(0)
//    settingsPanel.add(themeCombo)
//
//    settingsPanel.add(new JLabel("Font Size:"))
//    val fontSizeSlider = new JSlider(SwingConstants.HORIZONTAL, 8, 24, 14)
//    fontSizeSlider.setMajorTickSpacing(4)
//    fontSizeSlider.setMinorTickSpacing(1)
//    fontSizeSlider.setPaintTicks(true)
//    fontSizeSlider.setPaintLabels(true)
//    settingsPanel.add(fontSizeSlider)
//
//    settingsPanel.add(new JLabel("Animation Speed:"))
//    val animSpeedSlider = new JSlider(SwingConstants.HORIZONTAL, 1, 5, 3)
//    animSpeedSlider.setMajorTickSpacing(1)
//    animSpeedSlider.setPaintTicks(true)
//    animSpeedSlider.setPaintLabels(true)
//    settingsPanel.add(animSpeedSlider)
//
//    settingsPanel.add(new JLabel("Notifications:"))
//    val notifCheckbox = new JCheckBox("Enable Notifications", true)
//    settingsPanel.add(notifCheckbox)
//
//    settingsPanel.add(new JLabel("Auto-save:"))
//    val autoSaveCheckbox = new JCheckBox("Enable Auto-save", true)
//    settingsPanel.add(autoSaveCheckbox)
//
//    val applyButton = new JButton("Apply Settings")
//    settingsPanel.add(applyButton)
//
//    panel.add(new JScrollPane(settingsPanel), BorderLayout.CENTER)
//
//    panel
//  }
//
//  // North panel content - app info header
//  override def northContent: Component = {
//    val panel = new JPanel(new BorderLayout())
//    panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10))
//
//    val titleLabel = new JLabel("Gooey Framework Demo Application", SwingConstants.LEFT)
//    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18))
//    panel.add(titleLabel, BorderLayout.WEST)
//
//    val buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT))
//    val helpButton = new JButton("Help")
//    val aboutButton = new JButton("About")
//
//    buttonPanel.add(helpButton)
//    buttonPanel.add(aboutButton)
//    panel.add(buttonPanel, BorderLayout.EAST)
//
//    panel
//  }
//
//  // South panel content - status bar
//  override def southContent: Component = {
//    val panel = new JPanel(new BorderLayout())
//    panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10))
//
//    val statusLabel = new JLabel("Ready", SwingConstants.LEFT)
//    panel.add(statusLabel, BorderLayout.WEST)
//
//    val memoryPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT))
//    val memoryBar = new JProgressBar(0, 100)
//    memoryBar.setValue(25)
//    memoryBar.setStringPainted(true)
//    memoryBar.setString("Memory: 25%")
//
//    // Update memory bar periodically
//    val memoryTimer = new Timer(2000, _ => {
//      val value = (Math.random() * 100).toInt
//      memoryBar.setValue(value)
//      memoryBar.setString(s"Memory: $value%")
//    })
//    memoryTimer.start()
//
//    memoryPanel.add(memoryBar)
//    panel.add(memoryPanel, BorderLayout.EAST)
//
//    panel
//  }
//
//  // Additional configuration for the root frame
//  override def configureRootFrame(rootFrame: JFrame): JFrame = {
//    val decorated = super.configureRootFrame(rootFrame)
//
//    // Set a custom icon (in a real app, you'd use an actual icon)
//    try {
//      val emptyImage = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
//      val graphics = emptyImage.createGraphics()
//      graphics.setColor(Color.BLUE)
//      graphics.fillRect(0, 0, 16, 16)
//      graphics.dispose()
//
//      decorated.setIconImage(emptyImage)
//    } catch {
//      case _: Exception => // Ignore if we can't set the icon
//    }
//
//    decorated
//  }
//
//  // Additional configuration for the main content panel
//  override def configureMainContentPanel(panel: JPanel): JPanel = {
//    val configured = super.configureMainContentPanel(panel)
//
//    // Set a nice background color
//    configured.setBackground(new Color(240, 240, 245))
//
//    configured
//  }
}
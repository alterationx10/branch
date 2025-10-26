package dev.alteration.branch.gooey.layout

import javax.swing.JPanel
import java.awt.GridLayout

/** Grid layout container.
  *
  * Arranges children in a grid with the specified number of columns. Rows are
  * added automatically as needed.
  *
  * @param columns
  *   Number of columns in the grid
  * @param spacing
  *   Space between grid cells in pixels (default: 8)
  * @param children
  *   Child components to arrange in grid
  */
case class Grid(columns: Int, spacing: Int = 8, children: Component*)
    extends Component {

  override def render(): RenderedComponent = {
    val rows = (children.size + columns - 1) / columns

    val panel = new JPanel()
    panel.setLayout(new GridLayout(rows, columns, spacing, spacing))
    panel.setOpaque(false)

    children.foreach { child =>
      val rendered = child.render()
      panel.add(rendered.asJComponent)
    }

    // Fill empty cells if needed
    val emptyCells = (rows * columns) - children.size
    if (emptyCells > 0) {
      (1 to emptyCells).foreach { _ =>
        val filler = new JPanel()
        filler.setOpaque(false)
        panel.add(filler)
      }
    }

    RenderedComponent(panel)
  }

}

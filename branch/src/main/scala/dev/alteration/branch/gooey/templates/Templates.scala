package dev.alteration.branch.gooey.templates

import dev.alteration.branch.gooey.layout.{Component, HStack, VStack}
import dev.alteration.branch.gooey.components.Text
import dev.alteration.branch.gooey.modifiers.*
import dev.alteration.branch.gooey.prebuilt.Cards
import dev.alteration.branch.gooey.styling.{withAlpha, Alignment, Colors, Fonts}

/** Pre-built application layout templates for common UI patterns. */
object Templates {

  /** Creates a three-column layout (sidebar, content, inspector).
    *
    * Common in applications like IDEs, design tools, etc.
    *
    * @param sidebar
    *   Left sidebar component
    * @param content
    *   Main content component
    * @param inspector
    *   Right inspector/properties panel component
    * @param sidebarWidth
    *   Width of sidebar in pixels (default: 200)
    * @param inspectorWidth
    *   Width of inspector in pixels (default: 250)
    * @return
    *   A three-column layout component
    */
  def threeColumn(
      sidebar: Component,
      content: Component,
      inspector: Component,
      sidebarWidth: Int = 200,
      inspectorWidth: Int = 250
  ): Component =
    HStack(
      spacing = 0,
      alignment = Alignment.center,
      sidebar.frame(sidebarWidth),
      content, // Takes remaining space
      inspector.frame(inspectorWidth)
    )

  /** Creates a master-detail layout.
    *
    * Common in mail clients, file browsers, etc.
    *
    * @param items
    *   List of items for the master list
    * @param selectedIndex
    *   Index of currently selected item
    * @param detailView
    *   Component to show in detail pane
    * @param masterWidth
    *   Width of master list in pixels (default: 300)
    * @return
    *   A master-detail layout component
    */
  def masterDetail(
      items: Seq[Component],
      selectedIndex: Int,
      detailView: Component,
      masterWidth: Int = 300
  ): Component =
    HStack(
      spacing = 0,
      alignment = Alignment.center,
      // Master list
      VStack(
        spacing = 0,
        alignment = Alignment.leading,
        items.zipWithIndex.map { case (item, idx) =>
          item.background(
            if (idx == selectedIndex) Colors.blue.withAlpha(0.1f)
            else Colors.transparent
          )
        }*
      )
        .frame(masterWidth)
        .background(Colors.gray(0.95f)),
      // Detail view
      detailView.padding(24)
    )

  /** Creates a dashboard layout with widget cards.
    *
    * @param widgets
    *   Sequence of (title, content) tuples for each widget
    * @return
    *   A dashboard layout component
    */
  def dashboard(widgets: Seq[(String, Component)]): Component =
    VStack(
      spacing = 24,
      alignment = Alignment.leading,
      widgets.map { case (title, widget) =>
        Cards.cardWithHeader(title, widget)
      }*
    ).padding(24)

  /** Creates a settings page layout with sections.
    *
    * @param sections
    *   Sequence of (section title, fields) tuples
    * @return
    *   A settings page layout component
    */
  def settingsPage(sections: Seq[(String, Seq[Component])]): Component =
    VStack(
      spacing = 32,
      alignment = Alignment.leading,
      sections.map { case (sectionTitle, fields) =>
        VStack(
          spacing = 16,
          alignment = Alignment.leading,
          (Seq(Text(sectionTitle).font(Fonts.headline)) ++ fields)*
        )
      }*
    ).padding(24)

}

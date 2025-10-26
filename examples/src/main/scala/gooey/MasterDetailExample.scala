package gooey

import dev.alteration.branch.gooey.GooeyApp
import dev.alteration.branch.gooey.layout.VStack
import dev.alteration.branch.gooey.components.Text
import dev.alteration.branch.gooey.templates.Templates
import dev.alteration.branch.gooey.prebuilt.Lists
import dev.alteration.branch.gooey.styling.{Fonts, Alignment}

import java.awt.Component

/** Master-detail layout example (like a mail client). */
object MasterDetailExample extends GooeyApp {

  override val appTitle: String = "Master-Detail Example"

  case class Email(
      from: String,
      subject: String,
      preview: String,
      body: String
  )

  val emails = Seq(
    Email(
      "alice@example.com",
      "Project Update",
      "Here's the latest on the project...",
      "Hi team,\n\nI wanted to give you all an update on the project progress. We've completed 75% of the planned features..."
    ),
    Email(
      "bob@example.com",
      "Meeting Tomorrow",
      "Don't forget about our meeting...",
      "Hey everyone,\n\nJust a friendly reminder about our meeting tomorrow at 10 AM. Please review the agenda beforehand."
    ),
    Email(
      "charlie@example.com",
      "Question about API",
      "I have a question regarding...",
      "Hi,\n\nI'm working with the new API and I'm not sure how to handle authentication. Could you help me understand the flow?"
    )
  )

  val selectedIndex = 0
  val selectedEmail = emails(selectedIndex)

  override def mainContent: Component = {
    Templates
      .masterDetail(
        items = emails.map { email =>
          Lists.listItem(
            title = email.from,
            subtitle = Some(email.subject)
          )
        },
        selectedIndex = selectedIndex,
        detailView = VStack(
          spacing = 16,
          alignment = Alignment.leading,
          children = Seq(
            Text(selectedEmail.subject).font(Fonts.title),
            Text(s"From: ${selectedEmail.from}").font(Fonts.caption),
            Text(selectedEmail.body).font(Fonts.body)
          )*
        ),
        masterWidth = 320
      )
      .render()
      .asJComponent
  }

}

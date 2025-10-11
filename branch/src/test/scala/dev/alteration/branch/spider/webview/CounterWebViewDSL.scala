package dev.alteration.branch.spider.webview

import dev.alteration.branch.spider.webview.html._

/** Example state for a counter WebView */
case class CounterStateDSL(count: Int, name: String)

/** Modernized CounterWebView using the HTML DSL.
  *
  * This demonstrates the improved developer experience with:
  *   - Type-safe HTML construction
  *   - Automatic escaping (no manual escapeHtml needed)
  *   - Composable elements
  *   - Clean, readable code
  *
  * Compare this to the original CounterWebView to see the improvement!
  */
class CounterWebViewDSL extends WebView[CounterStateDSL] {

  override def mount(
      params: Map[String, String],
      session: Session
  ): CounterStateDSL = {
    val initialCount = params.get("count").flatMap(_.toIntOption).getOrElse(0)
    val initialName = params.getOrElse("name", "World")
    CounterStateDSL(count = initialCount, name = initialName)
  }

  override def handleEvent(
      event: String,
      payload: Map[String, Any],
      state: CounterStateDSL
  ): CounterStateDSL = {
    val newState = event match {
      case "increment" =>
        state.copy(count = state.count + 1)

      case "decrement" =>
        state.copy(count = state.count - 1)

      case "reset" =>
        state.copy(count = 0)

      case "update-name" =>
        val newName = payload.get("value").map(_.toString).getOrElse(state.name)
        state.copy(name = newName)

      case _ =>
        state
    }
    println(s"$state => $newState")
    newState
  }

  override def render(state: CounterStateDSL): String = {
    // No need for s"""...""" string interpolation!
    // No need for manual escapeHtml!
    // Type-safe attributes with := operator!

    div(
      style := "font-family: Arial, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; border: 1px solid #ccc; border-radius: 8px;"
    )(
      h1(style := "text-align: center; color: #333;")(
        text("Branch WebView Counter")
      ),
      div(style := "text-align: center; margin: 30px 0;")(
        h2()(text(s"Hello, ${state.name}!")) // Automatic escaping!
      ),
      div(style := "text-align: center; margin: 30px 0;")(
        div(
          style := s"font-size: 48px; font-weight: bold; color: ${getCountColor(state.count)};"
        )(
          text(state.count)
        )
      ),
      div(
        style := "display: flex; gap: 10px; justify-content: center; margin: 20px 0;"
      )(
        button(
          wvClick := "decrement",
          style := "padding: 10px 20px; font-size: 18px; cursor: pointer; background: #f44336; color: white; border: none; border-radius: 4px;"
        )(
          text("-")
        ),
        button(
          wvClick := "reset",
          style := "padding: 10px 20px; font-size: 18px; cursor: pointer; background: #9e9e9e; color: white; border: none; border-radius: 4px;"
        )(
          text("Reset")
        ),
        button(
          wvClick := "increment",
          style := "padding: 10px 20px; font-size: 18px; cursor: pointer; background: #4caf50; color: white; border: none; border-radius: 4px;"
        )(
          text("+")
        )
      ),
      div(style := "margin: 30px 0;")(
        label(
          style := "display: block; margin-bottom: 8px; font-weight: bold;"
        )(
          text("Your Name:")
        ),
        input(
          tpe := "text",
          wvChange := "update-name",
          value := state.name,
          style := "width: 100%; padding: 10px; font-size: 16px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box;"
        )
      ),
      div(
        style := "text-align: center; margin-top: 40px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 14px;"
      )(
        p()(text("This is a real-time, stateful UI powered by Branch WebView")),
        p()(text("State lives on the server, updates happen over WebSocket")),
        p(style := "color: #4caf50; font-weight: bold;")(
          text("✨ Built with the Branch HTML DSL!")
        )
      )
    ).render // Convert Html to String
  }

  /** Get color based on count value */
  private def getCountColor(count: Int): String = {
    if count > 0 then "#4caf50"     // Green for positive
    else if count < 0 then "#f44336" // Red for negative
    else "#333"                      // Dark gray for zero
  }

  override def terminate(reason: Option[Throwable], state: CounterStateDSL): Unit = {
    println(s"CounterWebViewDSL terminated. Final count: ${state.count}")
  }
}

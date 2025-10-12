package dev.alteration.branch.spider.webview.styling

import dev.alteration.branch.spider.webview.html.Html

/** Styling utilities for Branch WebView.
  *
  * Provides CSS-in-Scala capabilities with automatic scoping.
  */

/** String interpolator for CSS blocks.
  *
  * Example:
  * {{{
  * val styles = css"""
  *   .container {
  *     max-width: 600px;
  *     margin: 0 auto;
  *   }
  * """
  * }}}
  */
extension (sc: StringContext) {
  def css(args: Any*): String =
    sc.s(args*)
}

/** Create a style tag from CSS text.
  *
  * @param cssText
  *   The CSS content
  * @return
  *   An Html.Raw element containing the style tag
  */
def styleTag(cssText: String): Html =
  Html.Raw(s"<style>\n${cssText.trim}\n</style>")

/** Create inline styles from property pairs.
  *
  * @param properties
  *   CSS property-value pairs
  * @return
  *   A style attribute string
  */
def inlineStyle(properties: (String, String)*): String =
  properties.map { case (prop, value) => s"$prop: $value" }.mkString("; ")

/** CSS reset/normalize styles for consistent rendering. */
val cssReset: String = """
  * {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
  }

  html, body {
    height: 100%;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
  }

  button {
    font-family: inherit;
  }
"""

/** Common utility classes (Tailwind-inspired). */
val utilityClasses: String = """
  /* Flexbox utilities */
  .flex { display: flex; }
  .flex-col { flex-direction: column; }
  .flex-row { flex-direction: row; }
  .items-center { align-items: center; }
  .items-start { align-items: flex-start; }
  .items-end { align-items: flex-end; }
  .justify-center { justify-content: center; }
  .justify-between { justify-content: space-between; }
  .justify-around { justify-content: space-around; }
  .gap-1 { gap: 4px; }
  .gap-2 { gap: 8px; }
  .gap-3 { gap: 12px; }
  .gap-4 { gap: 16px; }
  .gap-6 { gap: 24px; }

  /* Grid utilities */
  .grid { display: grid; }
  .grid-cols-2 { grid-template-columns: repeat(2, 1fr); }
  .grid-cols-3 { grid-template-columns: repeat(3, 1fr); }
  .grid-cols-4 { grid-template-columns: repeat(4, 1fr); }

  /* Spacing utilities */
  .m-0 { margin: 0; }
  .m-1 { margin: 4px; }
  .m-2 { margin: 8px; }
  .m-4 { margin: 16px; }
  .m-6 { margin: 24px; }
  .mt-2 { margin-top: 8px; }
  .mt-4 { margin-top: 16px; }
  .mb-2 { margin-bottom: 8px; }
  .mb-4 { margin-bottom: 16px; }
  .p-0 { padding: 0; }
  .p-2 { padding: 8px; }
  .p-4 { padding: 16px; }
  .p-6 { padding: 24px; }

  /* Text utilities */
  .text-center { text-align: center; }
  .text-left { text-align: left; }
  .text-right { text-align: right; }
  .text-sm { font-size: 0.875rem; }
  .text-base { font-size: 1rem; }
  .text-lg { font-size: 1.125rem; }
  .text-xl { font-size: 1.25rem; }
  .text-2xl { font-size: 1.5rem; }
  .font-bold { font-weight: bold; }

  /* Color utilities */
  .text-gray { color: #a0aec0; }
  .text-dark { color: #2d3748; }
  .bg-light { background-color: #f7fafc; }
  .bg-white { background-color: white; }

  /* Border utilities */
  .rounded { border-radius: 4px; }
  .rounded-md { border-radius: 8px; }
  .rounded-lg { border-radius: 12px; }
  .rounded-full { border-radius: 9999px; }
  .border { border: 1px solid #e2e8f0; }

  /* Shadow utilities */
  .shadow-sm { box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
  .shadow { box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
  .shadow-lg { box-shadow: 0 10px 15px rgba(0,0,0,0.1); }

  /* Display utilities */
  .hidden { display: none; }
  .block { display: block; }
  .inline-block { display: inline-block; }

  /* Width/Height utilities */
  .w-full { width: 100%; }
  .h-full { height: 100%; }
  .max-w-sm { max-width: 384px; }
  .max-w-md { max-width: 448px; }
  .max-w-lg { max-width: 512px; }
  .max-w-xl { max-width: 576px; }
  .max-w-2xl { max-width: 672px; }

  /* Cursor utilities */
  .cursor-pointer { cursor: pointer; }
  .cursor-not-allowed { cursor: not-allowed; }
"""

/** Complete base stylesheet including reset and utilities. */
val baseStyles: String = cssReset + "\n" + utilityClasses

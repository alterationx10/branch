package dev.alteration.branch.spider.webview

import scala.io.StdIn

/** Improved WebView demo showcasing Phase 4a ergonomics improvements.
  *
  * This demonstrates:
  *   - ✅ HTML DSL with type-safe elements
  *   - ✅ WebView-specific attributes (wvClick, wvChange)
  *   - ✅ Component helpers (textInput, clickButton)
  *   - ✅ WebViewServer builder API
  *
  * Compare this to WebViewDemo to see the improvement!
  *
  * To run:
  * {{{
  *   sbt "testOnly *ImprovedWebViewDemo"
  * }}}
  */
object ImprovedWebViewDemo {

  def main(args: Array[String]): Unit = {
    // Phase 4a: WebViewServer builder API
    // Before: Manual ActorSystem + WebViewHandler + WebSocketServer.start
    // After: Fluent builder API!
    val server = WebViewServer()
      .withRouteFactory("/counter", () => new CounterWebViewDSL())
      .start(port = 9001)

    server.printInfo()

    println("To test the WebView:")
    println("1. Save the HTML below to a file (e.g., improved-counter.html)")
    println("2. Open it in a web browser")
    println("3. The counter should load and be interactive")
    println()
    println("HTML to save:")
    println("-" * 60)
    println(demoHtml)
    println("-" * 60)
    println()
    println("Press ENTER to stop the server...")

    StdIn.readLine()

    server.stop()
    println("Goodbye!")
  }

  /** Demo HTML page (same client code as before) */
  val demoHtml: String = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Branch WebView Counter Demo (Improved)</title>
</head>
<body>
  <div id="root">
    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; text-align: center;">
      <h1>Connecting...</h1>
      <p>Please wait while we connect to the WebView server.</p>
    </div>
  </div>

  <script>
    class BranchWebView {
      constructor(url, options = {}) {
        this.url = url;
        this.options = {
          rootSelector: options.rootSelector || '#root',
          reconnectDelay: options.reconnectDelay || 1000,
          maxReconnectDelay: options.maxReconnectDelay || 30000,
          heartbeatInterval: options.heartbeatInterval || 30000,
          debug: options.debug || false,
          ...options
        };

        this.ws = null;
        this.reconnectAttempts = 0;
        this.heartbeatTimer = null;
        this.isConnected = false;

        this.init();
      }

      init() {
        this.connect();
        this.setupEventDelegation();
      }

      connect() {
        console.log('[BranchWebView] Connecting to', this.url);

        try {
          this.ws = new WebSocket(this.url);

          this.ws.onopen = () => {
            console.log('[BranchWebView] Connected');
            this.isConnected = true;
            this.reconnectAttempts = 0;
            this.send({ type: 'ready' });
            this.startHeartbeat();
          };

          this.ws.onmessage = (event) => {
            this.handleMessage(event.data);
          };

          this.ws.onerror = (error) => {
            console.error('[BranchWebView] WebSocket error:', error);
          };

          this.ws.onclose = () => {
            console.log('[BranchWebView] Connection closed');
            this.isConnected = false;
            this.stopHeartbeat();
            this.scheduleReconnect();
          };
        } catch (error) {
          console.error('[BranchWebView] Failed to create WebSocket:', error);
          this.scheduleReconnect();
        }
      }

      scheduleReconnect() {
        const delay = Math.min(
          this.options.reconnectDelay * Math.pow(2, this.reconnectAttempts),
          this.options.maxReconnectDelay
        );

        console.log(`[BranchWebView] Reconnecting in ${delay}ms`);

        setTimeout(() => {
          this.reconnectAttempts++;
          this.connect();
        }, delay);
      }

      startHeartbeat() {
        this.stopHeartbeat();
        this.heartbeatTimer = setInterval(() => {
          if (this.isConnected) {
            this.send({ type: 'ping' });
          }
        }, this.options.heartbeatInterval);
      }

      stopHeartbeat() {
        if (this.heartbeatTimer) {
          clearInterval(this.heartbeatTimer);
          this.heartbeatTimer = null;
        }
      }

      send(message) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
          this.ws.send(JSON.stringify(message));
          if (this.options.debug) {
            console.log('[BranchWebView] Sent:', message);
          }
        }
      }

      handleMessage(data) {
        try {
          const message = JSON.parse(data);
          if (this.options.debug) {
            console.log('[BranchWebView] Received:', message);
          }

          switch (message.type) {
            case 'replace':
              this.handleReplace(message);
              break;
            case 'patch':
              this.handlePatch(message);
              break;
            case 'pong':
              break;
            case 'error':
              console.error('[BranchWebView] Server error:', message.message);
              break;
          }
        } catch (error) {
          console.error('[BranchWebView] Error parsing message:', error);
        }
      }

      handleReplace(message) {
        const target = message.target || 'root';
        const selector = target === 'root' ? this.options.rootSelector : `#${target}`;
        const element = document.querySelector(selector);

        if (element) {
          element.innerHTML = message.html;
        } else {
          console.error('[BranchWebView] Target element not found:', selector);
        }
      }

      handlePatch(message) {
        const element = document.getElementById(message.target);
        if (element) {
          element.innerHTML = message.html;
        }
      }

      setupEventDelegation() {
        document.addEventListener('click', (e) => {
          const target = e.target.closest('[wv-click]');
          if (target) {
            e.preventDefault();
            const event = target.getAttribute('wv-click');
            this.sendEvent('click', event, target, null);
          }
        });

        document.addEventListener('change', (e) => {
          const target = e.target.closest('[wv-change]');
          if (target) {
            const event = target.getAttribute('wv-change');
            const value = this.getInputValue(target);
            this.sendEvent('change', event, target, value);
          }
        });
      }

      getInputValue(element) {
        if (element.type === 'checkbox') {
          return element.checked;
        } else if (element.type === 'radio') {
          return element.checked ? element.value : null;
        } else {
          return element.value;
        }
      }

      sendEvent(eventType, eventName, target, value) {
        this.send({
          type: 'event',
          event: eventName,
          target: target.id || 'unknown',
          value: value
        });
      }
    }

    // Initialize WebView connection
    const webview = new BranchWebView('ws://localhost:9001/counter', {
      debug: true
    });
  </script>
</body>
</html>"""
}

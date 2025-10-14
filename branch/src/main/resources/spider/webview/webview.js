/**
 * Branch WebView Client
 *
 * A lightweight JavaScript library for real-time, stateful web UIs.
 * Connects to a Branch WebView server over WebSocket and handles
 * DOM updates and event routing.
 *
 * Usage:
 *   <script src="/webview/webview.js"></script>
 *   <script>
 *     const webview = new BranchWebView('ws://localhost:8080/webview');
 *   </script>
 */

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

  /**
   * Initialize the WebView connection
   */
  init() {
    this.connect();
    this.setupEventDelegation();
  }

  /**
   * Establish WebSocket connection
   */
  connect() {
    this.log('Connecting to', this.url);

    try {
      this.ws = new WebSocket(this.url);

      this.ws.onopen = () => {
        this.log('Connected');
        this.isConnected = true;
        this.reconnectAttempts = 0;

        // Send ready message
        this.send({ type: 'ready' });

        // Start heartbeat
        this.startHeartbeat();

        // Fire connected event
        this.fireEvent('connected');
      };

      this.ws.onmessage = (event) => {
        this.handleMessage(event.data);
      };

      this.ws.onerror = (error) => {
        this.log('WebSocket error:', error);
        this.fireEvent('error', { error });
      };

      this.ws.onclose = () => {
        this.log('Connection closed');
        this.isConnected = false;
        this.stopHeartbeat();
        this.fireEvent('disconnected');
        this.scheduleReconnect();
      };
    } catch (error) {
      this.log('Failed to create WebSocket:', error);
      this.scheduleReconnect();
    }
  }

  /**
   * Schedule a reconnection attempt
   */
  scheduleReconnect() {
    const delay = Math.min(
      this.options.reconnectDelay * Math.pow(2, this.reconnectAttempts),
      this.options.maxReconnectDelay
    );

    this.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts + 1})`);

    setTimeout(() => {
      this.reconnectAttempts++;
      this.connect();
    }, delay);
  }

  /**
   * Start heartbeat timer
   */
  startHeartbeat() {
    this.stopHeartbeat();

    this.heartbeatTimer = setInterval(() => {
      if (this.isConnected) {
        this.send({ type: 'ping' });
      }
    }, this.options.heartbeatInterval);
  }

  /**
   * Stop heartbeat timer
   */
  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  /**
   * Send a message to the server
   */
  send(message) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
      this.log('Sent:', message);
    } else {
      this.log('Cannot send message, not connected');
    }
  }

  /**
   * Handle incoming message from server
   */
  handleMessage(data) {
    try {
      const message = JSON.parse(data);
      this.log('Received:', message);

      switch (message.type) {
        case 'replace':
          this.handleReplace(message);
          break;
        case 'patch':
          this.handlePatch(message);
          break;
        case 'pong':
          // Heartbeat response, do nothing
          break;
        case 'error':
          this.handleError(message);
          break;
        default:
          this.log('Unknown message type:', message.type);
      }
    } catch (error) {
      this.log('Error parsing message:', error, data);
    }
  }

  /**
   * Handle replace message - replace entire element
   */
  handleReplace(message) {
    const target = message.target || 'root';
    const selector = target === 'root' ? this.options.rootSelector : `#${target}`;
    const element = document.querySelector(selector);

    if (element) {
      element.innerHTML = message.html;
      this.fireEvent('updated', { target, html: message.html });
    } else {
      this.log('Target element not found:', selector);
    }
  }

  /**
   * Handle patch message - update specific element
   */
  handlePatch(message) {
    const element = document.getElementById(message.target);

    if (element) {
      element.innerHTML = message.html;
      this.fireEvent('patched', { target: message.target, html: message.html });
    } else {
      this.log('Patch target not found:', message.target);
    }
  }

  /**
   * Handle error message from server
   */
  handleError(message) {
    this.log('Server error:', message.message);
    this.fireEvent('server-error', { message: message.message });
  }

  /**
   * Setup event delegation for WebView events
   */
  setupEventDelegation() {
    // Handle clicks
    document.addEventListener('click', (e) => {
      const target = e.target.closest('[wv-click]');
      if (target) {
        e.preventDefault();
        const event = target.getAttribute('wv-click');
        this.sendEvent('click', event, target, null);
      }
    });

    // Handle input changes
    document.addEventListener('change', (e) => {
      const target = e.target.closest('[wv-change]');
      if (target) {
        const event = target.getAttribute('wv-change');
        const value = this.getInputValue(target);
        this.sendEvent('change', event, target, value);
      }
    });

    // Handle form submissions
    document.addEventListener('submit', (e) => {
      const form = e.target.closest('[wv-submit]');
      if (form) {
        e.preventDefault();
        const event = form.getAttribute('wv-submit');
        const formData = new FormData(form);
        const value = Object.fromEntries(formData.entries());
        this.sendEvent('submit', event, form, value);
      }
    });

    // Handle keyboard events
    document.addEventListener('keyup', (e) => {
      const target = e.target.closest('[wv-keyup]');
      if (target) {
        const event = target.getAttribute('wv-keyup');
        const value = this.getInputValue(target);
        this.sendEvent('keyup', event, target, value);
      }
    });
  }

  /**
   * Get value from an input element
   */
  getInputValue(element) {
    if (element.type === 'checkbox') {
      return element.checked;
    } else if (element.type === 'radio') {
      return element.checked ? element.value : null;
    } else {
      return element.value;
    }
  }

  /**
   * Send an event to the server
   */
  sendEvent(eventType, eventJsonStr, target, value) {
    // EventCodec events are JSON strings. Parse to avoid double-stringification.
    // Forms may use simple event names, which will fail JSON.parse - that's fine.
    let event = eventJsonStr;
    if (typeof eventJsonStr === 'string') {
      try {
        event = JSON.parse(eventJsonStr);
      } catch (e) {
        // Not JSON (e.g., "AddTodo" from a form), keep as string
      }
    }

    this.send({
      type: 'event',
      event: event,
      target: target.id || 'unknown',
      value: value
    });
  }

  /**
   * Fire a custom event on the document
   */
  fireEvent(name, detail = {}) {
    document.dispatchEvent(new CustomEvent(`webview:${name}`, { detail }));
  }

  /**
   * Log a message (if debug is enabled)
   */
  log(...args) {
    if (this.options.debug) {
      console.log('[BranchWebView]', ...args);
    }
  }

  /**
   * Close the connection
   */
  close() {
    this.stopHeartbeat();
    if (this.ws) {
      this.ws.close();
    }
  }
}

// Export for both browser and module environments
if (typeof module !== 'undefined' && module.exports) {
  module.exports = BranchWebView;
}
if (typeof window !== 'undefined') {
  window.BranchWebView = BranchWebView;
}

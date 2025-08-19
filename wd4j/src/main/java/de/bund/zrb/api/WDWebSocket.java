package de.bund.zrb.api;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface WDWebSocket {

  void onClose(Consumer<WDWebSocket> handler);
  void offClose(Consumer<WDWebSocket> handler);
  void onFrameReceived(Consumer<WebSocketFrame> handler);
  void offFrameReceived(Consumer<WebSocketFrame> handler);
  void onFrameSent(Consumer<WebSocketFrame> handler);
  void offFrameSent(Consumer<WebSocketFrame> handler);
  void onSocketError(Consumer<String> handler);
  void offSocketError(Consumer<String> handler);

  class WaitForFrameReceivedOptions {
    public Predicate<WebSocketFrame> predicate;
    public Double timeout;
    public WaitForFrameReceivedOptions setPredicate(Predicate<WebSocketFrame> predicate) {
      this.predicate = predicate;
      return this;
    }
    public WaitForFrameReceivedOptions setTimeout(double timeout) {
      this.timeout = timeout;
      return this;
    }
  }
  class WaitForFrameSentOptions {
    public Predicate<WebSocketFrame> predicate;
    public Double timeout;
    public WaitForFrameSentOptions setPredicate(Predicate<WebSocketFrame> predicate) {
      this.predicate = predicate;
      return this;
    }
    public WaitForFrameSentOptions setTimeout(double timeout) {
      this.timeout = timeout;
      return this;
    }
  }
  boolean isClosed();
  String url();
  default WebSocketFrame waitForFrameReceived(Runnable callback) {
    return waitForFrameReceived(null, callback);
  }
  WebSocketFrame waitForFrameReceived(WaitForFrameReceivedOptions options, Runnable callback);
  default WebSocketFrame waitForFrameSent(Runnable callback) {
    return waitForFrameSent(null, callback);
  }
  WebSocketFrame waitForFrameSent(WaitForFrameSentOptions options, Runnable callback);
}


package io.bobba.woodsheep.net;

import io.bobba.woodsheep.core.gameclients.GameClientRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@AllArgsConstructor
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
  private final GameClientRegistry registry;

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    log.trace("Connected: {}", session.getId());
    registry.add(session);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    log.trace("Disconnected: {}", session.getId());
    registry.remove(session);
  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    log.trace("Received: {}", message.getPayload());
    registry.handleMessage(session, message.getPayload());
  }
}

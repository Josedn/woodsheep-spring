package io.bobba.woodsheep.net;

import static org.mockito.Mockito.*;

import io.bobba.woodsheep.core.gameclients.GameClientRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class GameWebSocketHandlerTest {

  private GameClientRegistry registry;
  private GameWebSocketHandler handler;
  private WebSocketSession session;

  @BeforeEach
  void setUp() {
    registry = mock(GameClientRegistry.class);
    handler = new GameWebSocketHandler(registry);
    session = mock(WebSocketSession.class);
    when(session.getId()).thenReturn("session-1");
  }

  @Test
  void afterConnectionEstablished_delegatesToRegistry() {
    handler.afterConnectionEstablished(session);

    verify(registry).add(session);
    verifyNoMoreInteractions(registry);
  }

  @Test
  void afterConnectionClosed_delegatesToRegistry() throws Exception {
    handler.afterConnectionClosed(session, CloseStatus.NORMAL);

    verify(registry).remove(session);
    verifyNoMoreInteractions(registry);
  }

  @Test
  void handleTextMessage_delegatesPayloadToRegistry() throws Exception {
    TextMessage message = new TextMessage("{\"requestType\":\"ping\",\"payload\":{}}");

    handler.handleTextMessage(session, message);

    verify(registry).handleMessage(session, message.getPayload());
    verifyNoMoreInteractions(registry);
  }
}

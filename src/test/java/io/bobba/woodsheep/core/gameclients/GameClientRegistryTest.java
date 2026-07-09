package io.bobba.woodsheep.core.gameclients;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class GameClientRegistryTest {

  private GameClientMessageHandler messageHandler;
  private GameClientRegistry registry;
  private WebSocketSession session;

  @BeforeEach
  void setUp() {
    messageHandler = mock(GameClientMessageHandler.class);
    registry = new GameClientRegistry(messageHandler);
    session = mock(WebSocketSession.class);
    when(session.getId()).thenReturn("session-1");
    when(session.isOpen()).thenReturn(true);
  }

  @Test
  void add_thenHandleMessage_dispatchesToMessageHandler() {
    registry.add(session);
    registry.handleMessage(session, "{\"requestType\":\"login\",\"payload\":{\"sso\":\"tok\"}}");

    verify(messageHandler)
        .handleMessage(
            argThat(c -> c.getId().equals("session-1")),
            eq("{\"requestType\":\"login\",\"payload\":{\"sso\":\"tok\"}}"));
  }

  @Test
  void remove_stopsClientAndDropsFromRegistry() throws Exception {
    registry.add(session);
    registry.remove(session);

    // After removal, the session should have been closed (stop() closes open sessions).
    verify(session, atLeastOnce()).isOpen();
    verify(session).close();
  }

  @Test
  void add_multipleSessions_eachDispatchedIndependently() {
    WebSocketSession session2 = mock(WebSocketSession.class);
    when(session2.getId()).thenReturn("session-2");
    when(session2.isOpen()).thenReturn(true);

    registry.add(session);
    registry.add(session2);

    String msg1 = "{\"requestType\":\"login\",\"payload\":{\"sso\":\"a\"}}";
    String msg2 = "{\"requestType\":\"login\",\"payload\":{\"sso\":\"b\"}}";

    registry.handleMessage(session, msg1);
    registry.handleMessage(session2, msg2);

    verify(messageHandler).handleMessage(argThat(c -> c.getId().equals("session-1")), eq(msg1));
    verify(messageHandler).handleMessage(argThat(c -> c.getId().equals("session-2")), eq(msg2));
  }
}

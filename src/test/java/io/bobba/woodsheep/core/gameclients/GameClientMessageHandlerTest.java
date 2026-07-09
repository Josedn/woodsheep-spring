package io.bobba.woodsheep.core.gameclients;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import io.bobba.woodsheep.core.communication.protocol.EmptyObjectPayload;
import io.bobba.woodsheep.core.communication.protocol.IncomingEventHandler;
import io.bobba.woodsheep.core.communication.protocol.OpCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class GameClientMessageHandlerTest {

  // ── helpers ────────────────────────────────────────────────────────────────

  /** Builds a real GameClient backed by a closed mock session (no I/O side-effects). */
  private GameClient stubClient() {
    WebSocketSession session = mock(WebSocketSession.class);
    when(session.getId()).thenReturn("s-1");
    when(session.isOpen()).thenReturn(false);
    return new GameClient("s-1", session);
  }

  @OpCode("testOp")
  static class TestHandler implements IncomingEventHandler<EmptyObjectPayload> {
    boolean called = false;
    GameClient lastClient;

    @Override
    public void handle(GameClient client, EmptyObjectPayload payload) {
      called = true;
      lastClient = client;
    }

    @Override
    public Class<EmptyObjectPayload> payloadType() {
      return EmptyObjectPayload.class;
    }
  }

  // ── construction ───────────────────────────────────────────────────────────

  @Test
  void constructor_missingOpCode_throwsIllegalState() {
    IncomingEventHandler<EmptyObjectPayload> noAnnotation =
        new IncomingEventHandler<>() {
          @Override
          public void handle(GameClient client, EmptyObjectPayload payload) {}

          @Override
          public Class<EmptyObjectPayload> payloadType() {
            return EmptyObjectPayload.class;
          }
        };

    assertThatThrownBy(() -> new GameClientMessageHandler(List.of(noAnnotation)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("missing @OpCode");
  }

  @Test
  void constructor_duplicateOpCode_throwsIllegalState() {
    TestHandler h1 = new TestHandler();
    TestHandler h2 = new TestHandler();

    assertThatThrownBy(() -> new GameClientMessageHandler(List.of(h1, h2)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Duplicate OpCode");
  }

  // ── dispatch ───────────────────────────────────────────────────────────────

  @Test
  void handleMessage_knownOpCode_invokesHandler() {
    TestHandler handler = new TestHandler();
    GameClientMessageHandler messageHandler = new GameClientMessageHandler(List.of(handler));
    GameClient client = stubClient();

    messageHandler.handleMessage(client, "{\"requestType\":\"testOp\",\"payload\":{}}");

    assertThat(handler.called).isTrue();
    assertThat(handler.lastClient).isSameAs(client);
  }

  @Test
  void handleMessage_unknownOpCode_logsWarningAndDoesNotThrow() {
    TestHandler handler = new TestHandler();
    GameClientMessageHandler messageHandler = new GameClientMessageHandler(List.of(handler));
    GameClient client = stubClient();

    // Should not throw; unknown opcodes are logged and discarded.
    messageHandler.handleMessage(client, "{\"requestType\":\"unknownOp\",\"payload\":{}}");

    assertThat(handler.called).isFalse();
  }

  @Test
  void handleMessage_malformedJson_stopsClient() throws Exception {
    TestHandler handler = new TestHandler();
    GameClientMessageHandler messageHandler = new GameClientMessageHandler(List.of(handler));

    WebSocketSession session = mock(WebSocketSession.class);
    when(session.getId()).thenReturn("s-1");
    when(session.isOpen()).thenReturn(true);
    GameClient client = new GameClient("s-1", session);

    messageHandler.handleMessage(client, "not-json");

    // stop() should close the session when JSON is unparseable.
    verify(session).close();
  }

  // assertThat import: use standard JUnit5 / AssertJ — included via spring-boot-starter-test
  private static <T> org.assertj.core.api.AbstractBooleanAssert<?> assertThat(boolean actual) {
    return org.assertj.core.api.Assertions.assertThat(actual);
  }

  private static <T> org.assertj.core.api.AbstractObjectAssert<?, T> assertThat(T actual) {
    return org.assertj.core.api.Assertions.assertThat(actual);
  }
}

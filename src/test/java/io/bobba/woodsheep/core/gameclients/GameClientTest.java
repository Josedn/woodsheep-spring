package io.bobba.woodsheep.core.gameclients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;
import io.bobba.woodsheep.core.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.core.JacksonException;

class GameClientTest {

  private WebSocketSession session;
  private GameClient client;

  @BeforeEach
  void setUp() {
    session = mock(WebSocketSession.class);
    when(session.getId()).thenReturn("session-1");
    client = new GameClient("session-1", session);
  }

  @Test
  void sendMessage_openSession_writesJson() throws Exception {
    when(session.isOpen()).thenReturn(true);
    OutgoingMessage message = stubMessage("ping", "{\"requestType\":\"ping\",\"payload\":null}");

    client.sendMessage(message);

    verify(session).sendMessage(any(TextMessage.class));
  }

  @Test
  void sendMessage_closedSession_callsStop() throws Exception {
    when(session.isOpen()).thenReturn(false);
    OutgoingMessage message = stubMessage("ping", "{\"requestType\":\"ping\",\"payload\":null}");

    client.sendMessage(message);

    verify(session, never()).sendMessage(any());
    // stop() should attempt to close (even though already closed — guard inside stop())
    verify(session, atLeastOnce()).isOpen();
  }

  @Test
  void setUser_attachesUserAndSendsLoginOk() throws Exception {
    when(session.isOpen()).thenReturn(true);
    User user = new User("user-1");
    user.setUsername("Alice");

    client.setUser(user);

    assertThat(client.getUser()).isSameAs(user);
    // LoginOkEventComposer is sent automatically
    verify(session).sendMessage(
        argThat(msg -> msg instanceof TextMessage tm && tm.getPayload().contains("loginOk")));
  }

  @Test
  void stop_openSession_closesSessionAndNotifiesUser() throws Exception {
    when(session.isOpen()).thenReturn(true);
    User user = mock(User.class);
    client.setUser(user); // triggers sendMessage; isOpen must be true
    when(session.isOpen()).thenReturn(true);

    client.stop();

    verify(session).close();
    verify(user).onStop();
  }

  @Test
  void stop_alreadyClosedSession_doesNotThrow() throws Exception {
    when(session.isOpen()).thenReturn(false);

    client.stop(); // should not throw

    verify(session, never()).close();
  }

  // Builds a minimal OutgoingMessage stub whose stringify() returns the provided json.
  private OutgoingMessage stubMessage(String requestType, String json) throws JacksonException {
    OutgoingMessage msg = mock(OutgoingMessage.class);
    when(msg.stringify()).thenReturn(json);
    return msg;
  }
}

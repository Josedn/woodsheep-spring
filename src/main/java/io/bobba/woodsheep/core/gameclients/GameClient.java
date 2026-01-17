package io.bobba.woodsheep.core.gameclients;

import io.bobba.woodsheep.core.communication.outgoing.user.LoginOkEventComposer;
import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;
import io.bobba.woodsheep.core.users.User;
import java.io.IOException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@RequiredArgsConstructor
@Data
public class GameClient {

  private final String id;
  private final WebSocketSession session;
  private User user;

  public void sendMessage(OutgoingMessage message) {
    if (this.session.isOpen()) {
      try {
        final String json = message.stringify();
        log.trace("Sent: {}", json);
        this.session.sendMessage(new TextMessage(json));
      } catch (com.fasterxml.jackson.core.JacksonException e) {
        log.warn("Error converting to JSON", e);
      } catch (IOException e) {
        log.warn("Error sending message", e);
      }
    } else {
      this.stop();
    }
  }

  public void setUser(User user) {
    this.user = user;
    this.sendMessage(new LoginOkEventComposer(user));
  }

  public void stop() {
    log.debug("Stopping session {}", id);

    if (this.session.isOpen()) {
      try {
        this.session.close();
      } catch (IOException e) {
        log.warn("Error closing session", e);
      }
    }
    if (this.user != null) {
      this.user.onStop();
    }
    // TODO: close everything else
  }
}

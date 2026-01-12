package io.bobba.woodsheep.core.gameclients;

import io.bobba.woodsheep.core.communication.outgoing.OutgoingMessage;
import io.bobba.woodsheep.core.communication.outgoing.user.LoginOkEventComposer;
import io.bobba.woodsheep.core.users.User;
import java.io.IOException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.core.JacksonException;

@Slf4j
@RequiredArgsConstructor
@Data
public class GameClient {

  private final String id;
  private final WebSocketSession session;
  private User user;

  private void sendMessage(OutgoingMessage message) {
    if (this.session.isOpen()) {
      try {
        this.session.sendMessage(new TextMessage(message.stringify()));
      } catch (IOException e) {
        log.warn("Error sending message", e);
      } catch (JacksonException e) {
        log.warn("Error converting to JSON", e);
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
    // TODO: close everything else
  }
}

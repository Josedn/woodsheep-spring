package io.bobba.woodsheep.core.gameclients;

import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@AllArgsConstructor
public class GameClient {
  private final String id;
  private final WebSocketSession session;

  private void sendMessage(String message) throws IOException {
    this.session.sendMessage(new TextMessage(message));
  }
}

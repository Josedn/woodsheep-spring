package io.bobba.woodsheep.core.gameclients;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@RequiredArgsConstructor
@Component
public class GameClientRegistry {
  private final Map<String, GameClient> sessions = new ConcurrentHashMap<>();
  private final GameClientMessageHandler gameClientMessageHandler;

  public void add(WebSocketSession session) {
    this.sessions.put(session.getId(), new GameClient(session.getId(), session));
  }

  public void remove(WebSocketSession session) {
    final GameClient gameClient = this.sessions.get(session.getId());
    gameClient.stop();
    this.sessions.remove(session.getId());
  }

  public void handleMessage(WebSocketSession session, String message) {
    final GameClient gameClient = this.sessions.get(session.getId());
    this.gameClientMessageHandler.handleMessage(gameClient, message);
  }
}

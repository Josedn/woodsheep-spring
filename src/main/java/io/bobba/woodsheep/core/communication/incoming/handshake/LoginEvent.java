package io.bobba.woodsheep.core.communication.incoming.handshake;

import io.bobba.woodsheep.core.communication.IncomingEvent;
import io.bobba.woodsheep.core.gameclients.GameClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoginEvent implements IncomingEvent {
  record LoginEventMessage(String ssoTicket) {}

  @Override
  public void handle(GameClient client, Object payload) {
    // final LoginEventMessage parsed = (LoginEventMessage) payload;
    log.info("Logged in {}", payload.toString());
  }

  @Override
  public String getOpCode() {
    return "login";
  }
}

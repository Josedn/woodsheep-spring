package io.bobba.woodsheep.core.communication.incoming.handshake;

import io.bobba.woodsheep.core.communication.incoming.handshake.LoginEventHandler.LoginEventMessage;
import io.bobba.woodsheep.core.communication.protocol.IncomingEventHandler;
import io.bobba.woodsheep.core.communication.protocol.OpCode;
import io.bobba.woodsheep.core.gameclients.GameClient;
import io.bobba.woodsheep.core.users.UserManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@OpCode(io.bobba.woodsheep.core.communication.protocol.Op.LOGIN)
@RequiredArgsConstructor
public class LoginEventHandler implements IncomingEventHandler<LoginEventMessage> {
  private final UserManager userManager;

  public record LoginEventMessage(String sso) {}

  @Override
  public void handle(GameClient session, LoginEventMessage payload) {
    final String sso = payload.sso();
    this.userManager.tryLogin(session, sso);
  }

  @Override
  public Class<LoginEventMessage> payloadType() {
    return LoginEventMessage.class;
  }
}

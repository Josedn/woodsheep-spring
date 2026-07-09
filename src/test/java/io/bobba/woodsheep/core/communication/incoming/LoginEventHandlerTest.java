package io.bobba.woodsheep.core.communication.incoming;

import static org.mockito.Mockito.*;

import io.bobba.woodsheep.core.communication.incoming.handshake.LoginEventHandler;
import io.bobba.woodsheep.core.communication.incoming.handshake.LoginEventHandler.LoginEventMessage;
import io.bobba.woodsheep.core.gameclients.GameClient;
import io.bobba.woodsheep.core.users.UserManager;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class LoginEventHandlerTest {

  @Test
  void handle_delegatesToUserManager() {
    UserManager userManager = mock(UserManager.class);
    LoginEventHandler handler = new LoginEventHandler(userManager);
    WebSocketSession ws = mock(WebSocketSession.class);
    when(ws.getId()).thenReturn("s-1");
    when(ws.isOpen()).thenReturn(false);
    GameClient client = new GameClient("s-1", ws);

    handler.handle(client, new LoginEventMessage("my-sso-token"));

    verify(userManager).tryLogin(client, "my-sso-token");
  }
}

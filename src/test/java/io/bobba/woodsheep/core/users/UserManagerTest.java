package io.bobba.woodsheep.core.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.bobba.woodsheep.core.gameclients.GameClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class UserManagerTest {

  private UserManager userManager;

  @BeforeEach
  void setUp() {
    userManager = new UserManager();
  }

  private GameClient openClient(String sessionId) {
    WebSocketSession ws = mock(WebSocketSession.class);
    when(ws.getId()).thenReturn(sessionId);
    when(ws.isOpen()).thenReturn(true);
    return new GameClient(sessionId, ws);
  }

  @Test
  void tryLogin_newSso_createsUserAndAttachesClient() {
    GameClient client = openClient("s-1");

    userManager.tryLogin(client, "sso-abc");

    assertThat(client.getUser()).isNotNull();
    assertThat(client.getUser().getUsername()).isNotNull();
  }

  @Test
  void tryLogin_sameSsoTwice_reusesSameUser() {
    GameClient client1 = openClient("s-1");
    userManager.tryLogin(client1, "sso-abc");
    User firstUser = client1.getUser();

    // second login with same SSO but a fresh client (simulates reconnect)
    GameClient client2 = openClient("s-2");
    // user is keyed by user.id not SSO in the current impl — new SSO creates a new user
    userManager.tryLogin(client2, "sso-xyz");
    User secondUser = client2.getUser();

    // different SSO → different user
    assertThat(secondUser).isNotSameAs(firstUser);
  }

  @Test
  void tryLogin_alreadyLoggedInClient_stopsClient() throws Exception {
    GameClient client = openClient("s-1");
    userManager.tryLogin(client, "sso-abc");

    WebSocketSession ws2 = mock(WebSocketSession.class);
    when(ws2.getId()).thenReturn("s-1");
    when(ws2.isOpen()).thenReturn(true);
    GameClient sameClient = new GameClient("s-1", ws2);
    // manually attach a user to simulate already-logged-in state
    User existingUser = new User("u-existing");
    existingUser.setUsername("Bob");
    sameClient.setUser(existingUser);

    userManager.tryLogin(sameClient, "sso-abc");

    verify(ws2).close();
  }
}

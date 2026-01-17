package io.bobba.woodsheep.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.bobba.woodsheep.core.communication.incoming.handshake.LoginEventHandler;
import io.bobba.woodsheep.core.communication.incoming.room.CreateRoomEventHandler;
import io.bobba.woodsheep.core.communication.incoming.room.GetRoomListEventHandler;
import io.bobba.woodsheep.core.communication.incoming.room.JoinRoomEventHandler;
import io.bobba.woodsheep.core.communication.incoming.room.SendChatMessageHandler;
import io.bobba.woodsheep.core.gameclients.GameClient;
import io.bobba.woodsheep.core.gameclients.GameClientMessageHandler;
import io.bobba.woodsheep.core.rooms.RoomManager;
import io.bobba.woodsheep.core.users.UserManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class GameClientMessageHandlerTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private UserManager userManager;
  private RoomManager roomManager;
  private GameClientMessageHandler handler;

  @BeforeEach
  void setup() {
    userManager = new UserManager();
    roomManager = new RoomManager();
    handler =
        new GameClientMessageHandler(
            new ObjectMapper(),
            List.of(
                new LoginEventHandler(userManager),
                new CreateRoomEventHandler(roomManager),
                new JoinRoomEventHandler(roomManager),
                new GetRoomListEventHandler(roomManager),
                new SendChatMessageHandler()));
  }

  @Test
  void loginSetsUserAndSendsLoginOk() throws Exception {
    WebSocketSession session = mock(WebSocketSession.class);
    when(session.isOpen()).thenReturn(true);
    GameClient client = new GameClient("c1", session);

    handler.handleMessage(client, "{\"requestType\":\"login\",\"payload\":{\"sso\":\"abc\"}}");

    assertNotNull(client.getUser(), "User should be set on client after login");

    ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
    verify(session, atLeastOnce()).sendMessage(captor.capture());
    boolean sentLoginOk =
        captor.getAllValues().stream()
            .map(TextMessage::getPayload)
            .map(this::asJson)
            .anyMatch(n -> n.path("requestType").asText().equals("loginOk"));
    assertTrue(sentLoginOk, "Should send loginOk message");
  }

  @Test
  void createRoomSendsPrepareRoomAndAddUser() throws Exception {
    WebSocketSession session = mock(WebSocketSession.class);
    when(session.isOpen()).thenReturn(true);
    GameClient client = new GameClient("c1", session);

    // login first
    handler.handleMessage(client, "{\"requestType\":\"login\",\"payload\":{\"sso\":\"abc\"}}");
    clearInvocations(session);

    handler.handleMessage(client, "{\"requestType\":\"createRoom\",\"payload\":{}}");

    ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
    verify(session, atLeastOnce()).sendMessage(captor.capture());
    var messages = captor.getAllValues();

    boolean sentPrepareRoom =
        messages.stream()
            .map(TextMessage::getPayload)
            .map(this::asJson)
            .anyMatch(n -> n.path("requestType").asText().equals("prepareRoom"));
    boolean sentAddUser =
        messages.stream()
            .map(TextMessage::getPayload)
            .map(this::asJson)
            .anyMatch(n -> n.path("requestType").asText().equals("addUserToRoom"));

    assertTrue(sentPrepareRoom, "Should send prepareRoom message");
    assertTrue(sentAddUser, "Should send addUserToRoom message");
  }

  @Test
  void chatWithoutRoomDoesNothingAndDoesNotThrow() throws Exception {
    WebSocketSession session = mock(WebSocketSession.class);
    when(session.isOpen()).thenReturn(true);
    GameClient client = new GameClient("c1", session);

    // login only, do not join/create a room
    handler.handleMessage(client, "{\"requestType\":\"login\",\"payload\":{\"sso\":\"abc\"}}");
    clearInvocations(session);

    // send chat message with no current room
    assertDoesNotThrow(
        () ->
            handler.handleMessage(
                client, "{\"requestType\":\"chatMessage\",\"payload\":{\"message\":\"hi\"}}"));

    // should not send any messages as a result
    verify(session, never()).sendMessage(any(TextMessage.class));
  }

  @Test
  void joinRoomMovesUserAndSendsPrepAndAdd() throws Exception {
    // Client 1
    WebSocketSession s1 = mock(WebSocketSession.class);
    when(s1.isOpen()).thenReturn(true);
    GameClient c1 = new GameClient("c1", s1);
    handler.handleMessage(c1, "{\"requestType\":\"login\",\"payload\":{\"sso\":\"u1\"}}");
    handler.handleMessage(c1, "{\"requestType\":\"createRoom\",\"payload\":{}}");

    // Client 2 creates another room to join
    WebSocketSession s2 = mock(WebSocketSession.class);
    when(s2.isOpen()).thenReturn(true);
    GameClient c2 = new GameClient("c2", s2);
    handler.handleMessage(c2, "{\"requestType\":\"login\",\"payload\":{\"sso\":\"u2\"}}");
    clearInvocations(s2);
    handler.handleMessage(c2, "{\"requestType\":\"createRoom\",\"payload\":{}}");

    // Capture roomId from client2's prepareRoom
    ArgumentCaptor<TextMessage> cap2 = ArgumentCaptor.forClass(TextMessage.class);
    verify(s2, atLeastOnce()).sendMessage(cap2.capture());
    String roomId =
        cap2.getAllValues().stream()
            .map(TextMessage::getPayload)
            .map(this::asJson)
            .filter(n -> n.path("requestType").asText().equals("prepareRoom"))
            .map(n -> n.path("payload").path("roomId").asText())
            .findFirst()
            .orElseThrow();

    clearInvocations(s1);
    handler.handleMessage(
        c1, "{\"requestType\":\"joinRoom\",\"payload\":{\"roomId\":\"" + roomId + "\"}}");

    ArgumentCaptor<TextMessage> cap1 = ArgumentCaptor.forClass(TextMessage.class);
    verify(s1, atLeastOnce()).sendMessage(cap1.capture());
    var msgs = cap1.getAllValues();
    boolean sentPrepare =
        msgs.stream()
            .map(TextMessage::getPayload)
            .map(this::asJson)
            .anyMatch(n -> n.path("requestType").asText().equals("prepareRoom"));
    boolean sentAdd =
        msgs.stream()
            .map(TextMessage::getPayload)
            .map(this::asJson)
            .anyMatch(n -> n.path("requestType").asText().equals("addUserToRoom"));

    assertTrue(sentPrepare, "Should send prepareRoom on join");
    assertTrue(sentAdd, "Should send addUserToRoom on join");
  }

  @Test
  void roomListSendsListAndLeavesRoom() throws Exception {
    WebSocketSession s = mock(WebSocketSession.class);
    when(s.isOpen()).thenReturn(true);
    GameClient c = new GameClient("c", s);

    handler.handleMessage(c, "{\"requestType\":\"login\",\"payload\":{\"sso\":\"u\"}}");
    handler.handleMessage(c, "{\"requestType\":\"createRoom\",\"payload\":{}}");
    assertNotNull(c.getUser().getCurrentRoom(), "User should be in a room after createRoom");
    clearInvocations(s);

    handler.handleMessage(c, "{\"requestType\":\"roomList\",\"payload\":{}}");

    ArgumentCaptor<TextMessage> cap = ArgumentCaptor.forClass(TextMessage.class);
    verify(s, atLeastOnce()).sendMessage(cap.capture());
    boolean sentList =
        cap.getAllValues().stream()
            .map(TextMessage::getPayload)
            .map(this::asJson)
            .anyMatch(n -> n.path("requestType").asText().equals("roomList"));
    assertTrue(sentList, "Should send roomList");
    assertNull(c.getUser().getCurrentRoom(), "User should leave room after requesting roomList");
  }

  private JsonNode asJson(String s) {
    try {
      return mapper.readTree(s);
    } catch (Exception e) {
      throw new AssertionError("Invalid JSON: " + s, e);
    }
  }
}

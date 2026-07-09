package io.bobba.woodsheep.core.rooms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.bobba.woodsheep.core.gameclients.GameClient;
import io.bobba.woodsheep.core.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class RoomManagerTest {

  private RoomManager roomManager;

  @BeforeEach
  void setUp() {
    roomManager = new RoomManager();
  }

  private User makeUser(String id) throws Exception {
    WebSocketSession ws = mock(WebSocketSession.class);
    when(ws.getId()).thenReturn(id);
    when(ws.isOpen()).thenReturn(true);
    GameClient client = new GameClient(id, ws);
    User user = new User(id);
    user.setUsername("User-" + id);
    user.setSession(client);
    client.setUser(user);
    return user;
  }

  @Test
  void createRoom_addsUserToNewRoom() throws Exception {
    User user = makeUser("u-1");

    roomManager.createRoom(user);

    assertThat(user.getCurrentRoom()).isNotNull();
  }

  @Test
  void prepareRoomForUser_validRoom_addsUser() throws Exception {
    User owner = makeUser("u-owner");
    roomManager.createRoom(owner);
    String roomId = owner.getCurrentRoom().getId();

    User joiner = makeUser("u-joiner");
    roomManager.prepareRoomForUser(joiner, roomId);

    assertThat(joiner.getCurrentRoom()).isNotNull();
    assertThat(joiner.getCurrentRoom().getId()).isEqualTo(roomId);
  }

  @Test
  void prepareRoomForUser_invalidRoom_sendsRoomRejected() throws Exception {
    User user = makeUser("u-1");
    WebSocketSession ws = (WebSocketSession) user.getSession().getSession();
    clearInvocations(ws);

    roomManager.prepareRoomForUser(user, "nonexistent-room-id");

    verify(ws)
        .sendMessage(
            argThat(m -> m instanceof TextMessage tm && tm.getPayload().contains("roomRejected")));
    assertThat(user.getCurrentRoom()).isNull();
  }

  @Test
  void prepareRoomForUser_sameRoomAlreadyIn_resendRoomInfo() throws Exception {
    User user = makeUser("u-1");
    roomManager.createRoom(user);
    String roomId = user.getCurrentRoom().getId();
    WebSocketSession ws = (WebSocketSession) user.getSession().getSession();
    clearInvocations(ws);

    roomManager.prepareRoomForUser(user, roomId);

    // serializeRoomInfo sends RoomInfoComposer + AddUserToRoomComposer
    verify(ws, atLeast(2)).sendMessage(any(TextMessage.class));
    assertThat(user.getCurrentRoom().getId()).isEqualTo(roomId);
  }

  @Test
  void prepareRoomForUser_switchRoom_leavesOldRoom() throws Exception {
    User user = makeUser("u-1");
    roomManager.createRoom(user);
    Room firstRoom = user.getCurrentRoom();

    User owner2 = makeUser("u-owner2");
    roomManager.createRoom(owner2);
    String secondRoomId = owner2.getCurrentRoom().getId();

    roomManager.prepareRoomForUser(user, secondRoomId);

    assertThat(user.getCurrentRoom().getId()).isEqualTo(secondRoomId);
    assertThat(firstRoom.getUnSyncUsers()).noneMatch(ru -> ru.getUser() == user);
  }

  @Test
  void sendRoomList_sendsRoomListMessage() throws Exception {
    User user = makeUser("u-1");
    WebSocketSession ws = (WebSocketSession) user.getSession().getSession();
    clearInvocations(ws);

    roomManager.sendRoomList(user);

    verify(ws)
        .sendMessage(
            argThat(m -> m instanceof TextMessage tm && tm.getPayload().contains("roomList")));
  }
}

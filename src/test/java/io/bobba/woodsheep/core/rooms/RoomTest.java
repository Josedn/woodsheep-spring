package io.bobba.woodsheep.core.rooms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.bobba.catanatron.enums.Color;
import io.bobba.woodsheep.core.gameclients.GameClient;
import io.bobba.woodsheep.core.users.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class RoomTest {

  private Room room;

  private User makeUser(String id) throws Exception {
    WebSocketSession ws = mock(WebSocketSession.class);
    when(ws.getId()).thenReturn(id);
    when(ws.isOpen()).thenReturn(true);
    GameClient client = new GameClient(id, ws);
    User user = new User(id);
    user.setUsername("User-" + id);
    // attach client without triggering LoginOkEventComposer side-effects in assertions
    user.setSession(client);
    client.setUser(user); // sends loginOk — session is open, so ws.sendMessage is called here
    return user;
  }

  @BeforeEach
  void setUp() {
    room = new Room("room-1");
  }

  @Test
  void addUserToRoom_setsCurrentRoomOnUser() throws Exception {
    User user = makeUser("u-1");

    room.addUserToRoom(user);

    assertThat(user.getCurrentRoom()).isSameAs(room);
  }

  @Test
  void addUserToRoom_broadcastsAddMessage() throws Exception {
    User user = makeUser("u-1");
    WebSocketSession ws = (WebSocketSession) user.getSession().getSession();

    room.addUserToRoom(user);

    // addUserToRoom broadcasts AddUserToRoomComposer to existing members, then calls
    // serializeRoomInfo which sends RoomInfoComposer + AddUserToRoomComposer to the new user.
    verify(ws, atLeast(2)).sendMessage(any(TextMessage.class));
  }

  @Test
  void removeUserFromRoom_clearsCurrentRoom() throws Exception {
    User user = makeUser("u-1");
    room.addUserToRoom(user);

    room.removeUserFromRoom(user);

    assertThat(user.getCurrentRoom()).isNull();
    assertThat(room.getUnSyncUsers()).isEmpty();
  }

  @Test
  void removeUserFromRoom_userNotInRoom_doesNothing() throws Exception {
    User outsider = makeUser("u-outsider");
    // not added to room
    room.removeUserFromRoom(outsider); // should not throw
  }

  @Test
  void handleChatMessage_broadcastsToChatMembers() throws Exception {
    User sender = makeUser("u-1");
    User receiver = makeUser("u-2");
    room.addUserToRoom(sender);
    room.addUserToRoom(receiver);

    WebSocketSession senderWs = (WebSocketSession) sender.getSession().getSession();
    WebSocketSession receiverWs = (WebSocketSession) receiver.getSession().getSession();
    clearInvocations(senderWs, receiverWs);

    room.handleChatMessage(sender, "hello");

    verify(senderWs)
        .sendMessage(
            argThat(m -> m instanceof TextMessage tm && tm.getPayload().contains("chatMessage")));
    verify(receiverWs)
        .sendMessage(
            argThat(m -> m instanceof TextMessage tm && tm.getPayload().contains("chatMessage")));
  }

  @Test
  void handleStartGame_transitionsToInGame() throws Exception {
    User user = makeUser("u-1");
    room.addUserToRoom(user);

    room.handleStartGame(user);

    assertThat(room.getRoomState()).isEqualTo(RoomState.IN_GAME);
    assertThat(room.getGame()).isNotNull();
  }

  @Test
  void handleStartGame_calledTwice_onlyStartsOnce() throws Exception {
    User user = makeUser("u-1");
    room.addUserToRoom(user);

    room.handleStartGame(user);
    room.handleStartGame(user);

    assertThat(room.getRoomState()).isEqualTo(RoomState.IN_GAME);
  }

  @Test
  void addUserToRoom_eachUserGetsDistinctColor() throws Exception {
    List<User> users = List.of(makeUser("u-1"), makeUser("u-2"), makeUser("u-3"), makeUser("u-4"));
    for (User u : users) {
      room.addUserToRoom(u);
    }

    List<Color> colors = room.getUnSyncUsers().stream().map(RoomUser::getColor).toList();
    assertThat(colors).doesNotHaveDuplicates();
    assertThat(colors).containsExactlyInAnyOrder(Color.values());
  }

  @Test
  void addUserToRoom_roomFull_sendsRoomRejected() throws Exception {
    for (int i = 1; i <= 4; i++) {
      room.addUserToRoom(makeUser("u-" + i));
    }
    User fifth = makeUser("u-5");
    WebSocketSession fifthWs = (WebSocketSession) fifth.getSession().getSession();
    clearInvocations(fifthWs);

    room.addUserToRoom(fifth);

    verify(fifthWs)
        .sendMessage(
            argThat(m -> m instanceof TextMessage tm && tm.getPayload().contains("roomRejected")));
    assertThat(fifth.getCurrentRoom()).isNull();
    assertThat(room.getUnSyncUsers()).hasSize(4);
  }

  @Test
  void generateGameStateMessage_waitingRoom_emptyTiles() throws Exception {
    var msg = room.generateGameStateMessage();
    String json = msg.stringify();

    assertThat(json).contains("WAITING");
    assertThat(json).contains("\"tiles\":[]");
  }

  @Test
  void generateGameStateMessage_inGame_includesTiles() throws Exception {
    User user = makeUser("u-1");
    room.addUserToRoom(user);
    room.handleStartGame(user);

    String json = room.generateGameStateMessage().stringify();

    assertThat(json).contains("IN_GAME");
    assertThat(json).doesNotContain("\"tiles\":[]");
  }
}

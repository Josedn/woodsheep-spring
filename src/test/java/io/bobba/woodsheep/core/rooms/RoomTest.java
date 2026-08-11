package io.bobba.woodsheep.core.rooms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.catanatron.core.enums.ActionPrompt;
import com.catanatron.core.enums.Color;
import com.catanatron.core.game.Game;
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

  @Test
  void handleRoll_beforeInitialBuildPhaseComplete_isRejectedAndNoRollRecorded() throws Exception {
    User user = makeUser("u-1");
    room.addUserToRoom(user);
    room.handleStartGame(user);

    room.handleRoll(user);

    assertThat(room.getLastRoll()).isNull();
  }

  @Test
  void handleRoll_afterInitialBuildPhase_rollsAndMarksPlayerAsRolled() throws Exception {
    User user = makeUser("u-1");
    room.addUserToRoom(user);
    room.handleStartGame(user);
    advanceToPlayTurn(room.getGame());
    Color color = room.getUnSyncUsers().get(0).getColor();

    room.handleRoll(user);

    assertThat(room.getLastRoll()).isNotNull();
    assertThat(room.getGame().state.playerState(color).hasRolled).isTrue();
  }

  @Test
  void handleEndTurn_beforeRolling_isRejected() throws Exception {
    User user = makeUser("u-1");
    room.addUserToRoom(user);
    room.handleStartGame(user);
    advanceToPlayTurn(room.getGame());
    int turnsBefore = room.getGame().state.numTurns;

    room.handleEndTurn(user);

    assertThat(room.getGame().state.numTurns).isEqualTo(turnsBefore);
  }

  @Test
  void handleRoll_byUserNotInRoom_doesNothing() throws Exception {
    User owner = makeUser("u-owner");
    User outsider = makeUser("u-outsider");
    room.addUserToRoom(owner);
    room.handleStartGame(owner);
    advanceToPlayTurn(room.getGame());

    room.handleRoll(outsider);

    assertThat(room.getLastRoll()).isNull();
  }

  @Test
  void generateGameStateMessage_includesOwnHandButHidesOthersExactCards() throws Exception {
    User a = makeUser("u-a");
    User b = makeUser("u-b");
    room.addUserToRoom(a);
    room.addUserToRoom(b);
    room.handleStartGame(a);
    Color colorA =
        room.getUnSyncUsers().stream().filter(ru -> ru.getUser() == a).findFirst().get().getColor();

    String json = room.generateGameStateMessage(colorA).stringify();

    assertThat(json).contains("\"yourColor\":\"" + colorA + "\"");
    assertThat(json).contains("\"yourHand\":{");
    assertThat(json).doesNotContain("resourcesInHand");
  }

  /** Fast-forwards the initial build phase by always taking the first playable action. */
  private void advanceToPlayTurn(Game game) {
    int guard = 0;
    while (game.state.currentPrompt != ActionPrompt.PLAY_TURN) {
      game.execute(game.playableActions.get(0), true, null);
      if (++guard > 1000) {
        throw new IllegalStateException("Failed to reach PLAY_TURN within guard limit");
      }
    }
  }
}

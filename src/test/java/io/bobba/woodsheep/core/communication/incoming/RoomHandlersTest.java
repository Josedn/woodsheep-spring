package io.bobba.woodsheep.core.communication.incoming;

import static org.mockito.Mockito.*;

import io.bobba.woodsheep.core.communication.incoming.room.*;
import io.bobba.woodsheep.core.communication.incoming.room.JoinRoomEventHandler.JoinRoomEventMessage;
import io.bobba.woodsheep.core.communication.incoming.room.SendChatMessageHandler.SendChatMessageMessage;
import io.bobba.woodsheep.core.communication.protocol.EmptyObjectPayload;
import io.bobba.woodsheep.core.gameclients.GameClient;
import io.bobba.woodsheep.core.rooms.Room;
import io.bobba.woodsheep.core.rooms.RoomManager;
import io.bobba.woodsheep.core.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class RoomHandlersTest {

  private GameClient unauthenticated;
  private GameClient authenticated;
  private User user;
  private Room room;

  @BeforeEach
  void setUp() {
    WebSocketSession ws = mock(WebSocketSession.class);
    when(ws.getId()).thenReturn("s-unauth");
    when(ws.isOpen()).thenReturn(false);
    unauthenticated = new GameClient("s-unauth", ws);

    WebSocketSession ws2 = mock(WebSocketSession.class);
    when(ws2.getId()).thenReturn("s-auth");
    when(ws2.isOpen()).thenReturn(false);
    authenticated = new GameClient("s-auth", ws2);

    user = mock(User.class);
    room = mock(Room.class);
    // Wire user into the authenticated client without triggering LoginOkEventComposer
    authenticated.setUser(user);
    when(user.getCurrentRoom()).thenReturn(room);
  }

  // ── CreateRoomEventHandler ────────────────────────────────────────────────

  @Test
  void createRoom_withUser_delegatesToRoomManager() {
    RoomManager roomManager = mock(RoomManager.class);
    CreateRoomEventHandler handler = new CreateRoomEventHandler(roomManager);

    handler.handle(authenticated, new EmptyObjectPayload());

    verify(roomManager).createRoom(user);
  }

  @Test
  void createRoom_noUser_doesNothing() {
    RoomManager roomManager = mock(RoomManager.class);
    CreateRoomEventHandler handler = new CreateRoomEventHandler(roomManager);

    handler.handle(unauthenticated, new EmptyObjectPayload());

    verifyNoInteractions(roomManager);
  }

  // ── GetRoomListEventHandler ───────────────────────────────────────────────

  @Test
  void getRoomList_withUser_sendsListAndLeavesRoom() {
    RoomManager roomManager = mock(RoomManager.class);
    GetRoomListEventHandler handler = new GetRoomListEventHandler(roomManager);

    handler.handle(authenticated, new EmptyObjectPayload());

    verify(roomManager).sendRoomList(user);
    verify(user).leaveRoom();
  }

  @Test
  void getRoomList_noUser_doesNothing() {
    RoomManager roomManager = mock(RoomManager.class);
    GetRoomListEventHandler handler = new GetRoomListEventHandler(roomManager);

    handler.handle(unauthenticated, new EmptyObjectPayload());

    verifyNoInteractions(roomManager);
  }

  // ── JoinRoomEventHandler ──────────────────────────────────────────────────

  @Test
  void joinRoom_withUser_delegatesToRoomManager() {
    RoomManager roomManager = mock(RoomManager.class);
    JoinRoomEventHandler handler = new JoinRoomEventHandler(roomManager);

    handler.handle(authenticated, new JoinRoomEventMessage("room-42"));

    verify(roomManager).prepareRoomForUser(user, "room-42");
  }

  @Test
  void joinRoom_noUser_doesNothing() {
    RoomManager roomManager = mock(RoomManager.class);
    JoinRoomEventHandler handler = new JoinRoomEventHandler(roomManager);

    handler.handle(unauthenticated, new JoinRoomEventMessage("room-42"));

    verifyNoInteractions(roomManager);
  }

  // ── SendChatMessageHandler ────────────────────────────────────────────────

  @Test
  void chatMessage_withUserAndRoom_delegatesToRoom() {
    SendChatMessageHandler handler = new SendChatMessageHandler();

    handler.handle(authenticated, new SendChatMessageMessage("hello world"));

    verify(room).handleChatMessage(user, "hello world");
  }

  @Test
  void chatMessage_noUser_doesNothing() {
    SendChatMessageHandler handler = new SendChatMessageHandler();

    handler.handle(unauthenticated, new SendChatMessageMessage("hello"));

    verifyNoInteractions(room);
  }

  @Test
  void chatMessage_noRoom_doesNothing() {
    when(user.getCurrentRoom()).thenReturn(null);
    SendChatMessageHandler handler = new SendChatMessageHandler();

    handler.handle(authenticated, new SendChatMessageMessage("hello"));

    verify(room, never()).handleChatMessage(any(), any());
  }

  // ── StartGameHandler ──────────────────────────────────────────────────────

  @Test
  void startGame_withUserAndRoom_delegatesToRoom() {
    StartGameHandler handler = new StartGameHandler();

    handler.handle(authenticated, new EmptyObjectPayload());

    verify(room).handleStartGame(user);
  }

  @Test
  void startGame_noUser_doesNothing() {
    StartGameHandler handler = new StartGameHandler();

    handler.handle(unauthenticated, new EmptyObjectPayload());

    verifyNoInteractions(room);
  }

  @Test
  void startGame_noRoom_doesNothing() {
    when(user.getCurrentRoom()).thenReturn(null);
    StartGameHandler handler = new StartGameHandler();

    handler.handle(authenticated, new EmptyObjectPayload());

    verify(room, never()).handleStartGame(any());
  }
}

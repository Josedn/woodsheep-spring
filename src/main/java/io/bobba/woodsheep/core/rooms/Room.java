package io.bobba.woodsheep.core.rooms;

import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.enums.Resource;
import io.bobba.catanatron.game.Game;
import io.bobba.woodsheep.core.communication.outgoing.room.AddUserToRoomComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.ChatMessageComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.GameStateComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.RemoveUserFromRoomComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.RoomInfoComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.RoomRejectedComposer;
import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;
import io.bobba.woodsheep.core.users.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Getter
public class Room {

  private static final int MAX_PLAYERS = 4;
  private final String id;
  private final Map<Integer, RoomUser> users = new ConcurrentHashMap<>();
  private int userCounter = 0;
  private RoomState roomState = RoomState.WAITING;
  private Game game;
  private static final Random RNG = new Random();

  public OutgoingMessage generateGameStateMessage() {
    List<GameStateComposer.TilePayload> tilesState = List.of();
    if (roomState == RoomState.IN_GAME) {
      final var landTiles = game.state.board.map.landTiles;
      tilesState =
          landTiles.entrySet().stream()
              .map(
                  coordinateTileEntry -> {
                    final var tile = coordinateTileEntry.getValue();
                    final var coordinate = coordinateTileEntry.getKey();
                    return new GameStateComposer.TilePayload(
                        tile.id,
                        Optional.ofNullable(tile.resource).map(Resource::toString).orElse("DESERT"),
                        Optional.ofNullable(tile.number).orElse(0),
                        coordinate.x(),
                        coordinate.y(),
                        coordinate.z());
                  })
              .toList();
    }
    return new GameStateComposer(this.roomState.toString(), tilesState);
  }

  public void removeUserFromRoom(User user) {
    RoomUser roomUser = this.getRoomUserByUser(user);
    if (roomUser != null) {
      users.remove(roomUser.getVirtualId());
      user.onRoomLeave();
      sendMessage(new RemoveUserFromRoomComposer(roomUser.getVirtualId()));
      log.debug("User removed from room: {}", user.getUsername());
    }
  }

  private Color nextAvailableColor() {
    var takenColors =
        getUnSyncUsers().stream()
            .map(RoomUser::getColor)
            .collect(java.util.stream.Collectors.toSet());
    for (Color color : Color.values()) {
      if (!takenColors.contains(color)) {
        return color;
      }
    }
    return Color.values()[0];
  }

  public void addUserToRoom(User user) {
    if (user.getSession() == null) {
      return;
    }
    if (users.size() >= MAX_PLAYERS) {
      user.getSession().sendMessage(new RoomRejectedComposer("full"));
      return;
    }
    RoomUser roomUser = new RoomUser(this.userCounter++, user, nextAvailableColor());
    user.setCurrentRoom(this);
    this.sendMessage(new AddUserToRoomComposer(roomUser));
    this.users.put(roomUser.getVirtualId(), roomUser);
    this.serializeRoomInfo(user);
    log.debug("User added to room: {}", user.getUsername());
  }

  public void serializeRoomInfo(User user) {
    List<RoomUser> usersCopy = getUnSyncUsers();
    user.getSession().sendMessage(new RoomInfoComposer(this.id, "base", false, true, 4, 30, 7, 10));
    user.getSession().sendMessage(new AddUserToRoomComposer(usersCopy));
  }

  public void sendMessage(OutgoingMessage outgoingMessage) {
    List<RoomUser> usersCopy = getUnSyncUsers();
    for (RoomUser roomUser : usersCopy) {
      User user = roomUser.getUser();
      if (user.getSession() != null) {
        user.getSession().sendMessage(outgoingMessage);
      }
    }
  }

  private RoomUser getRoomUserByUser(User user) {
    List<RoomUser> usersCopy = getUnSyncUsers();
    for (RoomUser roomUser : usersCopy) {
      if (roomUser.getUser() == user) {
        return roomUser;
      }
    }
    return null;
  }

  public void handleChatMessage(User user, String message) {
    RoomUser roomUser = this.getRoomUserByUser(user);
    if (roomUser != null) {
      this.sendMessage(new ChatMessageComposer(roomUser.getVirtualId(), message));
    }
  }

  public List<RoomUser> getUnSyncUsers() {
    return new ArrayList<>(users.values());
  }

  public synchronized void handleStartGame(User user) {
    // TODO: Check if user is host
    if (this.game == null && this.roomState == RoomState.WAITING) {
      this.roomState = RoomState.IN_GAME;
      this.game =
          new Game(new ArrayList<>(users.values()), RNG.nextLong(), 7, false, 10, null, true);
      sendMessage(generateGameStateMessage());
    }
  }
}

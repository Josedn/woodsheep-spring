package io.bobba.woodsheep.core.rooms;

import com.catanatron.core.enums.ActionType;
import com.catanatron.core.enums.Color;
import com.catanatron.core.enums.Resource;
import com.catanatron.core.game.Game;
import com.catanatron.core.state.Action;
import com.catanatron.core.state.ActionRecord;
import com.catanatron.core.state.StateFunctions;
import io.bobba.woodsheep.core.communication.outgoing.room.AddUserToRoomComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.ChatMessageComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.GameStateComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.RemoveUserFromRoomComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.RoomInfoComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.RoomRejectedComposer;
import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;
import io.bobba.woodsheep.core.users.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
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
  private int[] lastRoll;
  private static final Random RNG = new Random();

  /** Spectator/no-viewer variant — carries no private hand info. */
  public OutgoingMessage generateGameStateMessage() {
    return generateGameStateMessage(null);
  }

  /**
   * Builds a game-state message tailored to {@code viewerColor}: the viewer's own resource hand is
   * included in full, but only public info (counts, VP, longest road/largest army) is included for
   * every player, so no one can see an opponent's exact hand.
   */
  public OutgoingMessage generateGameStateMessage(Color viewerColor) {
    List<GameStateComposer.TilePayload> tilesState = List.of();
    List<GameStateComposer.BuildingPayload> buildingsState = List.of();
    List<GameStateComposer.RoadPayload> roadsState = List.of();
    String currentColor = null;
    String currentTurnColor = null;
    String currentPrompt = null;
    List<GameStateComposer.PlayerPayload> players = List.of();
    Map<String, Integer> yourHand = null;
    List<String> playableActionTypes = List.of();

    if (roomState == RoomState.IN_GAME) {
      final var state = game.state;
      final var board = state.board;
      tilesState =
          board.map.landTiles.entrySet().stream()
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
      buildingsState =
          board.buildings.entrySet().stream()
              .map(
                  entry ->
                      new GameStateComposer.BuildingPayload(
                          entry.getKey(),
                          entry.getValue()[0].toString(),
                          board.buildingTypes.get(entry.getKey()).toString()))
              .toList();
      roadsState =
          board.roads.entrySet().stream()
              .map(
                  entry ->
                      new GameStateComposer.RoadPayload(
                          entry.getKey().a(), entry.getKey().b(), entry.getValue().toString()))
              .toList();

      currentColor = state.currentColor().toString();
      currentTurnColor = state.currentTurnColor().toString();
      currentPrompt = state.currentPrompt.toString();

      Color longestRoadColor = StateFunctions.getLongestRoadColor(state);
      Color largestArmyColor = (Color) StateFunctions.getLargestArmy(state)[0];
      players =
          Arrays.stream(state.colors)
              .map(
                  c ->
                      new GameStateComposer.PlayerPayload(
                          c.toString(),
                          StateFunctions.getVisibleVictoryPoints(state, c),
                          StateFunctions.playerNumResourceCards(state, c),
                          StateFunctions.getDevCardsInHandTotal(state, c),
                          c == longestRoadColor,
                          c == largestArmyColor))
              .toList();

      if (viewerColor != null && state.colorToIndex.containsKey(viewerColor)) {
        int[] hand = state.playerState(viewerColor).resourcesInHand;
        yourHand = new LinkedHashMap<>();
        for (Resource r : Resource.ALL) yourHand.put(r.toString(), hand[r.ordinal()]);

        if (viewerColor == state.currentColor()) {
          playableActionTypes =
              game.playableActions.stream()
                  .map(action -> action.actionType().toString())
                  .distinct()
                  .toList();
        }
      }
    }

    return new GameStateComposer(
        this.roomState.toString(),
        tilesState,
        buildingsState,
        roadsState,
        currentColor,
        currentTurnColor,
        currentPrompt,
        this.lastRoll,
        players,
        viewerColor != null ? viewerColor.toString() : null,
        yourHand,
        playableActionTypes);
  }

  /** Sends each room member their own personalized game-state view. */
  public void sendGameState() {
    for (RoomUser roomUser : getUnSyncUsers()) {
      User user = roomUser.getUser();
      if (user.getSession() != null) {
        user.getSession().sendMessage(generateGameStateMessage(roomUser.getColor()));
      }
    }
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
      sendGameState();
    }
  }

  public synchronized void handleRoll(User user) {
    performAction(user, ActionType.ROLL, null);
  }

  public synchronized void handleEndTurn(User user) {
    performAction(user, ActionType.END_TURN, null);
  }

  /**
   * Validates that {@code user} may currently play {@code actionType} (it must be their turn and a
   * legal move per the engine's own playable-actions check), applies it, and broadcasts the
   * resulting state. Does nothing if the action was illegal.
   */
  private void performAction(User user, ActionType actionType, Object value) {
    if (roomState != RoomState.IN_GAME || game == null) return;
    RoomUser roomUser = getRoomUserByUser(user);
    if (roomUser == null) return;

    Action action = new Action(roomUser.getColor(), actionType, value);
    ActionRecord record;
    try {
      record = game.execute(action, true, null);
    } catch (IllegalArgumentException e) {
      log.debug("Rejected action {} from {}: {}", action, user.getUsername(), e.getMessage());
      return;
    }
    if (actionType == ActionType.ROLL) {
      this.lastRoll = (int[]) record.result();
    }
    sendGameState();
  }
}

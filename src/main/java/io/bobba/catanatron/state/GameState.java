package io.bobba.catanatron.state;

import io.bobba.catanatron.enums.ActionPrompt;
import io.bobba.catanatron.enums.BuildingType;
import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.enums.DevCard;
import io.bobba.catanatron.models.Board;
import io.bobba.catanatron.models.CatanMap;
import io.bobba.catanatron.models.Decks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * All mutable state for a game in progress. Mirrors State in state.py.
 *
 * <p>player_state from Python is replaced by a per-Color PlayerState object, which is faster to
 * copy and easier to access than a flat string-keyed map.
 */
public class GameState {

  // Player ordering (shuffled at construction)
  public final Color[] colors;
  public final Map<Color, Integer> colorToIndex;

  // Board and decks
  public Board board;
  public int[] resourceFreqdeck;
  public List<DevCard> developmentListdeck;

  // Per-player state
  public final Map<Color, PlayerState> playerStates;

  /** node_id lists per color per building type — cache */
  public final Map<Color, Map<BuildingType, List<Integer>>> buildingsByColor;

  // Log
  public final List<ActionRecord> actionRecords;

  // Turn tracking
  public int numTurns = 0;
  public int currentPlayerIndex = 0;
  public int currentTurnIndex = 0;

  // Phase flags
  public ActionPrompt currentPrompt = ActionPrompt.BUILD_INITIAL_SETTLEMENT;
  public boolean isInitialBuildPhase = true;
  public boolean isDiscarding = false;
  public boolean isMovingKnight = false;
  public boolean isRoadBuilding = false;
  public int freeRoadsAvailable = 0;

  // Trade state
  public boolean isResolvingTrade = false;
  public int[] currentTrade = new int[11]; // 10-resource tuple + acceptee color index
  public boolean[] acceptees;

  // Config
  public final int discardLimit;
  public final boolean friendlyRobber;

  public GameState(
      List<Player> players,
      CatanMap map,
      int discardLimit,
      boolean friendlyRobber,
      boolean officialSpiral,
      Random rng) {
    // Shuffle seating order
    List<Player> shuffled = new ArrayList<>(players);
    Collections.shuffle(shuffled, rng);

    this.colors = shuffled.stream().map(Player::getColor).toArray(Color[]::new);
    this.colorToIndex = new HashMap<>();
    for (int i = 0; i < colors.length; i++) colorToIndex.put(colors[i], i);

    this.board = new Board(map);
    this.resourceFreqdeck = Decks.startingResourceBank();
    this.developmentListdeck = Decks.startingDevCardBank();
    Collections.shuffle(this.developmentListdeck, rng);

    this.playerStates = new HashMap<>();
    this.buildingsByColor = new HashMap<>();
    for (Color c : colors) {
      playerStates.put(c, new PlayerState());
      Map<BuildingType, List<Integer>> byType = new HashMap<>();
      for (BuildingType bt : BuildingType.values()) byType.put(bt, new ArrayList<>());
      buildingsByColor.put(c, byType);
    }

    this.actionRecords = new ArrayList<>();
    this.acceptees = new boolean[colors.length];
    this.discardLimit = discardLimit;
    this.friendlyRobber = friendlyRobber;
  }

  private GameState(
      Color[] colors, Map<Color, Integer> colorToIndex, int discardLimit, boolean friendlyRobber) {
    this.colors = colors;
    this.colorToIndex = colorToIndex;
    this.discardLimit = discardLimit;
    this.friendlyRobber = friendlyRobber;
    this.playerStates = new HashMap<>();
    this.buildingsByColor = new HashMap<>();
    this.actionRecords = new ArrayList<>();
    this.resourceFreqdeck = new int[5];
    this.developmentListdeck = new ArrayList<>();
    this.acceptees = new boolean[0];
  }

  public Color currentColor() {
    return colors[currentPlayerIndex];
  }

  public Color currentTurnColor() {
    return colors[currentTurnIndex];
  }

  public PlayerState playerState(Color color) {
    return playerStates.get(color);
  }

  public int playerIndex(Color color) {
    return colorToIndex.get(color);
  }

  public GameState copy() {
    GameState c = new GameState(colors, colorToIndex, discardLimit, friendlyRobber);

    c.board = board.copy();
    c.resourceFreqdeck = resourceFreqdeck.clone();
    c.developmentListdeck = new ArrayList<>(developmentListdeck);

    for (Color color : colors) {
      c.playerStates.put(color, playerStates.get(color).copy());
      Map<BuildingType, List<Integer>> byType = new HashMap<>();
      for (BuildingType bt : BuildingType.values()) {
        byType.put(bt, new ArrayList<>(buildingsByColor.get(color).get(bt)));
      }
      c.buildingsByColor.put(color, byType);
    }

    c.actionRecords.addAll(actionRecords);

    c.numTurns = numTurns;
    c.currentPlayerIndex = currentPlayerIndex;
    c.currentTurnIndex = currentTurnIndex;
    c.currentPrompt = currentPrompt;
    c.isInitialBuildPhase = isInitialBuildPhase;
    c.isDiscarding = isDiscarding;
    c.isMovingKnight = isMovingKnight;
    c.isRoadBuilding = isRoadBuilding;
    c.freeRoadsAvailable = freeRoadsAvailable;
    c.isResolvingTrade = isResolvingTrade;
    c.currentTrade = currentTrade.clone();
    c.acceptees = acceptees.clone();

    return c;
  }
}

package io.bobba.woodsheep.game.engine;

import io.bobba.woodsheep.game.map.CatanMap;
import io.bobba.woodsheep.game.model.Decks;
import io.bobba.woodsheep.game.model.DevCard;
import io.bobba.woodsheep.game.model.PlayerColor;
import io.bobba.woodsheep.game.model.PlayerState;
import io.bobba.woodsheep.game.model.actions.ActionPrompt;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class State {
  public final List<Player> players;
  public final List<PlayerColor> colors;

  public int[] bank = Decks.startingResourceBank();
  public final Map<String, Integer> playerStateInt = new HashMap<>();
  public final Map<PlayerColor, PlayerState> playerState;
  public int currentPlayerIndex = 0;
  public int currentTurnIndex = 0;
  public int numTurns = 0;
  public ActionPrompt currentPrompt = ActionPrompt.BUILD_INITIAL_SETTLEMENT;

  public boolean isInitialBuildPhase = true;
  public boolean isDiscarding = false;
  public boolean isMovingKnight = false;

  public final CatanMap map;
  public final Board board;
  public int robberTileId;
  public int discardLimit = 7;
  public final java.util.Map<PlayerColor, Integer> lastInitialSettlement =
      new java.util.HashMap<>();

  public java.util.List<DevCard> developmentDeck;
  public boolean isRoadBuilding = false;
  public int freeRoadsAvailable = 0;

  public State(List<Player> players) {
    this.players = players;
    this.map = CatanMap.base();
    this.board = new Board(map);
    this.colors = players.stream().map(p -> p.color).toList();
    this.developmentDeck =
        List.of(
            DevCard.KNIGHT,
            DevCard.MONOPOLY,
            DevCard.ROAD_BUILDING,
            DevCard.VICTORY_POINT,
            DevCard.YEAR_OF_PLENTY);
    this.playerState =
        this.colors.stream().collect(Collectors.toMap(c -> c, c -> new PlayerState()));
  }

  public Player currentPlayer() {
    return players.get(currentPlayerIndex);
  }

  public PlayerColor currentColor() {
    return colors.get(currentPlayerIndex);
  }
}

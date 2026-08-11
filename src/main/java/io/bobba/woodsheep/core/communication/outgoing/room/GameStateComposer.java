package io.bobba.woodsheep.core.communication.outgoing.room;

import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;
import java.util.List;
import java.util.Map;

public class GameStateComposer extends OutgoingMessage {
  private final Payload payload;

  public GameStateComposer(
      String gameState,
      List<TilePayload> tiles,
      List<BuildingPayload> buildings,
      List<RoadPayload> roads,
      String currentColor,
      String currentTurnColor,
      String currentPrompt,
      int[] diceRoll,
      List<PlayerPayload> players,
      String yourColor,
      Map<String, Integer> yourHand,
      List<String> playableActionTypes) {
    super("gameState");
    this.payload =
        new Payload(
            gameState,
            tiles,
            buildings,
            roads,
            currentColor,
            currentTurnColor,
            currentPrompt,
            diceRoll,
            players,
            yourColor,
            yourHand,
            playableActionTypes);
  }

  @Override
  public Object getPayload() {
    return this.payload;
  }

  private record Payload(
      String gameState,
      List<TilePayload> tiles,
      List<BuildingPayload> buildings,
      List<RoadPayload> roads,
      String currentColor,
      String currentTurnColor,
      String currentPrompt,
      int[] diceRoll,
      List<PlayerPayload> players,
      String yourColor,
      Map<String, Integer> yourHand,
      List<String> playableActionTypes) {}

  public record TilePayload(int id, String resource, int number, int q, int r, int s) {}

  public record BuildingPayload(int nodeId, String color, String type) {}

  public record RoadPayload(int nodeA, int nodeB, String color) {}

  /** Publicly-known info about a player — no hidden hand contents. */
  public record PlayerPayload(
      String color,
      int visibleVictoryPoints,
      int resourceCount,
      int devCardCount,
      boolean hasLongestRoad,
      boolean hasLargestArmy) {}
}

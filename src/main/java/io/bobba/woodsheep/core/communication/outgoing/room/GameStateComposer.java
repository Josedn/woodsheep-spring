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
      List<String> playableActionTypes,
      Map<String, Integer> bankResources,
      int bankDevCardCount,
      List<Integer> buildableSettlementNodeIds,
      List<Integer> buildableCityNodeIds,
      List<EdgePayload> buildableRoadEdges) {
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
            playableActionTypes,
            bankResources,
            bankDevCardCount,
            buildableSettlementNodeIds,
            buildableCityNodeIds,
            buildableRoadEdges);
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
      List<String> playableActionTypes,
      Map<String, Integer> bankResources,
      int bankDevCardCount,
      List<Integer> buildableSettlementNodeIds,
      List<Integer> buildableCityNodeIds,
      List<EdgePayload> buildableRoadEdges) {}

  public record TilePayload(
      int id, String resource, int number, int q, int r, int s, Map<String, Integer> nodes) {}

  public record BuildingPayload(int nodeId, String color, String type) {}

  public record RoadPayload(int nodeA, int nodeB, String color) {}

  public record EdgePayload(int nodeA, int nodeB) {}

  /** Publicly-known info about a player — no hidden hand contents. */
  public record PlayerPayload(
      String color,
      String username,
      boolean isBot,
      int visibleVictoryPoints,
      int realVictoryPoints,
      int resourceCount,
      int devCardCount,
      int armyCount,
      int roadLength,
      boolean hasLongestRoad,
      boolean hasLargestArmy) {}
}

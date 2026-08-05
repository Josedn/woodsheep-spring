package io.bobba.woodsheep.core.communication.outgoing.room;

import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;
import java.util.List;

public class GameStateComposer extends OutgoingMessage {
  private final Payload payload;

  public GameStateComposer(
      String gameState,
      List<TilePayload> tiles,
      List<BuildingPayload> buildings,
      List<RoadPayload> roads) {
    super("gameState");
    this.payload = new Payload(gameState, tiles, buildings, roads);
  }

  @Override
  public Object getPayload() {
    return this.payload;
  }

  private record Payload(
      String gameState,
      List<TilePayload> tiles,
      List<BuildingPayload> buildings,
      List<RoadPayload> roads) {}

  public record TilePayload(int id, String resource, int number, int q, int r, int s) {}

  public record BuildingPayload(int nodeId, String color, String type) {}

  public record RoadPayload(int nodeA, int nodeB, String color) {}
}

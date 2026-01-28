package io.bobba.woodsheep.game.engine;

import static io.bobba.woodsheep.game.map.geometry.Edge.edgeKey;

import io.bobba.woodsheep.game.map.CatanMap;
import io.bobba.woodsheep.game.map.geometry.Edge;
import io.bobba.woodsheep.game.model.BuildingType;
import io.bobba.woodsheep.game.model.PlayerColor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Board {
  private final CatanMap map;
  // nodeId -> (color, building)
  private final Map<Integer, Map.Entry<PlayerColor, BuildingType>> buildings = new HashMap<>();
  // edge (min,max) -> color
  private final Map<Long, PlayerColor> roads = new HashMap<>();
  private final Set<Integer> blockedNodes = new HashSet<>(); // distance-1 rule

  public Board(CatanMap map) {
    this.map = map;
  }

  public Set<Integer> buildableNodeIds(PlayerColor playerColor, boolean initialPhase) {
    if (initialPhase) {
      // any unblocked, empty land node
      Set<Integer> out = new HashSet<>(map.landNodes);
      out.removeAll(blockedNodes);
      out.removeAll(buildings.keySet());
      return out;
    }
    // Connected to existing road or building, still respecting distance-1
    Set<Integer> candidates = new HashSet<>();
    for (Integer nodeId : map.landNodes) {
      if (blockedNodes.contains(nodeId) || buildings.containsKey(nodeId)) continue;
      // must touch at least one of our roads
      boolean touchesOwnedRoad = false;
      for (Integer neighbor : map.nodeNeighbors.getOrDefault(nodeId, Set.of())) {
        if (roads.getOrDefault(edgeKey(nodeId, neighbor), null) == playerColor) {
          touchesOwnedRoad = true;
          break;
        }
      }
      if (touchesOwnedRoad) {
        candidates.add(nodeId);
      }
    }
    return candidates;
  }

  public List<Edge> buildableEdges(PlayerColor playerColor) {
    List<Edge> edges = new ArrayList<>();
    // Only edges adjacent to owned nodes OR extend from existing own roads
    Set<Integer> ownedNodes = ownedNodes(playerColor);
    // Edges from owned nodes
    for (Integer nodeA : ownedNodes) {
      for (Integer nodeB : map.nodeNeighbors.getOrDefault(nodeA, Set.of())) {
        long key = edgeKey(nodeA, nodeB);
        if (!roads.containsKey(key)) {
          edges.add(new Edge(Math.min(nodeA, nodeB), Math.max(nodeA, nodeB)));
        }
      }
    }
    // Edges extending owned roads (one endpoint shared)
    for (Map.Entry<Long, PlayerColor> roadEntry : roads.entrySet()) {
      if (roadEntry.getValue() != playerColor) continue;
      int nodeA = (int) (roadEntry.getKey() >> 32);
      int nodeB = (int) (roadEntry.getKey().longValue());
      for (Integer neighborOfA : map.nodeNeighbors.getOrDefault(nodeA, Set.of())) {
        long key = edgeKey(nodeA, neighborOfA);
        if (!roads.containsKey(key)) {
          edges.add(new Edge(Math.min(nodeA, neighborOfA), Math.max(nodeA, neighborOfA)));
        }
      }
      for (Integer neighborOfB : map.nodeNeighbors.getOrDefault(nodeB, Set.of())) {
        long key = edgeKey(nodeB, neighborOfB);
        if (!roads.containsKey(key)) {
          edges.add(new Edge(Math.min(nodeB, neighborOfB), Math.max(nodeB, neighborOfB)));
        }
      }
    }
    return edges;
  }

  private Set<Integer> ownedNodes(PlayerColor color) {
    Set<Integer> nodesOwned = new HashSet<>();
    for (Map.Entry<Integer, Map.Entry<PlayerColor, BuildingType>> buildingEntry :
        buildings.entrySet()) {
      if (buildingEntry.getValue().getKey() == color) {
        nodesOwned.add(buildingEntry.getKey());
      }
    }
    return nodesOwned;
  }

  public Set<Integer> ownedSettlementNodes(PlayerColor color) {
    Set<Integer> owned = new HashSet<>();
    for (Map.Entry<Integer, Map.Entry<PlayerColor, BuildingType>> e : buildings.entrySet()) {
      if (e.getValue().getKey() == color && e.getValue().getValue() == BuildingType.SETTLEMENT) {
        owned.add(e.getKey());
      }
    }
    return owned;
  }

  public void buildSettlement(PlayerColor color, int nodeId) {
    if (buildings.containsKey(nodeId)) {
      throw new IllegalArgumentException("occupied");
    }
    buildings.put(nodeId, Map.entry(color, BuildingType.SETTLEMENT));
    // distance-1 rule: block neighbors
    blockedNodes.add(nodeId);
    for (Integer n : map.nodeNeighbors.getOrDefault(nodeId, Set.of())) {
      blockedNodes.add(n);
    }
  }

  public void buildRoad(PlayerColor color, int nodeA, int nodeB) {
    long key = edgeKey(nodeA, nodeB);
    if (roads.containsKey(key)) {
      throw new IllegalArgumentException("road exists");
    }
    roads.put(key, color);
  }

  public Map.Entry<PlayerColor, BuildingType> buildingAt(int nodeId) {
    return buildings.get(nodeId);
  }
}

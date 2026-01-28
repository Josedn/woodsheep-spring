package io.bobba.woodsheep.game.map;

import io.bobba.woodsheep.game.map.geometry.Direction;
import io.bobba.woodsheep.game.map.geometry.Edge;
import io.bobba.woodsheep.game.map.geometry.EdgeRef;
import io.bobba.woodsheep.game.map.geometry.NodeRef;
import io.bobba.woodsheep.game.map.tiles.LandTile;
import io.bobba.woodsheep.game.map.tiles.Tile;
import io.bobba.woodsheep.game.map.tiles.TileType;
import io.bobba.woodsheep.game.map.tiles.WaterTile;
import io.bobba.woodsheep.game.model.Resource;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.javatuples.Pair;

public class CatanMap {
  public final Map<Coordinate, Tile> tiles = new LinkedHashMap<>();
  public final Map<Coordinate, LandTile> landTiles = new LinkedHashMap<>();
  public final Map<Integer, List<LandTile>> adjacentTilesByNode =
      new HashMap<>(); // nodeId -> tiles
  public final Map<Integer, LandTile> tilesById = new HashMap<>();

  public final Set<Integer> landNodes = new HashSet<>();
  public final Set<Long> edgeKeys = new HashSet<>(); // encoded (min,max)
  public final Map<Integer, Set<Integer>> nodeNeighbors = new HashMap<>();
  public final Map<Integer, Integer> tileNumberById = new HashMap<>();
  public final Map<Integer, Resource> tileResourceById = new HashMap<>();
  public final Map<Integer, Set<Integer>> tileNodesById = new HashMap<>();

  public static CatanMap base() {
    return fromTemplate(MapTemplate.buildBaseTemplate());
  }

  private static CatanMap fromTemplate(MapTemplate mapTemplate) {
    CatanMap m = new CatanMap();
    buildTiles(m, mapTemplate);
    m.rebuildCaches();
    return m;
  }

  private static void buildTiles(CatanMap catanMap, MapTemplate mapTemplate) {
    int nodeAutoinc = 0;
    int landIndex = 0;
    int idAutoinc = 0;
    // Maintain insertion order for deterministic ids
    for (Map.Entry<Coordinate, TileType> entry : mapTemplate.topology().entrySet()) {
      Coordinate coordinate = entry.getKey();
      TileType tileType = entry.getValue();
      // Initialize node/edge maps for this tile
      EnumMap<NodeRef, Integer> nodes = new EnumMap<>(NodeRef.class);
      EnumMap<EdgeRef, Edge> edges = new EnumMap<>(EdgeRef.class);

      for (NodeRef nodeRef : NodeRef.values()) {
        nodes.put(nodeRef, null);
      }
      for (EdgeRef edgeRef : EdgeRef.values()) {
        edges.put(edgeRef, null);
      }

      // Share with neighbors if present
      for (Direction direction : Direction.values()) {
        Coordinate nodeCoordinate = coordinate.add(direction.unitVector);
        Tile neighbor = catanMap.tiles.get(nodeCoordinate);
        if (neighbor == null) {
          continue;
        }
        if (neighbor instanceof LandTile neighborTile) {
          switch (direction) {
            case EAST -> {
              nodes.put(NodeRef.NORTHEAST, neighborTile.nodes().get(NodeRef.NORTHWEST));
              nodes.put(NodeRef.SOUTHEAST, neighborTile.nodes().get(NodeRef.SOUTHWEST));
              edges.put(EdgeRef.EAST, neighborTile.edges().get(EdgeRef.WEST));
            }
            case SOUTHEAST -> {
              nodes.put(NodeRef.SOUTH, neighborTile.nodes().get(NodeRef.NORTHWEST));
              nodes.put(NodeRef.SOUTHEAST, neighborTile.nodes().get(NodeRef.NORTH));
              edges.put(EdgeRef.SOUTHEAST, neighborTile.edges().get(EdgeRef.NORTHWEST));
            }
            case SOUTHWEST -> {
              nodes.put(NodeRef.SOUTH, neighborTile.nodes().get(NodeRef.NORTHEAST));
              nodes.put(NodeRef.SOUTHWEST, neighborTile.nodes().get(NodeRef.NORTH));
              edges.put(EdgeRef.SOUTHWEST, neighborTile.edges().get(EdgeRef.NORTHEAST));
            }
            case WEST -> {
              nodes.put(NodeRef.NORTHWEST, neighborTile.nodes().get(NodeRef.NORTHEAST));
              nodes.put(NodeRef.SOUTHWEST, neighborTile.nodes().get(NodeRef.SOUTHEAST));
              edges.put(EdgeRef.WEST, neighborTile.edges().get(EdgeRef.EAST));
            }
            case NORTHWEST -> {
              nodes.put(NodeRef.NORTH, neighborTile.nodes().get(NodeRef.SOUTHEAST));
              nodes.put(NodeRef.NORTHWEST, neighborTile.nodes().get(NodeRef.SOUTH));
              edges.put(EdgeRef.NORTHWEST, neighborTile.edges().get(EdgeRef.SOUTHEAST));
            }
            case NORTHEAST -> {
              nodes.put(NodeRef.NORTH, neighborTile.nodes().get(NodeRef.SOUTHWEST));
              nodes.put(NodeRef.NORTHEAST, neighborTile.nodes().get(NodeRef.SOUTH));
              edges.put(EdgeRef.NORTHEAST, neighborTile.edges().get(EdgeRef.SOUTHWEST));
            }
          }
        }
      }

      // Create new nodes/edges for unset
      for (NodeRef nr : NodeRef.values()) {
        if (nodes.get(nr) == null) {
          nodes.put(nr, nodeAutoinc++);
        }
      }
      for (EdgeRef er : EdgeRef.values()) {
        if (edges.get(er) == null) {
          var pair = getEdgeNodes(er);
          int a = nodes.get(pair.getValue0());
          int b = nodes.get(pair.getValue1());
          edges.put(er, new Edge(a, b));
        }
      }

      // create and save tile
      Tile tile;
      if (tileType == TileType.RESOURCE) {
        // assign resource/number in order, desert gets 0
        Resource resource = mapTemplate.tileResources().get(landIndex);
        int number =
            (resource == Resource.NONE
                ? 0
                : mapTemplate
                    .numbers()
                    .get(landIndex - countDesertsBefore(mapTemplate.tileResources(), landIndex)));
        tile = new LandTile(idAutoinc, resource, number, nodes, edges);
        catanMap.landTiles.put(coordinate, (LandTile) tile);
        catanMap.tilesById.put(idAutoinc, (LandTile) tile);
        landIndex++;
      } else {
        tile = new WaterTile(nodes, edges);
      }
      catanMap.tiles.put(coordinate, tile);
      idAutoinc++;
    }
  }

  private static int countDesertsBefore(List<Resource> tileResources, int idx) {
    int count = 0;
    for (int i = 0; i < idx; i++) {
      if (tileResources.get(i) == Resource.NONE) {
        count++;
      }
    }
    return count;
  }

  private static Pair<NodeRef, NodeRef> getEdgeNodes(EdgeRef er) {
    return switch (er) {
      case EAST -> Pair.with(NodeRef.NORTHEAST, NodeRef.SOUTHEAST);
      case SOUTHEAST -> Pair.with(NodeRef.SOUTHEAST, NodeRef.SOUTH);
      case SOUTHWEST -> Pair.with(NodeRef.SOUTH, NodeRef.SOUTHWEST);
      case WEST -> Pair.with(NodeRef.SOUTHWEST, NodeRef.NORTHWEST);
      case NORTHWEST -> Pair.with(NodeRef.NORTHWEST, NodeRef.NORTH);
      case NORTHEAST -> Pair.with(NodeRef.NORTH, NodeRef.NORTHEAST);
    };
  }

  private void rebuildCaches() {
    // land nodes
    for (LandTile tile : landTiles.values()) {
      landNodes.addAll(tile.nodes().values());
      for (Edge edge : tile.edges().values()) {
        int nodeA = Math.min(edge.nodeA(), edge.nodeB());
        int nodeB = Math.max(edge.nodeA(), edge.nodeB());
        long key = Edge.edgeKey(nodeA, nodeB);
        edgeKeys.add(key);
        nodeNeighbors.computeIfAbsent(nodeA, k -> new HashSet<>()).add(nodeB);
        nodeNeighbors.computeIfAbsent(nodeB, k -> new HashSet<>()).add(nodeA);
      }
      tileNumberById.put(tile.id(), tile.number());
      tileResourceById.put(tile.id(), tile.resource());
      tileNodesById.put(tile.id(), new HashSet<>(tile.nodes().values()));
    }
    // adjacent tiles by node
    for (LandTile tile : landTiles.values()) {
      for (Integer nodeId : tile.nodes().values()) {
        adjacentTilesByNode.computeIfAbsent(nodeId, k -> new ArrayList<>()).add(tile);
      }
    }
  }

  public Set<Integer> getTileIdsByNumber(int number) {
    Set<Integer> ids = new HashSet<>();
    for (Map.Entry<Integer, Integer> entry : tileNumberById.entrySet()) {
      if (entry.getValue() != null && entry.getValue() == number) {
        ids.add(entry.getKey());
      }
    }
    return ids;
  }

  public Set<Integer> getTileNodes(int tileId) {
    return tileNodesById.getOrDefault(tileId, Set.of());
  }
}

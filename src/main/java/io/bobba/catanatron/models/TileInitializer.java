package io.bobba.catanatron.models;

import static io.bobba.catanatron.models.MapTemplate.TileType.LAND;
import static io.bobba.catanatron.models.MapTemplate.TileType.WATER;

import io.bobba.catanatron.enums.Direction;
import io.bobba.catanatron.enums.EdgeRef;
import io.bobba.catanatron.enums.NodeRef;
import io.bobba.catanatron.enums.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Builds the tile map from a MapTemplate, mirroring initialize_tiles() in map.py. */
public final class TileInitializer {

  // Which NodeRef pair each EdgeRef connects.
  private static final Map<EdgeRef, NodeRef[]> EDGE_NODES;

  static {
    EDGE_NODES = new EnumMap<>(EdgeRef.class);
    EDGE_NODES.put(EdgeRef.EAST, new NodeRef[] {NodeRef.NORTHEAST, NodeRef.SOUTHEAST});
    EDGE_NODES.put(EdgeRef.SOUTHEAST, new NodeRef[] {NodeRef.SOUTHEAST, NodeRef.SOUTH});
    EDGE_NODES.put(EdgeRef.SOUTHWEST, new NodeRef[] {NodeRef.SOUTH, NodeRef.SOUTHWEST});
    EDGE_NODES.put(EdgeRef.WEST, new NodeRef[] {NodeRef.SOUTHWEST, NodeRef.NORTHWEST});
    EDGE_NODES.put(EdgeRef.NORTHWEST, new NodeRef[] {NodeRef.NORTHWEST, NodeRef.NORTH});
    EDGE_NODES.put(EdgeRef.NORTHEAST, new NodeRef[] {NodeRef.NORTH, NodeRef.NORTHEAST});
  }

  // Which nodes a port faces, keyed by the port tile's Direction.
  static final Map<Direction, NodeRef[]> PORT_DIRECTION_TO_NODEREFS;

  static {
    PORT_DIRECTION_TO_NODEREFS = new EnumMap<>(Direction.class);
    PORT_DIRECTION_TO_NODEREFS.put(
        Direction.WEST, new NodeRef[] {NodeRef.NORTHWEST, NodeRef.SOUTHWEST});
    PORT_DIRECTION_TO_NODEREFS.put(
        Direction.NORTHWEST, new NodeRef[] {NodeRef.NORTH, NodeRef.NORTHWEST});
    PORT_DIRECTION_TO_NODEREFS.put(
        Direction.NORTHEAST, new NodeRef[] {NodeRef.NORTHEAST, NodeRef.NORTH});
    PORT_DIRECTION_TO_NODEREFS.put(
        Direction.EAST, new NodeRef[] {NodeRef.SOUTHEAST, NodeRef.NORTHEAST});
    PORT_DIRECTION_TO_NODEREFS.put(
        Direction.SOUTHEAST, new NodeRef[] {NodeRef.SOUTH, NodeRef.SOUTHEAST});
    PORT_DIRECTION_TO_NODEREFS.put(
        Direction.SOUTHWEST, new NodeRef[] {NodeRef.SOUTHWEST, NodeRef.SOUTH});
  }

  private TileInitializer() {}

  public static Map<Coordinate, Object> initializeTiles(
      MapTemplate template,
      List<Integer> numbersParam,
      List<Resource> portResourcesParam,
      List<Resource> tileResourcesParam,
      boolean officialSpiral,
      Random rng) {

    List<Resource> portResources =
        shuffledCopy(
            portResourcesParam != null ? portResourcesParam : template.portResources(), rng);
    List<Resource> tileResources =
        shuffledCopy(
            tileResourcesParam != null ? tileResourcesParam : template.tileResources(), rng);
    List<Integer> numbers =
        shuffledCopy(numbersParam != null ? numbersParam : template.numbers(), rng);

    Map<Coordinate, Object> allTiles = new HashMap<>();
    int[] nodeAutoinc = {0};
    int[] tileAutoinc = {0};
    int[] portAutoinc = {0};

    for (Map.Entry<Coordinate, Object> entry : template.topology().entrySet()) {
      Coordinate coord = entry.getKey();
      Object tileType = entry.getValue();

      NodeEdgeResult ner = getNodesAndEdges(allTiles, coord, nodeAutoinc[0]);
      nodeAutoinc[0] = ner.nextNodeId;

      if (tileType instanceof MapTemplate.PortSpec ps) {
        Resource res = portResources.remove(portResources.size() - 1);
        allTiles.put(coord, new Port(portAutoinc[0]++, res, ps.direction(), ner.nodes, ner.edges));
      } else if (tileType == LAND) {
        Resource res = tileResources.remove(tileResources.size() - 1);
        if (res != null) {
          int num = numbers.remove(numbers.size() - 1);
          allTiles.put(coord, new LandTile(tileAutoinc[0]++, res, num, ner.nodes, ner.edges));
        } else {
          allTiles.put(coord, new LandTile(tileAutoinc[0]++, null, null, ner.nodes, ner.edges));
        }
      } else if (tileType == WATER) {
        allTiles.put(coord, new Water(ner.nodes, ner.edges));
      } else {
        throw new IllegalArgumentException("Invalid tile type: " + tileType);
      }
    }

    if (officialSpiral) {
      applyOfficialSpiral(allTiles, template);
    }

    return allTiles;
  }

  private static void applyOfficialSpiral(Map<Coordinate, Object> allTiles, MapTemplate template) {
    // Determine spiral start: outer corner of second ring for BASE, first ring for MINI
    long landCount = allTiles.values().stream().filter(t -> t instanceof LandTile).count();
    Coordinate start = (landCount > 7) ? new Coordinate(2, -2, 0) : new Coordinate(1, -1, 0);

    List<Coordinate> spiral = Spiral.spiralLandCoordinates(allTiles, start);
    int[] numbers = MapTemplates.BASE_NUMBERS_IN_SPIRAL_ORDER;
    int i = 0;
    for (Coordinate coord : spiral) {
      LandTile tile = (LandTile) allTiles.get(coord);
      if (tile.resource == null) continue; // desert — skip
      tile.number = numbers[i++];
    }
  }

  private static NodeEdgeResult getNodesAndEdges(
      Map<Coordinate, Object> tiles, Coordinate coord, int nodeAutoinc) {

    Map<NodeRef, Integer> nodes = new EnumMap<>(NodeRef.class);
    Map<EdgeRef, EdgeId> edges = new EnumMap<>(EdgeRef.class);
    for (NodeRef nr : NodeRef.values()) nodes.put(nr, null);
    for (EdgeRef er : EdgeRef.values()) edges.put(er, null);

    for (Direction d : Direction.values()) {
      Coordinate neighborCoord = coord.neighbor(d);
      Object neighbor = tiles.get(neighborCoord);
      if (neighbor == null) continue;

      Map<NodeRef, Integer> nNodes = tileNodes(neighbor);
      Map<EdgeRef, EdgeId> nEdges = tileEdges(neighbor);
      if (nNodes == null) continue;

      switch (d) {
        case EAST -> {
          nodes.put(NodeRef.NORTHEAST, nNodes.get(NodeRef.NORTHWEST));
          nodes.put(NodeRef.SOUTHEAST, nNodes.get(NodeRef.SOUTHWEST));
          edges.put(EdgeRef.EAST, nEdges.get(EdgeRef.WEST));
        }
        case SOUTHEAST -> {
          nodes.put(NodeRef.SOUTH, nNodes.get(NodeRef.NORTHWEST));
          nodes.put(NodeRef.SOUTHEAST, nNodes.get(NodeRef.NORTH));
          edges.put(EdgeRef.SOUTHEAST, nEdges.get(EdgeRef.NORTHWEST));
        }
        case SOUTHWEST -> {
          nodes.put(NodeRef.SOUTH, nNodes.get(NodeRef.NORTHEAST));
          nodes.put(NodeRef.SOUTHWEST, nNodes.get(NodeRef.NORTH));
          edges.put(EdgeRef.SOUTHWEST, nEdges.get(EdgeRef.NORTHEAST));
        }
        case WEST -> {
          nodes.put(NodeRef.NORTHWEST, nNodes.get(NodeRef.NORTHEAST));
          nodes.put(NodeRef.SOUTHWEST, nNodes.get(NodeRef.SOUTHEAST));
          edges.put(EdgeRef.WEST, nEdges.get(EdgeRef.EAST));
        }
        case NORTHWEST -> {
          nodes.put(NodeRef.NORTH, nNodes.get(NodeRef.SOUTHEAST));
          nodes.put(NodeRef.NORTHWEST, nNodes.get(NodeRef.SOUTH));
          edges.put(EdgeRef.NORTHWEST, nEdges.get(EdgeRef.SOUTHEAST));
        }
        case NORTHEAST -> {
          nodes.put(NodeRef.NORTH, nNodes.get(NodeRef.SOUTHWEST));
          nodes.put(NodeRef.NORTHEAST, nNodes.get(NodeRef.SOUTH));
          edges.put(EdgeRef.NORTHEAST, nEdges.get(EdgeRef.SOUTHWEST));
        }
      }
    }

    // Allocate new node IDs for still-null slots
    for (NodeRef nr : NodeRef.values()) {
      if (nodes.get(nr) == null) {
        nodes.put(nr, nodeAutoinc++);
      }
    }
    // Build edges for still-null slots
    for (EdgeRef er : EdgeRef.values()) {
      if (edges.get(er) == null) {
        NodeRef[] ends = EDGE_NODES.get(er);
        edges.put(er, new EdgeId(nodes.get(ends[0]), nodes.get(ends[1])));
      }
    }

    return new NodeEdgeResult(nodes, edges, nodeAutoinc);
  }

  private record NodeEdgeResult(
      Map<NodeRef, Integer> nodes, Map<EdgeRef, EdgeId> edges, int nextNodeId) {}

  private static Map<NodeRef, Integer> tileNodes(Object tile) {
    if (tile instanceof LandTile t) return t.nodes;
    if (tile instanceof Port t) return t.nodes;
    if (tile instanceof Water t) return t.nodes();
    return null;
  }

  private static Map<EdgeRef, EdgeId> tileEdges(Object tile) {
    if (tile instanceof LandTile t) return t.edges;
    if (tile instanceof Port t) return t.edges;
    if (tile instanceof Water t) return t.edges();
    return null;
  }

  private static <T> List<T> shuffledCopy(List<T> src, Random rng) {
    List<T> copy = new ArrayList<>(src);
    Collections.shuffle(copy, rng);
    return copy;
  }
}

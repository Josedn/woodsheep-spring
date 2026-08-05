package com.catanatron.core.models;

import com.catanatron.core.enums.NodeRef;
import com.catanatron.core.enums.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class CatanMap {

  public static final int NUM_NODES = 54;
  public static final int NUM_EDGES = 72;
  public static final int NUM_TILES = 19;

  public final Map<Coordinate, Object> tiles;
  public final Map<Coordinate, LandTile> landTiles;

  /** port resource (null = 3:1) → set of node IDs with that port */
  public final Map<Resource, Set<Integer>> portNodes;

  /** node IDs that are null-keyed (3:1) ports — stored separately */
  public final Set<Integer> genericPortNodes;

  public final Set<Integer> landNodes;

  /** node ID → adjacent land tiles */
  public final Map<Integer, List<LandTile>> adjacentTiles;

  /** node ID → per-resource expected production per roll */
  public final Map<Integer, Map<Resource, Double>> nodeProduction;

  public final Map<Integer, LandTile> tilesById;
  public final Map<Integer, Port> portsById;

  private CatanMap(
      Map<Coordinate, Object> tiles,
      Map<Coordinate, LandTile> landTiles,
      Map<Resource, Set<Integer>> portNodes,
      Set<Integer> genericPortNodes,
      Set<Integer> landNodes,
      Map<Integer, List<LandTile>> adjacentTiles,
      Map<Integer, Map<Resource, Double>> nodeProduction,
      Map<Integer, LandTile> tilesById,
      Map<Integer, Port> portsById) {
    this.tiles = Collections.unmodifiableMap(tiles);
    this.landTiles = Collections.unmodifiableMap(landTiles);
    this.portNodes = portNodes;
    this.genericPortNodes = genericPortNodes;
    this.landNodes = Collections.unmodifiableSet(landNodes);
    this.adjacentTiles = adjacentTiles;
    this.nodeProduction = nodeProduction;
    this.tilesById = Collections.unmodifiableMap(tilesById);
    this.portsById = Collections.unmodifiableMap(portsById);
  }

  public static CatanMap fromTemplate(MapTemplate template, boolean officialSpiral, Random rng) {
    Map<Coordinate, Object> tiles =
        TileInitializer.initializeTiles(template, null, null, null, officialSpiral, rng);
    return fromTiles(tiles);
  }

  public static CatanMap fromTiles(Map<Coordinate, Object> tiles) {
    Map<Coordinate, LandTile> landTiles = new HashMap<>();
    Map<Integer, LandTile> tilesById = new HashMap<>();
    Map<Integer, Port> portsById = new HashMap<>();

    for (Map.Entry<Coordinate, Object> e : tiles.entrySet()) {
      if (e.getValue() instanceof LandTile lt) {
        landTiles.put(e.getKey(), lt);
        tilesById.put(lt.id, lt);
      } else if (e.getValue() instanceof Port p) {
        portsById.put(p.id, p);
      }
    }

    Set<Integer> landNodes = new HashSet<>();
    for (LandTile lt : landTiles.values()) {
      landNodes.addAll(lt.nodes.values());
    }

    Map<Integer, List<LandTile>> adjacentTiles = initAdjacentTiles(landTiles);
    Map<Integer, Map<Resource, Double>> nodeProduction = initNodeProduction(adjacentTiles);

    Map<Resource, Set<Integer>> portNodes = new HashMap<>();
    Set<Integer> genericPortNodes = new HashSet<>();
    for (Port p : portsById.values()) {
      NodeRef[] refs = TileInitializer.PORT_DIRECTION_TO_NODEREFS.get(p.direction);
      Set<Integer> bucket =
          (p.resource != null)
              ? portNodes.computeIfAbsent(p.resource, k -> new HashSet<>())
              : genericPortNodes;
      bucket.add(p.nodes.get(refs[0]));
      bucket.add(p.nodes.get(refs[1]));
    }

    return new CatanMap(
        tiles,
        landTiles,
        portNodes,
        genericPortNodes,
        landNodes,
        adjacentTiles,
        nodeProduction,
        tilesById,
        portsById);
  }

  private static Map<Integer, List<LandTile>> initAdjacentTiles(
      Map<Coordinate, LandTile> landTiles) {
    Map<Integer, List<LandTile>> adj = new HashMap<>();
    for (LandTile lt : landTiles.values()) {
      for (int nodeId : lt.nodes.values()) {
        adj.computeIfAbsent(nodeId, k -> new java.util.ArrayList<>()).add(lt);
      }
    }
    return adj;
  }

  private static Map<Integer, Map<Resource, Double>> initNodeProduction(
      Map<Integer, List<LandTile>> adjacentTiles) {
    Map<Integer, Map<Resource, Double>> prod = new HashMap<>();
    for (Map.Entry<Integer, List<LandTile>> e : adjacentTiles.entrySet()) {
      Map<Resource, Double> counter = new HashMap<>();
      for (LandTile lt : e.getValue()) {
        if (lt.resource != null && lt.number != null) {
          counter.merge(lt.resource, numberProbability(lt.number), Double::sum);
        }
      }
      prod.put(e.getKey(), counter);
    }
    return prod;
  }

  private static double numberProbability(int number) {
    // Probability = (6 - |7 - number|) / 36
    return (6 - Math.abs(7 - number)) / 36.0;
  }
}

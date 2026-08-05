package com.catanatron.core.models;

import com.catanatron.core.enums.BuildingType;
import com.catanatron.core.enums.Color;
import com.catanatron.core.enums.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Board {

  public final CatanMap map;

  /** node_id → (Color, BuildingType) */
  public final Map<Integer, Color[]> buildings; // Color[]={color, null} — using parallel arrays

  public final Map<Integer, BuildingType> buildingTypes;

  /** EdgeId (canonical a<b) → Color */
  public final Map<EdgeId, Color> roads;

  /** color → list of node-sets (one per connected component) */
  public final Map<Color, List<Set<Integer>>> connectedComponents;

  public final Set<Integer> boardBuildableIds;
  public final Map<Color, Integer> roadLengths;
  public Color roadColor;
  public int roadLength;
  public Coordinate robberCoordinate;

  // shared across all Board instances for the same map
  private final Graph graph;

  // caches
  private Map<Color, List<EdgeId>> buildableEdgesCache = new HashMap<>();
  private Map<Color, Set<Resource>> playerPortResourcesCache = new HashMap<>();

  public Board(CatanMap map) {
    this.map = map;
    this.graph = Graph.fromMap(map);

    this.buildings = new HashMap<>();
    this.buildingTypes = new HashMap<>();
    this.roads = new HashMap<>();
    this.connectedComponents = new HashMap<>();
    for (Color c : Color.values()) connectedComponents.put(c, new ArrayList<>());
    this.boardBuildableIds = new HashSet<>(map.landNodes);
    this.roadLengths = new HashMap<>();
    for (Color c : Color.values()) roadLengths.put(c, 0);
    this.roadColor = null;
    this.roadLength = 0;

    // place robber on desert
    this.robberCoordinate =
        map.landTiles.entrySet().stream()
            .filter(e -> e.getValue().resource == null)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No desert tile found"));
  }

  // ===== Build actions =====

  public RoadResult buildSettlement(Color color, int nodeId, boolean initialBuildPhase) {
    List<Integer> buildable = buildableNodeIds(color, initialBuildPhase);
    if (!buildable.contains(nodeId))
      throw new IllegalArgumentException("Invalid Settlement Placement: not buildable");
    if (buildings.containsKey(nodeId))
      throw new IllegalArgumentException("Invalid Settlement Placement: building exists");

    buildings.put(nodeId, new Color[] {color});
    buildingTypes.put(nodeId, BuildingType.SETTLEMENT);

    Color previousRoadColor = roadColor;

    if (initialBuildPhase) {
      Set<Integer> component = new HashSet<>();
      component.add(nodeId);
      connectedComponents.get(color).add(component);
    } else {
      // Check if this settlement cuts any opponent's road
      Map<Color, List<EdgeId>> edgesByColor = new HashMap<>();
      for (int neighbor : graph.neighbors(nodeId)) {
        EdgeId edge = canonicalEdge(nodeId, neighbor);
        Color ec = roads.get(edge);
        edgesByColor.computeIfAbsent(ec, k -> new ArrayList<>()).add(edge);
      }
      for (Map.Entry<Color, List<EdgeId>> entry : edgesByColor.entrySet()) {
        Color ec = entry.getKey();
        List<EdgeId> edges = entry.getValue();
        if (ec == null || ec == color) continue;
        if (edges.size() == 2) {
          int a = otherNode(edges.get(0), nodeId);
          int c = otherNode(edges.get(1), nodeId);
          Set<Integer> aSet = dfsWalk(a, ec);
          Set<Integer> cSet = dfsWalk(c, ec);
          int idx = getComponentIndex(nodeId, ec);
          connectedComponents.get(ec).remove(idx);
          connectedComponents.get(ec).add(aSet);
          connectedComponents.get(ec).add(cSet);
          int newLen =
              connectedComponents.get(ec).stream()
                  .mapToInt(comp -> longestAcyclicPath(comp, ec).size())
                  .max()
                  .orElse(0);
          roadLengths.put(ec, newLen);
          recomputeRoadLeader();
        } else if (edges.size() == 1) {
          // Endpoint blocked: remove nodeId from opponent's component
          Integer bIdx = getComponentIndex(nodeId, ec);
          if (bIdx != null) connectedComponents.get(ec).get(bIdx).remove(nodeId);
        }
      }
    }

    boardBuildableIds.remove(nodeId);
    for (int neighbor : graph.neighbors(nodeId)) boardBuildableIds.remove(neighbor);
    buildableEdgesCache.clear();
    playerPortResourcesCache.clear();

    return new RoadResult(previousRoadColor, roadColor, new HashMap<>(roadLengths));
  }

  public RoadResult buildRoad(Color color, EdgeId edge) {
    List<EdgeId> buildable = buildableEdges(color);
    EdgeId canonical = canonicalEdge(edge.a(), edge.b());
    if (!buildable.contains(canonical))
      throw new IllegalArgumentException("Invalid Road Placement");

    roads.put(canonical, color);

    int a = canonical.a(), b = canonical.b();
    Integer aIdx = getComponentIndex(a, color);
    Integer bIdx = getComponentIndex(b, color);

    if (aIdx == null && !isEnemyNode(a, color)) {
      connectedComponents.get(color).get(bIdx).add(a);
    } else if (bIdx == null && !isEnemyNode(b, color)) {
      connectedComponents.get(color).get(aIdx).add(b);
    } else if (aIdx != null && bIdx != null && !aIdx.equals(bIdx)) {
      Set<Integer> merged = new HashSet<>();
      merged.addAll(connectedComponents.get(color).get(aIdx));
      merged.addAll(connectedComponents.get(color).get(bIdx));
      int larger = Math.max(aIdx, bIdx), smaller = Math.min(aIdx, bIdx);
      connectedComponents.get(color).remove(larger);
      connectedComponents.get(color).set(smaller, merged);
    }
    // else same component — nothing to do

    Color previousRoadColor = roadColor;
    // find updated component
    Set<Integer> component = findComponentContaining(a, color);
    if (component == null) component = findComponentContaining(b, color);
    int candidateLen = component != null ? longestAcyclicPath(component, color).size() : 0;
    roadLengths.put(color, Math.max(roadLengths.get(color), candidateLen));
    if (candidateLen >= 5 && candidateLen > roadLength) {
      roadColor = color;
      roadLength = candidateLen;
    }

    buildableEdgesCache.clear();
    return new RoadResult(previousRoadColor, roadColor, new HashMap<>(roadLengths));
  }

  public void buildCity(Color color, int nodeId) {
    if (!buildings.containsKey(nodeId)
        || buildings.get(nodeId)[0] != color
        || buildingTypes.get(nodeId) != BuildingType.SETTLEMENT) {
      throw new IllegalArgumentException("Invalid City Placement: no player settlement there");
    }
    buildingTypes.put(nodeId, BuildingType.CITY);
  }

  // ===== Queries =====

  public List<Integer> buildableNodeIds(Color color, boolean initialBuildPhase) {
    if (initialBuildPhase) {
      List<Integer> list = new ArrayList<>(boardBuildableIds);
      Collections.sort(list);
      return list;
    }
    Set<Integer> nodes = new HashSet<>();
    for (Set<Integer> comp : connectedComponents.get(color)) nodes.addAll(comp);
    List<Integer> result = new ArrayList<>();
    for (int n : nodes) if (boardBuildableIds.contains(n)) result.add(n);
    Collections.sort(result);
    return result;
  }

  public List<EdgeId> buildableEdges(Color color) {
    if (buildableEdgesCache.containsKey(color)) return buildableEdgesCache.get(color);

    Set<Integer> expandable = new HashSet<>();
    for (Set<Integer> comp : connectedComponents.get(color)) expandable.addAll(comp);

    Set<EdgeId> result = new HashSet<>();
    for (EdgeId edge : graph.edges(expandable)) {
      if (roads.get(edge) == null) result.add(edge);
    }
    List<EdgeId> list = new ArrayList<>(result);
    buildableEdgesCache.put(color, list);
    return list;
  }

  public Set<Resource> getPlayerPortResources(Color color) {
    if (playerPortResourcesCache.containsKey(color)) return playerPortResourcesCache.get(color);
    Set<Resource> resources = new HashSet<>();
    for (Map.Entry<Resource, Set<Integer>> entry : map.portNodes.entrySet()) {
      for (int nodeId : entry.getValue()) {
        if (isFriendlyNode(nodeId, color)) {
          resources.add(entry.getKey());
          break;
        }
      }
    }
    playerPortResourcesCache.put(color, resources);
    return resources;
  }

  public boolean hasGenericPort(Color color) {
    for (int nodeId : map.genericPortNodes) {
      if (isFriendlyNode(nodeId, color)) return true;
    }
    return false;
  }

  // ===== Longest road =====

  public List<List<EdgeId>> continuousRoadsByPlayer(Color color) {
    List<List<EdgeId>> paths = new ArrayList<>();
    for (Set<Integer> comp : connectedComponents.get(color)) {
      paths.add(longestAcyclicPath(comp, color));
    }
    return paths;
  }

  private List<EdgeId> longestAcyclicPath(Set<Integer> nodeSet, Color color) {
    List<EdgeId> best = Collections.emptyList();
    for (int start : nodeSet) {
      List<EdgeId> longest = dfsLongest(start, color);
      if (longest.size() > best.size()) best = longest;
    }
    return best;
  }

  private List<EdgeId> dfsLongest(int start, Color color) {
    record Frame(int node, List<EdgeId> path) {}
    List<Frame> agenda = new ArrayList<>();
    agenda.add(new Frame(start, new ArrayList<>()));
    List<EdgeId> best = Collections.emptyList();

    while (!agenda.isEmpty()) {
      Frame frame = agenda.remove(agenda.size() - 1);
      boolean leaf = true;
      for (int neighbor : graph.neighbors(frame.node)) {
        EdgeId edge = canonicalEdge(frame.node, neighbor);
        if (!isFriendlyRoad(edge, color)) continue;
        if (isEnemyNode(neighbor, color)) continue;
        if (!frame.path.contains(edge)) {
          List<EdgeId> newPath = new ArrayList<>(frame.path);
          newPath.add(edge);
          agenda.add(new Frame(neighbor, newPath));
          leaf = false;
        }
      }
      if (leaf && frame.path.size() > best.size()) best = frame.path;
    }
    return best;
  }

  // ===== DFS walk =====

  private Set<Integer> dfsWalk(int nodeId, Color color) {
    List<Integer> agenda = new ArrayList<>();
    agenda.add(nodeId);
    Set<Integer> visited = new HashSet<>();
    while (!agenda.isEmpty()) {
      int n = agenda.remove(agenda.size() - 1);
      visited.add(n);
      if (isEnemyNode(n, color)) continue;
      for (int v : graph.neighbors(n)) {
        if (!visited.contains(v)) {
          EdgeId edge = canonicalEdge(n, v);
          if (roads.get(edge) == color) agenda.add(v);
        }
      }
    }
    return visited;
  }

  // ===== Helpers =====

  public Color getNodeColor(int nodeId) {
    Color[] b = buildings.get(nodeId);
    return b != null ? b[0] : null;
  }

  public BuildingType getBuildingType(int nodeId) {
    return buildingTypes.get(nodeId);
  }

  public Color getEdgeColor(EdgeId edge) {
    return roads.get(edge);
  }

  public boolean isEnemyNode(int nodeId, Color color) {
    Color c = getNodeColor(nodeId);
    return c != null && c != color;
  }

  public boolean isFriendlyNode(int nodeId, Color color) {
    return getNodeColor(nodeId) == color;
  }

  public boolean isFriendlyRoad(EdgeId edge, Color color) {
    return roads.get(edge) == color;
  }

  public Board copy() {
    Board b = new Board(map);
    b.buildings.clear();
    b.buildings.putAll(this.buildings);
    b.buildingTypes.clear();
    b.buildingTypes.putAll(this.buildingTypes);
    b.roads.clear();
    b.roads.putAll(this.roads);
    b.connectedComponents.clear();
    for (Map.Entry<Color, List<Set<Integer>>> e : this.connectedComponents.entrySet()) {
      List<Set<Integer>> copy = new ArrayList<>();
      for (Set<Integer> s : e.getValue()) copy.add(new HashSet<>(s));
      b.connectedComponents.put(e.getKey(), copy);
    }
    b.boardBuildableIds.clear();
    b.boardBuildableIds.addAll(this.boardBuildableIds);
    b.roadLengths.clear();
    b.roadLengths.putAll(this.roadLengths);
    b.roadColor = this.roadColor;
    b.roadLength = this.roadLength;
    b.robberCoordinate = this.robberCoordinate;
    return b;
  }

  private static EdgeId canonicalEdge(int a, int b) {
    return a < b ? new EdgeId(a, b) : new EdgeId(b, a);
  }

  private static int otherNode(EdgeId edge, int node) {
    return edge.a() == node ? edge.b() : edge.a();
  }

  private Integer getComponentIndex(int nodeId, Color color) {
    List<Set<Integer>> comps = connectedComponents.get(color);
    for (int i = 0; i < comps.size(); i++) {
      if (comps.get(i).contains(nodeId)) return i;
    }
    return null;
  }

  private Set<Integer> findComponentContaining(int nodeId, Color color) {
    for (Set<Integer> comp : connectedComponents.get(color)) {
      if (comp.contains(nodeId)) return comp;
    }
    return null;
  }

  private void recomputeRoadLeader() {
    roadColor = null;
    roadLength = 0;
    for (Map.Entry<Color, Integer> e : roadLengths.entrySet()) {
      if (e.getValue() >= 5 && e.getValue() > roadLength) {
        roadColor = e.getKey();
        roadLength = e.getValue();
      }
    }
  }

  public record RoadResult(
      Color previousRoadColor, Color newRoadColor, Map<Color, Integer> roadLengths) {}
}

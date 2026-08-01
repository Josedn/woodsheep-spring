package io.bobba.catanatron.models;

import io.bobba.catanatron.enums.EdgeRef;
import io.bobba.catanatron.enums.NodeRef;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Minimal undirected graph — replaces networkx for board topology. */
public class Graph {

  private final Map<Integer, Set<Integer>> adj = new HashMap<>();

  public void addNode(int node) {
    adj.computeIfAbsent(node, k -> new HashSet<>());
  }

  public void addEdge(int a, int b) {
    adj.computeIfAbsent(a, k -> new HashSet<>()).add(b);
    adj.computeIfAbsent(b, k -> new HashSet<>()).add(a);
  }

  public Set<Integer> neighbors(int node) {
    return adj.getOrDefault(node, Collections.emptySet());
  }

  public Set<Integer> nodes() {
    return adj.keySet();
  }

  /** All edges as sorted (min,max) pairs — no duplicates. */
  public Set<EdgeId> edges() {
    Set<EdgeId> result = new HashSet<>();
    for (Map.Entry<Integer, Set<Integer>> e : adj.entrySet()) {
      int a = e.getKey();
      for (int b : e.getValue()) {
        if (a < b) result.add(new EdgeId(a, b));
      }
    }
    return result;
  }

  /** Edges incident to any node in the given set. */
  public Set<EdgeId> edges(Set<Integer> nodeSubset) {
    Set<EdgeId> result = new HashSet<>();
    for (int a : nodeSubset) {
      for (int b : neighbors(a)) {
        result.add(a < b ? new EdgeId(a, b) : new EdgeId(b, a));
      }
    }
    return result;
  }

  public static Graph fromMap(CatanMap map) {
    Graph g = new Graph();
    for (Object tile : map.tiles.values()) {
      Map<NodeRef, Integer> nodes = tileNodes(tile);
      Map<EdgeRef, EdgeId> edges = tileEdges(tile);
      if (nodes == null) continue;
      for (int n : nodes.values()) g.addNode(n);
      if (edges != null) {
        for (EdgeId e : edges.values()) g.addEdge(e.a(), e.b());
      }
    }
    return g;
  }

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
}

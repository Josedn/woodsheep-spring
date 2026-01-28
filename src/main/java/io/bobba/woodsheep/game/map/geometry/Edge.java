package io.bobba.woodsheep.game.map.geometry;

import java.util.Objects;

public final class Edge {
  private final int nodeA;
  private final int nodeB;

  public Edge(int nodeA, int nodeB) {
    if (nodeA <= nodeB) {
      this.nodeA = nodeA;
      this.nodeB = nodeB;
    } else {
      this.nodeA = nodeB;
      this.nodeB = nodeA;
    }
  }

  public int nodeA() {
    return nodeA;
  }

  public int nodeB() {
    return nodeB;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Edge edge)) return false;
    return nodeA == edge.nodeA && nodeB == edge.nodeB;
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeA, nodeB);
  }

  @Override
  public String toString() {
    return "(" + nodeA + "," + nodeB + ")";
  }

  public static long edgeKey(int nodeA, int nodeB) {
    int min = Math.min(nodeA, nodeB), max = Math.max(nodeA, nodeB);
    return (((long) min) << 32) | (max & 0xffffffffL);
  }
}

package io.bobba.woodsheep.game.map;

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
}

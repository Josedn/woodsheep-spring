package io.bobba.catanatron.models;

import io.bobba.catanatron.enums.EdgeRef;
import io.bobba.catanatron.enums.NodeRef;
import io.bobba.catanatron.enums.Resource;
import java.util.Map;

/** A land tile on the board. resource is null for the desert. number is null for the desert. */
public final class LandTile {

  public final int id;
  public final Resource resource; // null == desert
  public Integer number; // null == desert; mutable for spiral number assignment
  public final Map<NodeRef, Integer> nodes;
  public final Map<EdgeRef, EdgeId> edges;

  public LandTile(
      int id,
      Resource resource,
      Integer number,
      Map<NodeRef, Integer> nodes,
      Map<EdgeRef, EdgeId> edges) {
    this.id = id;
    this.resource = resource;
    this.number = number;
    this.nodes = nodes;
    this.edges = edges;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof LandTile other)) return false;
    return id == other.id;
  }
}

package com.catanatron.core.models;

import com.catanatron.core.enums.Direction;
import com.catanatron.core.enums.EdgeRef;
import com.catanatron.core.enums.NodeRef;
import com.catanatron.core.enums.Resource;
import java.util.Map;

/** A port tile. resource is null for a 3:1 port. */
public final class Port {

  public final int id;
  public final Resource resource; // null == 3:1 port
  public final Direction direction;
  public final Map<NodeRef, Integer> nodes;
  public final Map<EdgeRef, EdgeId> edges;

  public Port(
      int id,
      Resource resource,
      Direction direction,
      Map<NodeRef, Integer> nodes,
      Map<EdgeRef, EdgeId> edges) {
    this.id = id;
    this.resource = resource;
    this.direction = direction;
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
    if (!(obj instanceof Port other)) return false;
    return id == other.id;
  }
}

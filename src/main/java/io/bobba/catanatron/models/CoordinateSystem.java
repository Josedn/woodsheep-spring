package io.bobba.catanatron.models;

import io.bobba.catanatron.enums.Direction;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public final class CoordinateSystem {

  private CoordinateSystem() {}

  /** Total tile count for a board with the given number of layers (inclusive). */
  public static int numTilesFor(int layer) {
    if (layer == 0) return 1;
    return 6 * layer + numTilesFor(layer - 1);
  }

  /**
   * BFS expansion outward from (0,0,0) for the given number of layers. Mirrors
   * generate_coordinate_system() in coordinate_system.py.
   */
  public static Set<Coordinate> generateCoordinateSystem(int numLayers) {
    int target = numTilesFor(numLayers);

    Queue<Coordinate> agenda = new ArrayDeque<>();
    agenda.add(Coordinate.ORIGIN);
    Set<Coordinate> visited = new HashSet<>();

    while (visited.size() < target) {
      Coordinate node = agenda.poll();
      visited.add(node);

      for (Direction d : Direction.values()) {
        Coordinate neighbor = node.neighbor(d);
        if (!visited.contains(neighbor) && !agenda.contains(neighbor)) {
          agenda.add(neighbor);
        }
      }
    }
    return visited;
  }
}

package io.bobba.catanatron.models;

import io.bobba.catanatron.enums.Direction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Spiral {

  private Spiral() {}

  /**
   * Yields land-tile coordinates in coast-following spiral order from an outer-edge start
   * coordinate toward the center. Mirrors spiral_land_coordinates() in spiral.py.
   */
  public static List<Coordinate> spiralLandCoordinates(
      Map<Coordinate, Object> allTiles, Coordinate start) {
    if (!(allTiles.get(start) instanceof LandTile)) {
      throw new IllegalArgumentException("start must be a land tile");
    }

    boolean allNeighborsLand = true;
    for (Direction d : Direction.values()) {
      if (!(allTiles.get(start.neighbor(d)) instanceof LandTile)) {
        allNeighborsLand = false;
        break;
      }
    }
    if (allNeighborsLand) {
      throw new IllegalArgumentException("start must be on the outer edge of the land mass");
    }

    // Reversed direction list (matches Python's directions.reverse())
    Direction[] dirs = Direction.values();
    Direction[] reversed = new Direction[dirs.length];
    for (int i = 0; i < dirs.length; i++) {
      reversed[i] = dirs[dirs.length - 1 - i];
    }

    Direction direction = null;
    for (int i = 0; i < reversed.length; i++) {
      Direction candidate = reversed[i];
      Direction previous = reversed[(i - 1 + reversed.length) % reversed.length];
      boolean candidateIsLand = allTiles.get(start.neighbor(candidate)) instanceof LandTile;
      boolean previousIsLand = allTiles.get(start.neighbor(previous)) instanceof LandTile;
      if (candidateIsLand && !previousIsLand) {
        direction = candidate;
        break;
      }
    }

    if (direction == null) {
      throw new IllegalStateException("could not determine an initial coast-following direction");
    }

    long totalLand = allTiles.values().stream().filter(t -> t instanceof LandTile).count();
    Set<Coordinate> visited = new HashSet<>();
    List<Coordinate> result = new ArrayList<>();
    Coordinate coord = start;

    while (visited.size() < totalLand) {
      if (!visited.contains(coord)) {
        visited.add(coord);
        result.add(coord);
      }

      Coordinate next = coord.neighbor(direction);
      if (allTiles.get(next) instanceof LandTile && !visited.contains(next)) {
        coord = next;
        continue;
      }

      // rotate direction
      int idx = indexOf(reversed, direction);
      direction = reversed[(idx + 1) % reversed.length];
    }

    return result;
  }

  private static int indexOf(Direction[] arr, Direction d) {
    for (int i = 0; i < arr.length; i++) {
      if (arr[i] == d) return i;
    }
    throw new IllegalArgumentException("direction not found");
  }
}

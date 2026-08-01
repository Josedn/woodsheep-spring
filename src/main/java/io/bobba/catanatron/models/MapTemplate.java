package io.bobba.catanatron.models;

import io.bobba.catanatron.enums.Direction;
import io.bobba.catanatron.enums.Resource;
import java.util.List;
import java.util.Map;

/**
 * Immutable description of a board layout: which resources go where, what numbers are available,
 * and the tile topology.
 *
 * <p>topology values are one of: TileType.LAND → LandTile TileType.WATER → Water PortSpec → Port
 * with a given direction
 */
public record MapTemplate(
    List<Integer> numbers,
    List<Resource> portResources, // null entries = 3:1 port
    List<Resource> tileResources, // null entry = desert
    Map<Coordinate, Object> topology // Object = TileType | PortSpec
    ) {
  public enum TileType {
    LAND,
    WATER
  }

  public record PortSpec(Direction direction) {}
}

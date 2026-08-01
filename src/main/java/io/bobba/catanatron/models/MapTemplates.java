package io.bobba.catanatron.models;

import static io.bobba.catanatron.models.MapTemplate.PortSpec;
import static io.bobba.catanatron.models.MapTemplate.TileType.LAND;
import static io.bobba.catanatron.models.MapTemplate.TileType.WATER;

import io.bobba.catanatron.enums.Direction;
import io.bobba.catanatron.enums.Resource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Factory for the built-in map templates. */
public final class MapTemplates {

  private MapTemplates() {}

  public static final int[] BASE_NUMBERS_IN_SPIRAL_ORDER = {
    5, 2, 6, 3, 8, 10, 9, 12, 11, 4, 8, 10, 9, 4, 5, 6, 3, 11
  };

  public static MapTemplate miniMap() {
    Map<Coordinate, Object> topology = new LinkedHashMap<>();
    topology.put(new Coordinate(0, 0, 0), LAND);
    topology.put(new Coordinate(1, -1, 0), LAND);
    topology.put(new Coordinate(0, -1, 1), LAND);
    topology.put(new Coordinate(-1, 0, 1), LAND);
    topology.put(new Coordinate(-1, 1, 0), LAND);
    topology.put(new Coordinate(0, 1, -1), LAND);
    topology.put(new Coordinate(1, 0, -1), LAND);
    topology.put(new Coordinate(2, -2, 0), WATER);
    topology.put(new Coordinate(1, -2, 1), WATER);
    topology.put(new Coordinate(0, -2, 2), WATER);
    topology.put(new Coordinate(-1, -1, 2), WATER);
    topology.put(new Coordinate(-2, 0, 2), WATER);
    topology.put(new Coordinate(-2, 1, 1), WATER);
    topology.put(new Coordinate(-2, 2, 0), WATER);
    topology.put(new Coordinate(-1, 2, -1), WATER);
    topology.put(new Coordinate(0, 2, -2), WATER);
    topology.put(new Coordinate(1, 1, -2), WATER);
    topology.put(new Coordinate(2, 0, -2), WATER);
    topology.put(new Coordinate(2, -1, -1), WATER);

    return new MapTemplate(
        Arrays.asList(3, 4, 5, 6, 8, 9, 10),
        List.of(),
        Arrays.asList(
            Resource.WOOD,
            null,
            Resource.BRICK,
            Resource.SHEEP,
            Resource.WHEAT,
            Resource.WHEAT,
            Resource.ORE),
        topology);
  }

  public static MapTemplate baseMap() {
    Map<Coordinate, Object> topology = new LinkedHashMap<>();
    // center
    topology.put(new Coordinate(0, 0, 0), LAND);
    // first ring
    topology.put(new Coordinate(1, -1, 0), LAND);
    topology.put(new Coordinate(0, -1, 1), LAND);
    topology.put(new Coordinate(-1, 0, 1), LAND);
    topology.put(new Coordinate(-1, 1, 0), LAND);
    topology.put(new Coordinate(0, 1, -1), LAND);
    topology.put(new Coordinate(1, 0, -1), LAND);
    // second ring
    topology.put(new Coordinate(2, -2, 0), LAND);
    topology.put(new Coordinate(1, -2, 1), LAND);
    topology.put(new Coordinate(0, -2, 2), LAND);
    topology.put(new Coordinate(-1, -1, 2), LAND);
    topology.put(new Coordinate(-2, 0, 2), LAND);
    topology.put(new Coordinate(-2, 1, 1), LAND);
    topology.put(new Coordinate(-2, 2, 0), LAND);
    topology.put(new Coordinate(-1, 2, -1), LAND);
    topology.put(new Coordinate(0, 2, -2), LAND);
    topology.put(new Coordinate(1, 1, -2), LAND);
    topology.put(new Coordinate(2, 0, -2), LAND);
    topology.put(new Coordinate(2, -1, -1), LAND);
    // water / port ring
    topology.put(new Coordinate(3, -3, 0), new PortSpec(Direction.WEST));
    topology.put(new Coordinate(2, -3, 1), WATER);
    topology.put(new Coordinate(1, -3, 2), new PortSpec(Direction.NORTHWEST));
    topology.put(new Coordinate(0, -3, 3), WATER);
    topology.put(new Coordinate(-1, -2, 3), new PortSpec(Direction.NORTHWEST));
    topology.put(new Coordinate(-2, -1, 3), WATER);
    topology.put(new Coordinate(-3, 0, 3), new PortSpec(Direction.NORTHEAST));
    topology.put(new Coordinate(-3, 1, 2), WATER);
    topology.put(new Coordinate(-3, 2, 1), new PortSpec(Direction.EAST));
    topology.put(new Coordinate(-3, 3, 0), WATER);
    topology.put(new Coordinate(-2, 3, -1), new PortSpec(Direction.EAST));
    topology.put(new Coordinate(-1, 3, -2), WATER);
    topology.put(new Coordinate(0, 3, -3), new PortSpec(Direction.SOUTHEAST));
    topology.put(new Coordinate(1, 2, -3), WATER);
    topology.put(new Coordinate(2, 1, -3), new PortSpec(Direction.SOUTHWEST));
    topology.put(new Coordinate(3, 0, -3), WATER);
    topology.put(new Coordinate(3, -1, -2), new PortSpec(Direction.SOUTHWEST));
    topology.put(new Coordinate(3, -2, -1), WATER);

    List<Resource> portResources =
        Arrays.asList(
            Resource.WOOD,
            Resource.BRICK,
            Resource.SHEEP,
            Resource.WHEAT,
            Resource.ORE,
            null,
            null,
            null,
            null);
    List<Resource> tileResources =
        Arrays.asList(
            Resource.WOOD,
            Resource.WOOD,
            Resource.WOOD,
            Resource.WOOD,
            Resource.BRICK,
            Resource.BRICK,
            Resource.BRICK,
            Resource.SHEEP,
            Resource.SHEEP,
            Resource.SHEEP,
            Resource.SHEEP,
            Resource.WHEAT,
            Resource.WHEAT,
            Resource.WHEAT,
            Resource.WHEAT,
            Resource.ORE,
            Resource.ORE,
            Resource.ORE,
            null // desert
            );
    List<Integer> numbers =
        Arrays.asList(2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12);

    return new MapTemplate(numbers, portResources, tileResources, topology);
  }
}

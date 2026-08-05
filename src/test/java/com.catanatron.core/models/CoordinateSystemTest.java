package com.catanatron.core.models;

import static org.junit.jupiter.api.Assertions.*;

import com.catanatron.core.enums.Direction;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CoordinateSystemTest {

  @Test
  void numTilesForLayer0() {
    assertEquals(1, CoordinateSystem.numTilesFor(0));
  }

  @Test
  void numTilesForLayer1() {
    assertEquals(7, CoordinateSystem.numTilesFor(1));
  }

  @Test
  void numTilesForLayer2() {
    // Standard Catan board: 19 land tiles in 2 layers
    assertEquals(19, CoordinateSystem.numTilesFor(2));
  }

  @Test
  void numTilesForLayer3() {
    assertEquals(37, CoordinateSystem.numTilesFor(3));
  }

  @Test
  void generateCoordinateSystemLayer0ContainsOrigin() {
    Set<Coordinate> coords = CoordinateSystem.generateCoordinateSystem(0);
    assertEquals(1, coords.size());
    assertTrue(coords.contains(Coordinate.ORIGIN));
  }

  @Test
  void generateCoordinateSystemLayer2Has19Tiles() {
    Set<Coordinate> coords = CoordinateSystem.generateCoordinateSystem(2);
    assertEquals(19, coords.size());
  }

  @Test
  void allCoordsHaveZeroSum() {
    Set<Coordinate> coords = CoordinateSystem.generateCoordinateSystem(2);
    for (Coordinate c : coords) {
      assertEquals(0, c.x() + c.y() + c.z(), "Cube coordinate invariant violated: " + c);
    }
  }

  @Test
  void neighborInEachDirection() {
    Coordinate origin = Coordinate.ORIGIN;
    assertEquals(new Coordinate(1, -1, 0), origin.neighbor(Direction.EAST));
    assertEquals(new Coordinate(-1, 1, 0), origin.neighbor(Direction.WEST));
    assertEquals(new Coordinate(1, 0, -1), origin.neighbor(Direction.NORTHEAST));
    assertEquals(new Coordinate(-1, 0, 1), origin.neighbor(Direction.SOUTHWEST));
    assertEquals(new Coordinate(0, 1, -1), origin.neighbor(Direction.NORTHWEST));
    assertEquals(new Coordinate(0, -1, 1), origin.neighbor(Direction.SOUTHEAST));
  }

  @Test
  void addCoordinates() {
    Coordinate a = new Coordinate(1, -1, 0);
    Coordinate b = new Coordinate(0, -1, 1);
    assertEquals(new Coordinate(1, -2, 1), a.add(b));
  }

  @Test
  void toAxial() {
    Coordinate c = new Coordinate(2, -3, 1);
    int[] axial = c.toAxial();
    assertArrayEquals(new int[] {2, 1}, axial);
  }

  @Test
  void offsetRoundTrip() {
    Coordinate original = new Coordinate(1, -2, 1);
    double[] offset = original.toOffset();
    Coordinate back = Coordinate.fromOffset(offset[0], offset[1]);
    assertEquals(original, back);
  }
}

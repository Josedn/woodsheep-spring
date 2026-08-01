package io.bobba.catanatron.models;

import io.bobba.catanatron.enums.Direction;

/**
 * Cube coordinate on the hex grid. Invariant: x + y + z == 0. See
 * https://math.stackexchange.com/questions/2254655/hexagon-grid-coordinate-system
 */
public record Coordinate(int x, int y, int z) {

  public static final Coordinate ORIGIN = new Coordinate(0, 0, 0);

  /** Unit vectors for each of the six hex directions. */
  private static final int[][] UNIT_VECTORS = {
    // indexed by Direction.ordinal()
    // EAST
    {1, -1, 0},
    // SOUTHEAST
    {0, -1, 1},
    // SOUTHWEST
    {-1, 0, 1},
    // WEST
    {-1, 1, 0},
    // NORTHWEST
    {0, 1, -1},
    // NORTHEAST
    {1, 0, -1},
  };

  public Coordinate add(Coordinate other) {
    return new Coordinate(x + other.x, y + other.y, z + other.z);
  }

  public Coordinate neighbor(Direction d) {
    int[] v = UNIT_VECTORS[d.ordinal()];
    return new Coordinate(x + v[0], y + v[1], z + v[2]);
  }

  /** Cube → axial (q, r) as int[2]. */
  public int[] toAxial() {
    return new int[] {x, z};
  }

  /** Cube → offset (col, row) as double[2] to preserve half-column offsets. */
  public double[] toOffset() {
    double col = x + (z - (z & 1)) / 2.0;
    double row = z;
    return new double[] {col, row};
  }

  /** Offset (col, row) → cube coordinate. */
  public static Coordinate fromOffset(double col, double row) {
    int z = (int) row;
    double x = col - (row - (z & 1)) / 2.0;
    double y = -x - z;
    return new Coordinate((int) x, (int) y, z);
  }
}

package io.bobba.woodsheep.game.map.geometry;

import io.bobba.woodsheep.game.map.Coordinate;

public enum Direction {
  NORTHEAST(new Coordinate(1, 0, -1)),
  SOUTHWEST(new Coordinate(-1, 0, 1)),
  NORTHWEST(new Coordinate(0, 1, -1)),
  SOUTHEAST(new Coordinate(0, -1, 1)),
  EAST(new Coordinate(1, -1, 0)),
  WEST(new Coordinate(-1, 1, 0));

  public final Coordinate unitVector;

  Direction(Coordinate unitVector) {
    this.unitVector = unitVector;
  }
}

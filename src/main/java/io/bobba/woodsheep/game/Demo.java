package io.bobba.woodsheep.game;

import io.bobba.woodsheep.game.map.CatanMap;

public class Demo {
  public static void main(String[] args) {
    final var map = CatanMap.base();

    System.out.println(map.landTiles);
  }
}

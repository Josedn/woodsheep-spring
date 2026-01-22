package io.bobba.woodsheep.game.engine;

import io.bobba.woodsheep.game.map.CatanMap;
import io.bobba.woodsheep.game.model.BuildingType;
import io.bobba.woodsheep.game.model.PlayerColor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Board {
  private final CatanMap map;
  // nodeId -> (color, building)
  private final Map<Integer, Map.Entry<PlayerColor, BuildingType>> buildings = new HashMap<>();
  // edge (min,max) -> color
  private final Map<Long, PlayerColor> roads = new HashMap<>();
  private final Set<Integer> blockedNodes = new HashSet<>(); // distance-1 rule

  public Board(CatanMap map) {
    this.map = map;
  }
}

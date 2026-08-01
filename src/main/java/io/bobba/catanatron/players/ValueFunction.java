package io.bobba.catanatron.players;

import io.bobba.catanatron.enums.BuildingType;
import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.enums.DevCard;
import io.bobba.catanatron.enums.Resource;
import io.bobba.catanatron.game.Game;
import io.bobba.catanatron.models.Board;
import io.bobba.catanatron.models.Coordinate;
import io.bobba.catanatron.models.LandTile;
import io.bobba.catanatron.state.GameState;
import io.bobba.catanatron.state.StateFunctions;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Heuristic value function for a position. Mirrors base_fn() in value.py.
 *
 * <p>Computes expected production from adjacent tiles, hand synergy toward city/settlement,
 * blockability, longest road, dev cards, and army size.
 */
public final class ValueFunction {

  // 1 pip = 1/36; a tile number's probability expressed in "pips" (2.778% each)
  private static final double PROBA_POINT = 2.778 / 100.0;
  private static final int TRANSLATE_VARIETY = 4;

  private ValueFunction() {}

  public static double evaluate(Game game, Color p0Color, HeuristicWeights w) {
    GameState state = game.state;
    Board board = state.board;

    // ---- production ----
    double ourProd = effectiveProduction(board, state, p0Color);
    double enemyProd = 0;
    for (Color c : state.colors) {
      if (c != p0Color) enemyProd += effectiveProduction(board, state, c);
    }

    // ---- visible VPs ----
    int visibleVps = StateFunctions.getVisibleVictoryPoints(state, p0Color);

    // ---- buildable nodes ----
    int numBuildableNodes = board.buildableNodeIds(p0Color, false).size();

    // ---- tiles owned (blockability) ----
    Set<LandTile> ownedTiles = new HashSet<>();
    for (int nodeId : StateFunctions.getPlayerBuildings(state, p0Color, BuildingType.SETTLEMENT))
      ownedTiles.addAll(board.map.adjacentTiles.getOrDefault(nodeId, List.of()));
    for (int nodeId : StateFunctions.getPlayerBuildings(state, p0Color, BuildingType.CITY))
      ownedTiles.addAll(board.map.adjacentTiles.getOrDefault(nodeId, List.of()));
    int numTiles = ownedTiles.size();

    // ---- hand synergy ----
    int[] hand = state.playerState(p0Color).resourcesInHand;
    double distToCity =
        (Math.max(2 - hand[Resource.WHEAT.ordinal()], 0)
                + Math.max(3 - hand[Resource.ORE.ordinal()], 0))
            / 5.0;
    double distToSettlement =
        (Math.max(1 - hand[Resource.WHEAT.ordinal()], 0)
                + Math.max(1 - hand[Resource.SHEEP.ordinal()], 0)
                + Math.max(1 - hand[Resource.BRICK.ordinal()], 0)
                + Math.max(1 - hand[Resource.WOOD.ordinal()], 0))
            / 4.0;
    double handSynergy = (2.0 - distToCity - distToSettlement) / 2.0;

    // ---- other hand stats ----
    int numInHand = StateFunctions.playerNumResourceCards(state, p0Color);
    double discardPenalty = numInHand > 7 ? w.discardPenalty : 0;
    int numDevCards = StateFunctions.playerNumDevCards(state, p0Color);
    int knightsPlayed = StateFunctions.getPlayedDevCards(state, p0Color, DevCard.KNIGHT);

    // ---- longest road ----
    int longestRoadLength = StateFunctions.getLongestRoadLength(state, p0Color);
    double longestRoadFactor = (numBuildableNodes == 0) ? w.longestRoad : 0.1;

    return visibleVps * w.publicVps
        + ourProd * w.production
        + enemyProd * w.enemyProduction
        + handSynergy * w.handSynergy
        + numBuildableNodes * w.buildableNodes
        + numTiles * w.numTiles
        + numInHand * w.handResources
        + discardPenalty
        + longestRoadLength * longestRoadFactor
        + numDevCards * w.handDevs
        + knightsPlayed * w.armySize;
  }

  /** Expected resource production per roll for a player (production + variety bonus). */
  public static double effectiveProduction(Board board, GameState state, Color color) {
    double[] resourceProd = new double[5];

    Set<Integer> ownedNodes = new HashSet<>();
    for (int n : StateFunctions.getPlayerBuildings(state, color, BuildingType.SETTLEMENT))
      ownedNodes.add(n);
    for (int n : StateFunctions.getPlayerBuildings(state, color, BuildingType.CITY))
      ownedNodes.add(n);

    // Build a reverse map: tile id → coordinate (needed for robber check)
    Map<Integer, Coordinate> tileIdToCoord = new java.util.HashMap<>();
    for (Map.Entry<Coordinate, LandTile> e : board.map.landTiles.entrySet())
      tileIdToCoord.put(e.getValue().id, e.getKey());

    for (int nodeId : ownedNodes) {
      BuildingType bt = board.getBuildingType(nodeId);
      int multiplier = (bt == BuildingType.CITY) ? 2 : 1;
      for (LandTile tile : board.map.adjacentTiles.getOrDefault(nodeId, List.of())) {
        if (tile.resource == null || tile.number == null) continue;
        Coordinate coord = tileIdToCoord.get(tile.id);
        if (board.robberCoordinate.equals(coord)) continue;
        double proba = numberProbability(tile.number);
        resourceProd[tile.resource.ordinal()] += proba * multiplier;
      }
    }

    double sum = 0, variety = 0;
    for (double p : resourceProd) {
      sum += p;
      if (p > 0) variety++;
    }
    return sum + variety * TRANSLATE_VARIETY * PROBA_POINT;
  }

  private static double numberProbability(int number) {
    return (6 - Math.abs(7 - number)) / 36.0;
  }
}

package com.catanatron.core.players;

/**
 * Tunable weights for the heuristic value function. Mirrors DEFAULT_WEIGHTS / CONTENDER_WEIGHTS in
 * value.py.
 */
public class HeuristicWeights {

  public double publicVps = 3e14;
  public double production = 1e8;
  public double enemyProduction = -1e8;
  public double numTiles = 1;
  public double buildableNodes = 1e3;
  public double longestRoad = 10;
  public double handSynergy = 1e2;
  public double handResources = 1;
  public double discardPenalty = -5;
  public double handDevs = 10;
  public double armySize = 10.1;

  public static HeuristicWeights defaults() {
    return new HeuristicWeights();
  }

  public static HeuristicWeights contender() {
    HeuristicWeights w = new HeuristicWeights();
    w.publicVps = 300000000000001.94;
    w.production = 100000002.04188395;
    w.enemyProduction = -99999998.03389844;
    w.numTiles = 2.91440418;
    w.buildableNodes = 1001.86278466;
    w.longestRoad = 12.127388499999999;
    w.handSynergy = 102.40606877;
    w.handResources = 2.43644327;
    w.discardPenalty = -3.00141993;
    w.handDevs = 10.721669799999999;
    w.armySize = 12.93844622;
    return w;
  }
}

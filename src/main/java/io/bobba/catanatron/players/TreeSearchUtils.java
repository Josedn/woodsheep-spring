package io.bobba.catanatron.players;

import io.bobba.catanatron.enums.ActionType;
import io.bobba.catanatron.enums.BuildingType;
import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.enums.DevCard;
import io.bobba.catanatron.enums.Resource;
import io.bobba.catanatron.game.Game;
import io.bobba.catanatron.models.Coordinate;
import io.bobba.catanatron.models.LandTile;
import io.bobba.catanatron.state.Action;
import io.bobba.catanatron.state.ActionRecord;
import io.bobba.catanatron.state.StateFunctions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Utilities for tree-search players (AlphaBeta, MCTS). Mirrors tree_search_utils.py. */
public final class TreeSearchUtils {

  private static final Set<ActionType> DETERMINISTIC_ACTIONS =
      Set.of(
          ActionType.END_TURN,
          ActionType.BUILD_SETTLEMENT,
          ActionType.BUILD_ROAD,
          ActionType.BUILD_CITY,
          ActionType.PLAY_KNIGHT_CARD,
          ActionType.PLAY_YEAR_OF_PLENTY,
          ActionType.PLAY_ROAD_BUILDING,
          ActionType.MARITIME_TRADE,
          ActionType.DISCARD,
          ActionType.PLAY_MONOPOLY);

  private TreeSearchUtils() {}

  /** Returns [(gameCopy, probability)] for a deterministic action (just 1 outcome, proba=1). */
  private static List<Outcome> executeDeterministic(Game game, Action action) {
    Game copy = game.copy();
    copy.execute(action, false, null);
    return List.of(new Outcome(copy, 1.0));
  }

  /**
   * Returns all possible outcomes of applying {@code action} to {@code game}, each weighted by
   * probability. Probabilities sum to 1.
   */
  public static List<Outcome> executeSpectrum(Game game, Action action) {
    if (DETERMINISTIC_ACTIONS.contains(action.actionType())) {
      return executeDeterministic(game, action);
    }

    switch (action.actionType()) {
      case BUY_DEVELOPMENT_CARD -> {
        // Build the "visible deck" from enemy hands + the face-down bank deck
        List<DevCard> visibleDeck = new ArrayList<>(game.state.developmentListdeck);
        for (Color c : game.state.colors) {
          if (c == action.color()) continue;
          for (DevCard card : DevCard.ALL)
            visibleDeck.addAll(
                Collections.nCopies(StateFunctions.getDevCardsInHand(game.state, c, card), card));
        }
        if (visibleDeck.isEmpty()) return executeDeterministic(game, action);

        List<Outcome> results = new ArrayList<>();
        Set<DevCard> seen = new HashSet<>();
        for (DevCard card : visibleDeck) {
          if (!seen.add(card)) continue;
          long count = visibleDeck.stream().filter(d -> d == card).count();
          double proba = (double) count / visibleDeck.size();
          Action specific = new Action(action.color(), ActionType.BUY_DEVELOPMENT_CARD, card);
          Game copy = game.copy();
          try {
            copy.execute(specific, false, null);
          } catch (Exception ignored) {
          }
          results.add(new Outcome(copy, proba));
        }
        return results;
      }
      case ROLL -> {
        List<Outcome> results = new ArrayList<>();
        for (int roll = 2; roll <= 12; roll++) {
          int hi = (int) Math.ceil(roll / 2.0);
          int lo = roll - hi;
          int[] outcome = {lo, hi};
          Action specific = new Action(action.color(), ActionType.ROLL, outcome);
          Game copy = game.copy();
          copy.execute(specific, false, null);
          results.add(new Outcome(copy, numberProbability(roll)));
        }
        return results;
      }
      case MOVE_ROBBER -> {
        Object[] val = (Object[]) action.value();
        Color robbedColor = (Color) val[1];
        if (robbedColor == null) return executeDeterministic(game, action);

        int[] opponentHand = StateFunctions.getPlayerFreqdeck(game.state, robbedColor);
        int handSize = Arrays.stream(opponentHand).sum();
        if (handSize == 0) return executeDeterministic(game, action);

        List<Outcome> results = new ArrayList<>();
        for (Resource resource : Resource.ALL) {
          if (opponentHand[resource.ordinal()] <= 0) continue;
          double proba = (double) opponentHand[resource.ordinal()] / handSize;
          ActionRecord rec = new ActionRecord(action, resource);
          Game copy = game.copy();
          try {
            copy.execute(action, false, rec);
          } catch (Exception ignored) {
          }
          results.add(new Outcome(copy, proba));
        }
        if (results.isEmpty()) return executeDeterministic(game, action);
        return results;
      }
      default ->
          throw new IllegalArgumentException(
              "Unknown non-deterministic action: " + action.actionType());
    }
  }

  /**
   * Expands all given actions and returns a map from action to its list of (gameCopy, probability)
   * outcomes.
   */
  public static Map<Action, List<Outcome>> expandSpectrum(Game game, List<Action> actions) {
    Map<Action, List<Outcome>> children = new LinkedHashMap<>();
    for (Action action : actions) children.put(action, executeSpectrum(game, action));
    return children;
  }

  /** Prune action list for smarter tree search. Mirrors list_prunned_actions(). */
  public static List<Action> listPrunedActions(Game game) {
    Color currentColor = game.state.currentColor();
    List<Action> actions = new ArrayList<>(game.playableActions);
    Set<ActionType> types = new HashSet<>();
    for (Action a : actions) types.add(a.actionType());

    // Prune initial settlements at 1-tile locations
    if (types.contains(ActionType.BUILD_SETTLEMENT) && game.state.isInitialBuildPhase) {
      actions.removeIf(
          a ->
              a.actionType() == ActionType.BUILD_SETTLEMENT
                  && game.state
                          .board
                          .map
                          .adjacentTiles
                          .getOrDefault((Integer) a.value(), List.of())
                          .size()
                      == 1);
    }

    // Prune 4:1 trades when player has a 3:1 port
    if (types.contains(ActionType.MARITIME_TRADE)) {
      Set<Resource> portResources = game.state.board.getPlayerPortResources(currentColor);
      boolean hasThreeToOne = portResources.contains(null);
      actions.removeIf(
          a -> {
            if (a.actionType() != ActionType.MARITIME_TRADE) return false;
            Object[] tv = (Object[]) a.value();
            // value[3] is receiving resource; value[2] is the ratio (4 = 4:1)
            return hasThreeToOne && tv[3] != null && (int) tv[2] == 4;
          });
    }

    // Prune robber actions to most impactful
    if (types.contains(ActionType.MOVE_ROBBER)) {
      actions = pruneRobberActions(currentColor, game, actions);
    }

    return actions;
  }

  private static List<Action> pruneRobberActions(
      Color currentColor, Game game, List<Action> actions) {
    // Find enemy color (simplified: first color that isn't current)
    Color enemyColor = null;
    for (Color c : game.state.colors) {
      if (c != currentColor) {
        enemyColor = c;
        break;
      }
    }
    if (enemyColor == null) return actions;

    final Color enemy = enemyColor;
    Set<LandTile> enemyOwnedTiles = new HashSet<>();
    for (int nodeId : StateFunctions.getPlayerBuildings(game.state, enemy, BuildingType.SETTLEMENT))
      enemyOwnedTiles.addAll(game.state.board.map.adjacentTiles.getOrDefault(nodeId, List.of()));
    for (int nodeId : StateFunctions.getPlayerBuildings(game.state, enemy, BuildingType.CITY))
      enemyOwnedTiles.addAll(game.state.board.map.adjacentTiles.getOrDefault(nodeId, List.of()));

    // Collect robber moves that hit an enemy tile
    Set<Action> robberMoves = new HashSet<>();
    Map<Coordinate, LandTile> landTiles = game.state.board.map.landTiles;
    for (Action a : actions) {
      if (a.actionType() != ActionType.MOVE_ROBBER) continue;
      Object[] val = (Object[]) a.value();
      Coordinate coord = (Coordinate) val[0];
      LandTile tile = landTiles.get(coord);
      if (tile != null && enemyOwnedTiles.contains(tile)) robberMoves.add(a);
    }

    if (robberMoves.isEmpty()) return actions;

    // Pick the most impactful robber move by impact on enemy production
    Action best = null;
    double bestScore = Double.NEGATIVE_INFINITY;
    for (Action a : robberMoves) {
      Game copy = game.copy();
      copy.execute(a, false, null);
      double enemyProd = ValueFunction.effectiveProduction(copy.state.board, copy.state, enemy);
      double ourProd =
          ValueFunction.effectiveProduction(copy.state.board, copy.state, currentColor);
      double score = enemyProd - ourProd;
      if (score > bestScore) {
        bestScore = score;
        best = a;
      }
    }

    final Action mostImpactful = best;
    actions.removeIf(a -> a.actionType() == ActionType.MOVE_ROBBER && !a.equals(mostImpactful));
    return actions;
  }

  private static double numberProbability(int number) {
    return (6 - Math.abs(7 - number)) / 36.0;
  }

  /** A (game copy, probability) pair returned by executeSpectrum. */
  public record Outcome(Game game, double proba) {}
}

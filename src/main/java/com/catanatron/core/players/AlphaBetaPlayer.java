package com.catanatron.core.players;

import com.catanatron.core.enums.Color;
import com.catanatron.core.game.Game;
import com.catanatron.core.state.Action;
import com.catanatron.core.state.Player;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * AlphaBeta minimax with expected-value expansion over stochastic outcomes. Mirrors AlphaBetaPlayer
 * in minimax.py.
 */
public class AlphaBetaPlayer implements Player {

  private static final int DEFAULT_DEPTH = 2;
  private static final long MAX_SEARCH_TIME_MS = 20_000;

  private final Color color;
  private final int depth;
  private final boolean pruning;
  private final HeuristicWeights weights;
  private final double epsilon;
  private final Random rng;

  public AlphaBetaPlayer(Color color) {
    this(color, DEFAULT_DEPTH, false, HeuristicWeights.defaults(), 0.0, new Random());
  }

  public AlphaBetaPlayer(Color color, int depth, boolean pruning) {
    this(color, depth, pruning, HeuristicWeights.defaults(), 0.0, new Random());
  }

  public AlphaBetaPlayer(
      Color color,
      int depth,
      boolean pruning,
      HeuristicWeights weights,
      double epsilon,
      Random rng) {
    this.color = color;
    this.depth = depth;
    this.pruning = pruning;
    this.weights = weights;
    this.epsilon = epsilon;
    this.rng = rng;
  }

  @Override
  public Color getColor() {
    return color;
  }

  @Override
  public boolean isBot() {
    return true;
  }

  protected List<Action> getActions(Game game) {
    return pruning ? TreeSearchUtils.listPrunedActions(game) : game.playableActions;
  }

  @Override
  public Action decide(Game game, List<Action> playableActions) {
    List<Action> actions = getActions(game);
    if (actions.size() == 1) return actions.get(0);

    if (epsilon > 0 && rng.nextDouble() < epsilon)
      return playableActions.get(rng.nextInt(playableActions.size()));

    long deadline = System.currentTimeMillis() + MAX_SEARCH_TIME_MS;
    DebugStateNode root =
        new DebugStateNode(String.valueOf(game.state.actionRecords.size()), color);
    Object[] result =
        alphabeta(
            game.copy(), depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, deadline, root);
    Action chosen = (Action) result[0];
    return chosen != null ? chosen : playableActions.get(0);
  }

  /** Returns {action_or_null, value}. */
  protected Object[] alphabeta(
      Game game, int d, double alpha, double beta, long deadline, DebugStateNode node) {
    if (d == 0 || game.winningColor() != null || System.currentTimeMillis() >= deadline) {
      double value = ValueFunction.evaluate(game, color, weights);
      node.expectedValue = value;
      return new Object[] {null, value};
    }

    boolean maximizing = game.state.currentColor() == color;
    List<Action> actions = getActions(game);
    Map<Action, List<TreeSearchUtils.Outcome>> actionOutcomes =
        TreeSearchUtils.expandSpectrum(game, actions);

    if (maximizing) {
      Action bestAction = null;
      double bestValue = Double.NEGATIVE_INFINITY;
      int i = 0;
      for (Map.Entry<Action, List<TreeSearchUtils.Outcome>> e : actionOutcomes.entrySet()) {
        Action action = e.getKey();
        DebugActionNode actionNode = new DebugActionNode(action);

        double expected = 0;
        int j = 0;
        for (TreeSearchUtils.Outcome outcome : e.getValue()) {
          DebugStateNode child =
              new DebugStateNode(
                  node.label + " " + i + " " + j, outcome.game().state.currentColor());
          Object[] res = alphabeta(outcome.game(), d - 1, alpha, beta, deadline, child);
          expected += outcome.proba() * (double) res[1];
          actionNode.children.add(child);
          actionNode.probas.add(outcome.proba());
          j++;
        }
        actionNode.expectedValue = expected;
        node.children.add(actionNode);

        if (expected > bestValue) {
          bestValue = expected;
          bestAction = action;
        }
        alpha = Math.max(alpha, bestValue);
        if (alpha >= beta) break;
        i++;
      }
      node.expectedValue = bestValue;
      return new Object[] {bestAction, bestValue};
    } else {
      Action bestAction = null;
      double bestValue = Double.POSITIVE_INFINITY;
      int i = 0;
      for (Map.Entry<Action, List<TreeSearchUtils.Outcome>> e : actionOutcomes.entrySet()) {
        Action action = e.getKey();
        DebugActionNode actionNode = new DebugActionNode(action);

        double expected = 0;
        int j = 0;
        for (TreeSearchUtils.Outcome outcome : e.getValue()) {
          DebugStateNode child =
              new DebugStateNode(
                  node.label + " " + i + " " + j, outcome.game().state.currentColor());
          Object[] res = alphabeta(outcome.game(), d - 1, alpha, beta, deadline, child);
          expected += outcome.proba() * (double) res[1];
          actionNode.children.add(child);
          actionNode.probas.add(outcome.proba());
          j++;
        }
        actionNode.expectedValue = expected;
        node.children.add(actionNode);

        if (expected < bestValue) {
          bestValue = expected;
          bestAction = action;
        }
        beta = Math.min(beta, bestValue);
        if (beta <= alpha) break;
        i++;
      }
      node.expectedValue = bestValue;
      return new Object[] {bestAction, bestValue};
    }
  }

  @Override
  public String toString() {
    return "AlphaBetaPlayer:" + color + "(depth=" + depth + ",pruning=" + pruning + ")";
  }

  // ---- Debug tree nodes ----

  public static class DebugStateNode {
    public final String label;
    public final Color color;
    public double expectedValue;
    public final List<DebugActionNode> children = new java.util.ArrayList<>();

    public DebugStateNode(String label, Color color) {
      this.label = label;
      this.color = color;
    }
  }

  public static class DebugActionNode {
    public final Action action;
    public double expectedValue;
    public final List<DebugStateNode> children = new java.util.ArrayList<>();
    public final List<Double> probas = new java.util.ArrayList<>();

    public DebugActionNode(Action action) {
      this.action = action;
    }
  }
}

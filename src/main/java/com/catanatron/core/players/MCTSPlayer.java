package com.catanatron.core.players;

import com.catanatron.core.enums.Color;
import com.catanatron.core.game.Game;
import com.catanatron.core.state.Action;
import com.catanatron.core.state.Player;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Monte Carlo Tree Search player with UCB1 selection and random playouts. Mirrors MCTSPlayer /
 * StateNode in mcts.py.
 */
public class MCTSPlayer implements Player {

  private static final int DEFAULT_SIMULATIONS = 10;
  private static final double EXP_C = Math.sqrt(2);
  private static final double EPSILON = 1e-8;

  private final Color color;
  private final int numSimulations;
  private final boolean pruning;
  private final Random rng;

  public MCTSPlayer(Color color) {
    this(color, DEFAULT_SIMULATIONS, false, new Random());
  }

  public MCTSPlayer(Color color, int numSimulations, boolean pruning, Random rng) {
    this.color = color;
    this.numSimulations = numSimulations;
    this.pruning = pruning;
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

  @Override
  public Action decide(Game game, List<Action> playableActions) {
    List<Action> actions = pruning ? TreeSearchUtils.listPrunedActions(game) : playableActions;
    if (actions.size() == 1) return actions.get(0);

    StateNode root = new StateNode(color, game.copy(), null, pruning, rng);
    for (int i = 0; i < numSimulations; i++) root.runSimulation();
    return root.chooseBestAction();
  }

  @Override
  public String toString() {
    return "MCTSPlayer:" + color + "(" + numSimulations + ":" + pruning + ")";
  }

  // ---- Inner tree node ----

  static class StateNode {
    final Color color;
    final Game game;
    final StateNode parent;
    final boolean pruning;
    final Random rng;
    final int level;

    // action → list of (child StateNode, probability)
    Map<Action, List<ChildEntry>> children = new LinkedHashMap<>();

    double wins = 0;
    int visits = 0;

    StateNode(Color color, Game game, StateNode parent, boolean pruning, Random rng) {
      this.color = color;
      this.game = game;
      this.parent = parent;
      this.pruning = pruning;
      this.rng = rng;
      this.level = (parent == null) ? 0 : parent.level + 1;
    }

    void runSimulation() {
      StateNode tmp = this;
      tmp.visits++;
      while (!tmp.isLeaf()) {
        tmp = tmp.select();
        tmp.visits++;
      }

      Color result;
      if (!tmp.isTerminal()) {
        tmp.expand();
        tmp = tmp.select();
        tmp.visits++;
        result = tmp.playout();
      } else {
        result = game.winningColor();
      }
      tmp.backpropagate(result == this.color);
    }

    boolean isLeaf() {
      return children.isEmpty();
    }

    boolean isTerminal() {
      return game.winningColor() != null;
    }

    void expand() {
      List<Action> actions =
          pruning ? TreeSearchUtils.listPrunedActions(game) : game.playableActions;
      for (Action action : actions) {
        List<TreeSearchUtils.Outcome> outcomes = TreeSearchUtils.executeSpectrum(game, action);
        List<ChildEntry> entries = new ArrayList<>();
        for (TreeSearchUtils.Outcome o : outcomes)
          entries.add(
              new ChildEntry(new StateNode(color, o.game(), this, pruning, rng), o.proba()));
        children.put(action, entries);
      }
    }

    StateNode select() {
      Action best = chooseBestAction();
      List<ChildEntry> entries = children.get(best);
      // weighted random selection among outcome nodes
      double total = entries.stream().mapToDouble(e -> e.proba).sum();
      double r = rng.nextDouble() * total;
      double acc = 0;
      for (ChildEntry e : entries) {
        acc += e.proba;
        if (r <= acc) return e.node;
      }
      return entries.get(entries.size() - 1).node;
    }

    Action chooseBestAction() {
      Action best = null;
      double bestScore = Double.NEGATIVE_INFINITY;
      for (Action action : game.playableActions) {
        double score = actionExpectedScore(action);
        if (score > bestScore) {
          bestScore = score;
          best = action;
        }
      }
      return best;
    }

    private double actionExpectedScore(Action action) {
      List<ChildEntry> entries = children.getOrDefault(action, List.of());
      double score = 0;
      for (ChildEntry e : entries) {
        score +=
            e.proba
                * (e.node.wins / (e.node.visits + EPSILON)
                    + EXP_C * Math.sqrt(Math.log(visits + EPSILON) / (e.node.visits + EPSILON)));
      }
      return score;
    }

    Color playout() {
      List<Player> randomPlayers = new ArrayList<>();
      for (Color c : game.state.colors) randomPlayers.add(new RandomPlayer(c, rng));
      Game playout = game.copyWithPlayers(randomPlayers);
      playout.play();
      return playout.winningColor();
    }

    void backpropagate(boolean win) {
      if (win) this.wins++;
      StateNode tmp = this;
      while (tmp.parent != null) {
        tmp = tmp.parent;
        if (win) tmp.wins++;
      }
    }

    record ChildEntry(StateNode node, double proba) {}
  }
}

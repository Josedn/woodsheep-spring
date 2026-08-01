package io.bobba.catanatron.players;

import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.game.Game;
import io.bobba.catanatron.state.Action;
import io.bobba.catanatron.state.Player;
import java.util.List;
import java.util.Random;

/**
 * Greedy 1-ply lookahead using the heuristic value function. Mirrors ValueFunctionPlayer in
 * value.py.
 */
public class ValueFunctionPlayer implements Player {

  private final Color color;
  private final HeuristicWeights weights;
  private final double epsilon;
  private final Random rng;

  public ValueFunctionPlayer(Color color) {
    this(color, HeuristicWeights.defaults(), 0.0, new Random());
  }

  public ValueFunctionPlayer(Color color, HeuristicWeights weights) {
    this(color, weights, 0.0, new Random());
  }

  public ValueFunctionPlayer(Color color, HeuristicWeights weights, double epsilon, Random rng) {
    this.color = color;
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

  @Override
  public Action decide(Game game, List<Action> playableActions) {
    if (playableActions.size() == 1) return playableActions.get(0);

    if (epsilon > 0 && rng.nextDouble() < epsilon)
      return playableActions.get(rng.nextInt(playableActions.size()));

    Action best = null;
    double bestValue = Double.NEGATIVE_INFINITY;
    for (Action action : playableActions) {
      Game copy = game.copy();
      copy.execute(action, false, null);
      double value = ValueFunction.evaluate(copy, color, weights);
      if (value > bestValue) {
        bestValue = value;
        best = action;
      }
    }
    return best;
  }

  @Override
  public String toString() {
    return "ValueFunctionPlayer:" + color;
  }
}

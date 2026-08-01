package io.bobba.catanatron.players;

import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.game.Game;
import io.bobba.catanatron.state.Action;
import io.bobba.catanatron.state.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * For each playable action, plays N random games and picks the action with the most wins. Mirrors
 * GreedyPlayoutsPlayer in playouts.py.
 */
public class GreedyPlayoutsPlayer implements Player {

  private static final int DEFAULT_NUM_PLAYOUTS = 25;

  private final Color color;
  private final int numPlayouts;
  private final Random rng;

  public GreedyPlayoutsPlayer(Color color) {
    this(color, DEFAULT_NUM_PLAYOUTS, new Random());
  }

  public GreedyPlayoutsPlayer(Color color, int numPlayouts, Random rng) {
    this.color = color;
    this.numPlayouts = numPlayouts;
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

    // Build a list of random players with matching colors for playouts
    List<Player> randomPlayers = new ArrayList<>();
    for (Color c : game.state.colors) randomPlayers.add(new RandomPlayer(c, rng));

    Action best = null;
    int maxWins = -1;
    for (Action action : playableActions) {
      Game afterAction = game.copy();
      afterAction.execute(action, false, null);

      int wins = 0;
      for (int i = 0; i < numPlayouts; i++) {
        Game playout = afterAction.copyWithPlayers(randomPlayers);
        playout.play();
        if (color.equals(playout.winningColor())) wins++;
      }
      if (wins > maxWins) {
        maxWins = wins;
        best = action;
      }
    }
    return best;
  }

  @Override
  public String toString() {
    return "GreedyPlayoutsPlayer:" + color + "(" + numPlayouts + ")";
  }
}

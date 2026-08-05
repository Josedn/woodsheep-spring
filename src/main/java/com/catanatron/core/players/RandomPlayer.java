package com.catanatron.core.players;

import com.catanatron.core.enums.Color;
import com.catanatron.core.state.Action;
import com.catanatron.core.state.GameState;
import java.util.List;
import java.util.Random;

/** Picks a uniformly random action. */
public class RandomPlayer implements com.catanatron.core.state.Player {

  private final Color color;
  private final Random rng;

  public RandomPlayer(Color color) {
    this(color, new Random());
  }

  public RandomPlayer(Color color, Random rng) {
    this.color = color;
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
  public Action decide(GameState state, List<Action> playableActions) {
    return playableActions.get(rng.nextInt(playableActions.size()));
  }

  @Override
  public String toString() {
    return "RandomPlayer:" + color;
  }
}

package io.bobba.catanatron.players;

import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.state.Action;
import io.bobba.catanatron.state.GameState;
import io.bobba.catanatron.state.Player;
import java.util.List;

/** Always picks the first available action. */
public class SimplePlayer implements Player {

  private final Color color;

  public SimplePlayer(Color color) {
    this.color = color;
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
    return playableActions.get(0);
  }

  @Override
  public String toString() {
    return "SimplePlayer:" + color;
  }
}

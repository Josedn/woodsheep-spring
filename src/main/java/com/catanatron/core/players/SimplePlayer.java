package com.catanatron.core.players;

import com.catanatron.core.enums.Color;
import com.catanatron.core.state.Action;
import com.catanatron.core.state.GameState;
import java.util.List;

/** Always picks the first available action. */
public class SimplePlayer implements com.catanatron.core.state.Player {

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

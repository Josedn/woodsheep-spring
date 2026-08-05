package com.catanatron.core.players;

import com.catanatron.core.enums.ActionType;
import com.catanatron.core.enums.Color;
import com.catanatron.core.state.Action;
import com.catanatron.core.state.GameState;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Picks at random but skews toward high-value action types. Mirrors WeightedRandomPlayer in
 * weighted_random.py.
 */
public class WeightedRandomPlayer implements com.catanatron.core.state.Player {

  private static final int WEIGHT_CITY = 10000;
  private static final int WEIGHT_SETTLEMENT = 1000;
  private static final int WEIGHT_DEV_CARD = 100;
  private static final int WEIGHT_DEFAULT = 1;

  private final Color color;
  private final Random rng;

  public WeightedRandomPlayer(Color color) {
    this(color, new Random());
  }

  public WeightedRandomPlayer(Color color, Random rng) {
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
    List<Action> bloated = new ArrayList<>();
    for (Action action : playableActions) {
      int weight = weightFor(action.actionType());
      for (int i = 0; i < weight; i++) bloated.add(action);
    }
    return bloated.get(rng.nextInt(bloated.size()));
  }

  private static int weightFor(ActionType type) {
    return switch (type) {
      case BUILD_CITY -> WEIGHT_CITY;
      case BUILD_SETTLEMENT -> WEIGHT_SETTLEMENT;
      case BUY_DEVELOPMENT_CARD -> WEIGHT_DEV_CARD;
      default -> WEIGHT_DEFAULT;
    };
  }

  @Override
  public String toString() {
    return "WeightedRandomPlayer:" + color;
  }
}

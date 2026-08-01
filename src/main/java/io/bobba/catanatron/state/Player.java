package io.bobba.catanatron.state;

import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.game.Game;
import java.util.List;

/** Interface for player decision logic, mirroring Player in player.py. */
public interface Player {

  Color getColor();

  boolean isBot();

  /**
   * Choose one action from playableActions. Override this for state-only decisions. Tree-search
   * players should override {@link #decide(Game, List)} instead.
   */
  default Action decide(GameState state, List<Action> playableActions) {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " must implement decide()");
  }

  /**
   * Game-aware decision hook — override this when the player needs to copy the game (e.g.
   * AlphaBeta, MCTS, GreedyPlayouts). The default delegates to the state-only variant.
   */
  default Action decide(Game game, List<Action> playableActions) {
    return decide(game.state, playableActions);
  }

  /** Called between games to reset any per-game learned state. */
  default void resetState() {}
}

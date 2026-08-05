package com.catanatron.core.game;

import com.catanatron.core.state.Action;

/** Hook interface for observing game lifecycle events. Mirrors GameAccumulator in game.py. */
public interface GameAccumulator {

  /** Called once the board is decided but before any actions. */
  default void before(Game game) {}

  /** Called before each action is applied, with the pre-action game state. */
  default void step(Game game, Action action) {}

  /** Called when the game is finished (winning_color may be null if turn limit hit). */
  default void after(Game game) {}
}

package io.bobba.catanatron.game;

import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.models.CatanMap;
import io.bobba.catanatron.models.MapTemplates;
import io.bobba.catanatron.state.Action;
import io.bobba.catanatron.state.ActionRecord;
import io.bobba.catanatron.state.GameState;
import io.bobba.catanatron.state.Player;
import io.bobba.catanatron.state.StateFunctions;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Thin wrapper around GameState that drives the game loop. Mirrors game.py Game class.
 *
 * <p>Player references live here (not in GameState) so that GameState remains a pure data struct
 * that is easy to copy and inspect.
 */
public class Game {

  public static final int TURNS_LIMIT = 1000;

  public final String id;
  public final long seed;
  public final int vpsToWin;
  public GameState state;
  public List<Action> playableActions;

  final List<Player> players;
  private final Random rng;

  public Game(
      List<Player> players,
      long seed,
      int discardLimit,
      boolean friendlyRobber,
      int vpsToWin,
      CatanMap map,
      boolean officialSpiral) {
    this.seed = seed;
    this.rng = new Random(seed);
    this.id = UUID.randomUUID().toString();
    this.vpsToWin = vpsToWin;
    this.players = new ArrayList<>(players);

    CatanMap resolvedMap =
        (map != null)
            ? map
            : CatanMap.fromTemplate(MapTemplates.baseMap(), officialSpiral, new Random(seed));
    this.state =
        new GameState(players, resolvedMap, discardLimit, friendlyRobber, officialSpiral, rng);
    this.playableActions = Actions.generatePlayableActions(state);
  }

  /** Convenience constructor with common defaults. */
  public Game(List<Player> players) {
    this(players, new Random().nextLong(), 7, false, 10, null, true);
  }

  // Private copy constructor
  private Game(Game src) {
    this.seed = src.seed;
    this.rng = new Random(src.seed);
    this.id = src.id;
    this.vpsToWin = src.vpsToWin;
    this.players = new ArrayList<>(src.players); // mutable copy so copyWithPlayers() works
    this.state = src.state.copy();
    this.playableActions = new ArrayList<>(src.playableActions);
  }

  // ===== Game loop =====

  public Color play() {
    return play(List.of());
  }

  public Color play(List<GameAccumulator> accumulators) {
    for (GameAccumulator acc : accumulators) acc.before(this);
    while (winningColor() == null && state.numTurns < TURNS_LIMIT) {
      playTick(accumulators);
    }
    for (GameAccumulator acc : accumulators) acc.after(this);
    return winningColor();
  }

  public ActionRecord playTick() {
    return playTick(List.of());
  }

  public ActionRecord playTick(List<GameAccumulator> accumulators) {
    Color current = state.currentColor();
    Player player = playerForColor(current);
    Action action = player.decide(this, playableActions);

    for (GameAccumulator acc : accumulators) acc.step(this, action);
    return execute(action, true, null);
  }

  public ActionRecord execute(Action action, boolean validateAction, ActionRecord record) {
    if (validateAction && !Actions.isValidAction(playableActions, state, action)) {
      throw new IllegalArgumentException(action + " not playable. playable=" + playableActions);
    }
    ActionRecord result = ApplyAction.applyAction(state, action, record, rng);
    playableActions = Actions.generatePlayableActions(state);
    return result;
  }

  public Color winningColor() {
    Color winner = null;
    for (Color c : state.colors) {
      if (StateFunctions.getActualVictoryPoints(state, c) >= vpsToWin) winner = c;
    }
    return winner;
  }

  public Game copy() {
    return new Game(this);
  }

  /** Returns a copy of this game with a different player list (for playouts). */
  public Game copyWithPlayers(List<Player> newPlayers) {
    Game g = new Game(this);
    g.players.clear();
    g.players.addAll(newPlayers);
    return g;
  }

  private Player playerForColor(Color color) {
    for (Player p : players) if (p.getColor() == color) return p;
    throw new IllegalStateException("No player for color " + color);
  }
}

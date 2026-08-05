package com.catanatron.core.models;

import static org.junit.jupiter.api.Assertions.*;

import com.catanatron.core.enums.ActionType;
import com.catanatron.core.enums.BuildingType;
import com.catanatron.core.enums.Color;
import com.catanatron.core.game.Game;
import com.catanatron.core.players.AlphaBetaPlayer;
import com.catanatron.core.players.GreedyPlayoutsPlayer;
import com.catanatron.core.players.HeuristicWeights;
import com.catanatron.core.players.MCTSPlayer;
import com.catanatron.core.players.RandomPlayer;
import com.catanatron.core.players.SimplePlayer;
import com.catanatron.core.players.TreeSearchUtils;
import com.catanatron.core.players.ValueFunction;
import com.catanatron.core.players.ValueFunctionPlayer;
import com.catanatron.core.players.WeightedRandomPlayer;
import com.catanatron.core.state.Action;
import com.catanatron.core.state.Player;
import com.catanatron.core.state.StateFunctions;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PlayersTest {

  private static final Color RED = Color.RED;
  private static final Color BLUE = Color.BLUE;

  private Game twoPlayerGame(Player p1, Player p2) {
    return new Game(List.of(p1, p2), 42L, 7, false, 10, null, true);
  }

  // ---- SimplePlayer ----

  @Test
  void simplePlayerAlwaysPicksFirst() {
    SimplePlayer p = new SimplePlayer(RED);
    Game game = twoPlayerGame(p, new SimplePlayer(BLUE));
    List<Action> actions = game.playableActions;
    assertSame(actions.get(0), p.decide(game.state, actions));
  }

  @Test
  void simplePlayerPlaysFullGame() {
    SimplePlayer p1 = new SimplePlayer(RED);
    SimplePlayer p2 = new SimplePlayer(BLUE);
    // Use vpsToWin=2: each player places 2 initial settlements (2 VP), so the game ends quickly.
    Game game = new Game(List.of(p1, p2), 42L, 7, false, 2, null, true);
    Color winner = game.play();
    assertNotNull(winner);
  }

  // ---- RandomPlayer ----

  @Test
  void randomPlayerPicksValidAction() {
    RandomPlayer p = new RandomPlayer(RED, new Random(1));
    Game game = twoPlayerGame(p, new SimplePlayer(BLUE));
    List<Action> actions = game.playableActions;
    Action chosen = p.decide(game.state, actions);
    assertTrue(actions.contains(chosen));
  }

  @Test
  void randomPlayerFullGame() {
    Game game =
        twoPlayerGame(new RandomPlayer(RED, new Random(7)), new RandomPlayer(BLUE, new Random(13)));
    assertNotNull(game.play());
  }

  // ---- WeightedRandomPlayer ----

  @Test
  void weightedRandomPlayerPicksValidAction() {
    WeightedRandomPlayer p = new WeightedRandomPlayer(RED, new Random(99));
    Game game = twoPlayerGame(p, new SimplePlayer(BLUE));
    List<Action> actions = game.playableActions;
    Action chosen = p.decide(game.state, actions);
    assertTrue(actions.contains(chosen));
  }

  // ---- ValueFunction ----

  @Test
  void effectiveProductionIsNonNegative() {
    Game game = twoPlayerGame(new SimplePlayer(RED), new SimplePlayer(BLUE));
    // Advance past initial placement so buildings exist
    for (int i = 0; i < 8; i++) game.playTick();
    double prod = ValueFunction.effectiveProduction(game.state.board, game.state, RED);
    assertTrue(prod >= 0.0);
  }

  @Test
  void valueFunctionIgnoresRobbedTile() {
    // Place robber on a tile owned by RED and verify production drops
    Game game = twoPlayerGame(new SimplePlayer(RED), new SimplePlayer(BLUE));
    for (int i = 0; i < 8; i++) game.playTick();

    double prodBefore = ValueFunction.effectiveProduction(game.state.board, game.state, RED);
    // Move robber to a tile adjacent to RED's settlement
    List<Integer> settlements =
        StateFunctions.getPlayerBuildings(game.state, RED, BuildingType.SETTLEMENT);
    if (!settlements.isEmpty()) {
      List<LandTile> adjTiles =
          game.state.board.map.adjacentTiles.getOrDefault(settlements.get(0), List.of());
      if (!adjTiles.isEmpty()) {
        // Find coordinate for that tile
        for (var e : game.state.board.map.landTiles.entrySet()) {
          if (e.getValue() == adjTiles.get(0)) {
            game.state.board.robberCoordinate = e.getKey();
            break;
          }
        }
        double prodAfter = ValueFunction.effectiveProduction(game.state.board, game.state, RED);
        assertTrue(
            prodAfter <= prodBefore, "Production should not increase after robber blocks a tile");
      }
    }
  }

  @Test
  void valueFunctionEvaluateReturnsFinite() {
    Game game = twoPlayerGame(new SimplePlayer(RED), new SimplePlayer(BLUE));
    for (int i = 0; i < 8; i++) game.playTick();
    double score = ValueFunction.evaluate(game, RED, HeuristicWeights.defaults());
    assertTrue(Double.isFinite(score));
  }

  // ---- ValueFunctionPlayer ----

  @Test
  void valueFunctionPlayerPicksValidAction() {
    ValueFunctionPlayer p = new ValueFunctionPlayer(RED);
    Game game = twoPlayerGame(p, new SimplePlayer(BLUE));
    // Advance to a state where there are real choices (after initial placement)
    for (int i = 0; i < 8; i++) game.playTick();
    List<Action> actions = game.playableActions;
    if (actions.size() > 1) {
      Action chosen = p.decide(game, actions);
      assertTrue(actions.contains(chosen));
    }
  }

  @Test
  void valueFunctionPlayerFullGame() {
    ValueFunctionPlayer p1 = new ValueFunctionPlayer(RED);
    SimplePlayer p2 = new SimplePlayer(BLUE);
    Game game = twoPlayerGame(p1, p2);
    assertNotNull(game.play());
  }

  // ---- TreeSearchUtils ----

  @Test
  void executeSpectrumRollHas11Outcomes() {
    Game game = twoPlayerGame(new SimplePlayer(RED), new SimplePlayer(BLUE));
    // Advance to a ROLL action
    while (!game.playableActions.isEmpty()
        && game.playableActions.get(0).actionType() != ActionType.ROLL) {
      game.playTick();
    }
    if (!game.playableActions.isEmpty()
        && game.playableActions.get(0).actionType() == ActionType.ROLL) {
      List<TreeSearchUtils.Outcome> outcomes =
          TreeSearchUtils.executeSpectrum(game, game.playableActions.get(0));
      assertEquals(11, outcomes.size());
      double totalProba = outcomes.stream().mapToDouble(TreeSearchUtils.Outcome::proba).sum();
      assertEquals(1.0, totalProba, 0.001);
    }
  }

  @Test
  void listPrunedActionsSubsetOfPlayable() {
    Game game = twoPlayerGame(new SimplePlayer(RED), new SimplePlayer(BLUE));
    for (int i = 0; i < 8; i++) game.playTick();
    List<Action> pruned = TreeSearchUtils.listPrunedActions(game);
    assertFalse(pruned.isEmpty());
    assertTrue(
        game.playableActions.containsAll(pruned),
        "Pruned actions must be a subset of playable actions");
  }

  // ---- AlphaBetaPlayer ----

  @Test
  void alphaBetaPicksValidAction() {
    AlphaBetaPlayer p = new AlphaBetaPlayer(RED, 1, false);
    Game game = twoPlayerGame(p, new SimplePlayer(BLUE));
    for (int i = 0; i < 8; i++) game.playTick();
    List<Action> actions = game.playableActions;
    if (actions.size() > 1) {
      Action chosen = p.decide(game, actions);
      assertTrue(actions.contains(chosen));
    }
  }

  @Test
  void alphaBetaFullGame() {
    AlphaBetaPlayer p1 = new AlphaBetaPlayer(RED, 1, false);
    SimplePlayer p2 = new SimplePlayer(BLUE);
    Game game = twoPlayerGame(p1, p2);
    assertNotNull(game.play());
  }

  // ---- GreedyPlayoutsPlayer ----

  @Test
  void greedyPlayoutsPicksValidAction() {
    GreedyPlayoutsPlayer p = new GreedyPlayoutsPlayer(RED, 3, new Random(1));
    Game game = twoPlayerGame(p, new SimplePlayer(BLUE));
    for (int i = 0; i < 8; i++) game.playTick();
    List<Action> actions = game.playableActions;
    Action chosen = p.decide(game, actions);
    assertTrue(actions.contains(chosen));
  }

  // ---- MCTSPlayer ----

  @Test
  void mctsPicksValidAction() {
    MCTSPlayer p = new MCTSPlayer(RED, 5, false, new Random(1));
    Game game = twoPlayerGame(p, new SimplePlayer(BLUE));
    for (int i = 0; i < 8; i++) game.playTick();
    List<Action> actions = game.playableActions;
    if (actions.size() > 1) {
      Action chosen = p.decide(game, actions);
      assertTrue(actions.contains(chosen));
    }
  }

  @Test
  void mctsFullGame() {
    MCTSPlayer p1 = new MCTSPlayer(RED, 5, false, new Random(42));
    SimplePlayer p2 = new SimplePlayer(BLUE);
    Game game = twoPlayerGame(p1, p2);
    assertNotNull(game.play());
  }
}

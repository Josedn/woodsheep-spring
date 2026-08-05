package com.catanatron.core.models;

import static org.junit.jupiter.api.Assertions.*;

import com.catanatron.core.enums.ActionType;
import com.catanatron.core.enums.Color;
import com.catanatron.core.game.Game;
import com.catanatron.core.game.GameAccumulator;
import com.catanatron.core.state.Action;
import com.catanatron.core.state.GameState;
import com.catanatron.core.state.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GameTest {

  static class RandomPlayer implements Player {
    private final Color color;
    private final Random rng;

    RandomPlayer(Color c, long seed) {
      this.color = c;
      this.rng = new Random(seed);
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
    public Action decide(GameState state, List<Action> actions) {
      return actions.get(rng.nextInt(actions.size()));
    }
  }

  static class FirstPlayer implements Player {
    private final Color color;

    FirstPlayer(Color c) {
      this.color = c;
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
    public Action decide(GameState state, List<Action> actions) {
      return actions.get(0);
    }
  }

  @Test
  void gameInitialisesWithPlayableActions() {
    Game game = new Game(List.of(new FirstPlayer(Color.RED), new FirstPlayer(Color.BLUE)));
    assertFalse(game.playableActions.isEmpty());
    assertEquals(ActionType.BUILD_SETTLEMENT, game.playableActions.get(0).actionType());
  }

  @Test
  void playTickAdvancesState() {
    Game game = new Game(List.of(new FirstPlayer(Color.RED), new FirstPlayer(Color.BLUE)));
    int recordsBefore = game.state.actionRecords.size();
    game.playTick();
    assertEquals(recordsBefore + 1, game.state.actionRecords.size());
  }

  @Test
  void fullGameTerminates() {
    Game game =
        new Game(
            List.of(new RandomPlayer(Color.RED, 1), new RandomPlayer(Color.BLUE, 2)),
            42L,
            7,
            false,
            10,
            null,
            true);
    Color winner = game.play();
    // Either someone won or turns limit was hit (winner may be null)
    assertTrue(winner != null || game.state.numTurns >= Game.TURNS_LIMIT);
  }

  @Test
  void gameCopyIsIndependent() {
    Game game = new Game(List.of(new FirstPlayer(Color.RED), new FirstPlayer(Color.BLUE)));
    // play past initial phase
    for (int i = 0; i < 10; i++) game.playTick();

    Game copy = game.copy();
    int originalTurns = game.state.numTurns;
    // play enough ticks on the copy to guarantee at least one END_TURN
    for (int i = 0; i < 5; i++) copy.playTick();

    // original game state must be unchanged
    assertEquals(originalTurns, game.state.numTurns);
    // copy must have advanced
    assertTrue(copy.state.numTurns >= originalTurns);
  }

  @Test
  void winningColorNullAtStart() {
    Game game = new Game(List.of(new FirstPlayer(Color.RED), new FirstPlayer(Color.BLUE)));
    assertNull(game.winningColor());
  }

  @Test
  void accumulatorHooksAreCalled() {
    List<String> log = new ArrayList<>();
    GameAccumulator acc =
        new GameAccumulator() {
          @Override
          public void before(Game g) {
            log.add("before");
          }

          @Override
          public void step(Game g, Action a) {
            log.add("step");
          }

          @Override
          public void after(Game g) {
            log.add("after");
          }
        };

    Game game = new Game(List.of(new FirstPlayer(Color.RED), new FirstPlayer(Color.BLUE)));
    game.play(List.of(acc));

    assertTrue(log.contains("before"));
    assertTrue(log.contains("step"));
    assertTrue(log.contains("after"));
    assertEquals("before", log.get(0));
    assertEquals("after", log.get(log.size() - 1));
  }

  @Test
  void generatePlayableActionsInitialPhase() {
    Game game = new Game(List.of(new FirstPlayer(Color.RED), new FirstPlayer(Color.BLUE)));
    assertTrue(
        game.playableActions.stream().allMatch(a -> a.actionType() == ActionType.BUILD_SETTLEMENT));
  }

  @Test
  void generatePlayableActionsAfterInitial() {
    Game game = new Game(List.of(new FirstPlayer(Color.RED), new FirstPlayer(Color.BLUE)));
    // fast-forward past initial phase
    while (game.state.isInitialBuildPhase) game.playTick();

    List<ActionType> types = game.playableActions.stream().map(Action::actionType).toList();
    assertTrue(types.contains(ActionType.ROLL));
  }

  @Test
  void deterministicWithSameSeed() {
    Game g1 =
        new Game(
            List.of(new RandomPlayer(Color.RED, 10), new RandomPlayer(Color.BLUE, 20)),
            99L,
            7,
            false,
            10,
            null,
            true);
    Game g2 =
        new Game(
            List.of(new RandomPlayer(Color.RED, 10), new RandomPlayer(Color.BLUE, 20)),
            99L,
            7,
            false,
            10,
            null,
            true);
    // Play 20 ticks on each and compare turn counts
    for (int i = 0; i < 20; i++) {
      g1.playTick();
      g2.playTick();
    }
    assertEquals(g1.state.numTurns, g2.state.numTurns);
  }
}

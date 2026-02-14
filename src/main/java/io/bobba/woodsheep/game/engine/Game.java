package io.bobba.woodsheep.game.engine;

import io.bobba.woodsheep.game.model.PlayerColor;
import io.bobba.woodsheep.game.model.PlayerState;
import io.bobba.woodsheep.game.model.actions.Action;
import io.bobba.woodsheep.game.model.actions.ActionGenerator;
import io.bobba.woodsheep.game.model.actions.ActionRecord;
import io.bobba.woodsheep.game.model.actions.Reducer;
import java.util.List;

public class Game {
  public static final int TURNS_LIMIT = 1000;
  public final State state;
  public List<Action<?>> playableActions;

  public Game(List<? extends Player> players) {
    this.state = new State(players);
  }

  public ActionRecord<?> execute(Action<?> action) {
    // Refresh playable actions based on current state, then validate type presence
    this.playableActions = ActionGenerator.generatePlayable(state);
    if (playableActions.stream().noneMatch(a -> a.type == action.type)) {
      throw new IllegalArgumentException("Action not playable now: " + action);
    }
    ActionRecord<?> rec = Reducer.apply(state, action);
    this.playableActions = ActionGenerator.generatePlayable(state);
    return rec;
  }

  public PlayerColor playTick() {
    Player currentPlayer = state.currentPlayer();
    Action<?> action = currentPlayer.decide(this, this.playableActions);
    execute(action);
    return winningColor();
  }

  public PlayerColor winningColor() {
    // Basic win rule: 10 VP or all pieces exhausted (simplified)
    for (var playerStatusEntry : state.playerState.entrySet()) {
      PlayerState playerState = playerStatusEntry.getValue();
      int vps = playerState.actualVictoryPoints;
      if (vps >= 10) {
        return playerStatusEntry.getKey();
      }
      int settlementsLeft = playerState.settlementsAvailable;
      int citiesLeft = playerState.citiesAvailable;
      if (settlementsLeft == 0 && citiesLeft == 0) {
        return playerStatusEntry.getKey();
      }
    }
    return null;
  }
}

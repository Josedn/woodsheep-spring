package io.bobba.woodsheep.game.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.bobba.woodsheep.game.model.PlayerColor;
import io.bobba.woodsheep.game.model.PlayerState;
import io.bobba.woodsheep.game.model.actions.Action;
import io.bobba.woodsheep.game.model.actions.ActionPrompt;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameTest {

  private static Player playerOf(PlayerColor color) {
    return new Player(color, false) {
      @Override
      public Action<?> decide(Game game, List<Action<?>> playable) {
        return null;
      }
    };
  }

  @Test
  void newGame_singlePlayer_stateIsCorrectlyInitialized() {
    Game game = new Game(List.of(playerOf(PlayerColor.RED)));

    assertThat(game.state.players).hasSize(1);
    assertThat(game.state.colors).containsExactly(PlayerColor.RED);
    assertThat(game.state.playerState).containsKey(PlayerColor.RED);
    assertThat(game.state.currentPrompt).isEqualTo(ActionPrompt.BUILD_INITIAL_SETTLEMENT);
    assertThat(game.state.isInitialBuildPhase).isTrue();
    assertThat(game.state.currentPlayerIndex).isZero();
    assertThat(game.playableActions).isNull();
  }

  @Test
  void newGame_multiplePlayers_allColorsRegistered() {
    List<Player> players =
        List.of(
            playerOf(PlayerColor.RED),
            playerOf(PlayerColor.BLUE),
            playerOf(PlayerColor.ORANGE),
            playerOf(PlayerColor.WHITE));

    Game game = new Game(players);

    assertThat(game.state.players).hasSize(4);
    assertThat(game.state.colors)
        .containsExactly(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.ORANGE, PlayerColor.WHITE);
    assertThat(game.state.playerState)
        .containsKeys(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.ORANGE, PlayerColor.WHITE);
  }

  @Test
  void newGame_eachPlayerState_hasInitialResourcesAndPieces() {
    Game game = new Game(List.of(playerOf(PlayerColor.RED), playerOf(PlayerColor.BLUE)));

    for (PlayerColor color : game.state.colors) {
      PlayerState ps = game.state.playerState.get(color);
      assertThat(ps.settlementsAvailable).isEqualTo(5);
      assertThat(ps.citiesAvailable).isEqualTo(4);
      assertThat(ps.roadsAvailable).isEqualTo(15);
      assertThat(ps.actualVictoryPoints).isZero();
    }
  }
}

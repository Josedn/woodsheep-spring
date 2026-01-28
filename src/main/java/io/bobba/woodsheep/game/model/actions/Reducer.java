package io.bobba.woodsheep.game.model.actions;

import io.bobba.woodsheep.game.engine.State;
import io.bobba.woodsheep.game.model.BuildingType;
import io.bobba.woodsheep.game.model.DevCard;
import io.bobba.woodsheep.game.model.PlayerColor;
import io.bobba.woodsheep.game.model.PlayerState;
import io.bobba.woodsheep.game.model.Resource;
import java.util.Random;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Reducer {
  private static final Random RNG = new Random();

  public static ActionRecord<?> apply(State state, Action<?> action) {
    return switch (action.type) {
      case END_TURN -> endTurn(state, action);
      case ROLL -> roll(state, action);
      case BUILD_SETTLEMENT -> initialBuildSettlement(state, action);
      default -> null;
    };
  }

  private static ActionRecord<?> endTurn(State state, Action<?> action) {
    // Clean per-turn flags
    PlayerState currentPlayerState = state.playerState.get(state.currentColor());
    currentPlayerState.hasRolled = false;
    // Reset dev-card per-turn flag and set owned-at-start markers for next turn
    currentPlayerState.hasPlayerDevCardInTurn = false;
    // Owned-at-start means playable next turn if in hand
    currentPlayerState.devCardsOwnedAtStart.put(
        DevCard.KNIGHT, currentPlayerState.devCardsInHand.get(DevCard.KNIGHT) > 0);
    currentPlayerState.devCardsOwnedAtStart.put(
        DevCard.MONOPOLY, currentPlayerState.devCardsInHand.get(DevCard.MONOPOLY) > 0);
    currentPlayerState.devCardsOwnedAtStart.put(
        DevCard.YEAR_OF_PLENTY, currentPlayerState.devCardsInHand.get(DevCard.YEAR_OF_PLENTY) > 0);
    currentPlayerState.devCardsOwnedAtStart.put(
        DevCard.ROAD_BUILDING, currentPlayerState.devCardsInHand.get(DevCard.ROAD_BUILDING) > 0);
    // Advance
    int nextPlayerIndex = (state.currentPlayerIndex + 1) % state.colors.size();
    state.currentPlayerIndex = nextPlayerIndex;
    state.currentTurnIndex = nextPlayerIndex;
    state.numTurns++;
    state.currentPrompt = ActionPrompt.PLAY_TURN;
    return new ActionRecord<>(action, null);
  }

  private static ActionRecord<?> roll(State state, Action<?> action) {
    PlayerState currentPlayerState = state.playerState.get(state.currentColor());
    currentPlayerState.hasRolled = true;
    int d1, d2;
    if (action.value instanceof int[] arr && arr.length == 2) {
      d1 = arr[0];
      d2 = arr[1];
    } else {
      d1 = RNG.nextInt(6) + 1;
      d2 = RNG.nextInt(6) + 1;
    }
    int sum = d1 + d2;
    if (sum == 7) {
      // Check discards
      int nextDiscardIdx = nextDiscardIndex(state);
      if (nextDiscardIdx >= 0) {
        state.currentPlayerIndex = nextDiscardIdx;
        state.currentPrompt = ActionPrompt.DISCARD;
        state.isDiscarding = true;
      } else {
        state.currentPrompt = ActionPrompt.MOVE_ROBBER;
        state.isMovingKnight = true;
      }
    } else {
      // Payout: for each tile with this number, give 1 per settlement, 2 per city to owner if not
      // robbed
      for (int tileId : state.map.getTileIdsByNumber(sum)) {
        if (tileId == state.robberTileId) {
          continue;
        }
        Resource res = state.map.tilesById.get(tileId).resource();
        if (res == null || res == Resource.NONE) {
          continue;
        }
        for (Integer nodeId : state.map.getTileNodes(tileId)) {
          var building = state.board.buildingAt(nodeId);
          if (building == null) {
            continue;
          }
          var color = building.getKey();
          var type = building.getValue();
          int amount = (type == BuildingType.CITY ? 2 : 1);
          state.playerState.get(color).addResource(res, amount);
        }
      }
      state.currentPrompt = ActionPrompt.PLAY_TURN;
    }
    return new ActionRecord<>(
        new Action<>(action.color, action.type, new int[] {d1, d2}), new int[] {d1, d2});
  }

  private static ActionRecord<?> initialBuildSettlement(State s, Action<?> a) {
    if (s.isInitialBuildPhase) {
      // Advance prompts as in snake placement (simplified)
      // Award 1 VP for settlement and consume piece.
      PlayerState currentPlayerState = s.playerState.get(s.currentColor());
      currentPlayerState.victoryPoints++;
      currentPlayerState.actualVictoryPoints++;
      currentPlayerState.settlementsAvailable--;
      int nodeId = (int) a.value;
      s.board.buildSettlement(s.currentColor(), nodeId);
      // Track last initial settlement to constrain initial road
      s.lastInitialSettlement.put(s.currentColor(), nodeId);
      // maintainLongestRoad(s); // settlements can block/cut in full rules; safe to recompute
      s.currentPrompt = ActionPrompt.BUILD_INITIAL_ROAD;
    } else {
      // Pay, place, update availability

    }
    return new ActionRecord<>(a, null);
  }

  private static int nextDiscardIndex(State state) {
    for (int i = 0; i < state.colors.size(); i++) {
      int idx = (state.currentTurnIndex + i) % state.colors.size();
      if (numResources(state, state.colors.get(idx)) > state.discardLimit) return idx;
    }
    return -1;
  }

  private static int numResources(State state, PlayerColor playerColor) {
    PlayerState playerState = state.playerState.get(playerColor);
    return playerState.resourcesInHand.values().stream().reduce(0, Integer::sum);
  }
}

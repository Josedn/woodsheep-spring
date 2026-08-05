package com.catanatron.core.game;

import com.catanatron.core.enums.ActionPrompt;
import com.catanatron.core.enums.ActionType;
import com.catanatron.core.enums.BuildingType;
import com.catanatron.core.enums.Color;
import com.catanatron.core.enums.DevCard;
import com.catanatron.core.enums.Resource;
import com.catanatron.core.models.Board;
import com.catanatron.core.models.Coordinate;
import com.catanatron.core.models.Decks;
import com.catanatron.core.models.EdgeId;
import com.catanatron.core.models.LandTile;
import com.catanatron.core.state.Action;
import com.catanatron.core.state.ActionRecord;
import com.catanatron.core.state.GameState;
import com.catanatron.core.state.PlayerState;
import com.catanatron.core.state.StateFunctions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Main rules engine. Mirrors apply_action.py exactly.
 *
 * <p>Each handler mutates state and returns an ActionRecord.
 */
public final class ApplyAction {

  private ApplyAction() {}

  public static ActionRecord applyAction(GameState state, Action action, Random rng) {
    return applyAction(state, action, null, rng);
  }

  public static ActionRecord applyAction(
      GameState state, Action action, ActionRecord record, Random rng) {

    ActionRecord result =
        switch (action.actionType()) {
          case END_TURN -> applyEndTurn(state, action);
          case BUILD_SETTLEMENT -> applyBuildSettlement(state, action);
          case BUILD_ROAD -> applyBuildRoad(state, action, rng);
          case BUILD_CITY -> applyBuildCity(state, action);
          case BUY_DEVELOPMENT_CARD -> applyBuyDevelopmentCard(state, action, record, rng);
          case ROLL -> applyRoll(state, action, record, rng);
          case DISCARD_RESOURCE -> applyDiscard(state, action);
          case MOVE_ROBBER -> applyMoveRobber(state, action, record, rng);
          case PLAY_KNIGHT_CARD -> applyPlayKnightCard(state, action);
          case PLAY_YEAR_OF_PLENTY -> applyPlayYearOfPlenty(state, action);
          case PLAY_MONOPOLY -> applyPlayMonopoly(state, action);
          case PLAY_ROAD_BUILDING -> applyPlayRoadBuilding(state, action);
          case MARITIME_TRADE -> applyMaritimeTrade(state, action);
          case OFFER_TRADE -> applyOfferTrade(state, action);
          case ACCEPT_TRADE -> applyAcceptTrade(state, action);
          case REJECT_TRADE -> applyRejectTrade(state, action);
          case CONFIRM_TRADE -> applyConfirmTrade(state, action);
          case CANCEL_TRADE -> applyCancelTrade(state, action);
          default ->
              throw new IllegalArgumentException("Unknown ActionType: " + action.actionType());
        };

    state.actionRecords.add(result);
    return result;
  }

  // ===== Handlers =====

  private static ActionRecord applyEndTurn(GameState state, Action action) {
    StateFunctions.playerCleanTurn(state, action.color());
    advanceTurn(state);
    state.currentPrompt = ActionPrompt.PLAY_TURN;
    return new ActionRecord(action, null);
  }

  private static ActionRecord applyBuildSettlement(GameState state, Action action) {
    int nodeId = (int) action.value();
    if (state.isInitialBuildPhase) {
      state.board.buildSettlement(action.color(), nodeId, true);
      StateFunctions.buildSettlement(state, action.color(), nodeId, true);

      List<Integer> buildings =
          StateFunctions.getPlayerBuildings(state, action.color(), BuildingType.SETTLEMENT);
      if (buildings.size() == 2) {
        // Second initial settlement — yield adjacent resources
        for (LandTile tile : state.board.map.adjacentTiles.getOrDefault(nodeId, List.of())) {
          if (tile.resource != null) {
            Decks.freqdeckDraw(state.resourceFreqdeck, 1, tile.resource);
            StateFunctions.playerDeckReplenish(state, action.color(), tile.resource, 1);
          }
        }
      }
      state.currentPrompt = ActionPrompt.BUILD_INITIAL_ROAD;
    } else {
      Board.RoadResult r = state.board.buildSettlement(action.color(), nodeId, false);
      StateFunctions.buildSettlement(state, action.color(), nodeId, false);
      state.resourceFreqdeck = Decks.freqdeckAdd(state.resourceFreqdeck, Decks.SETTLEMENT_COST);
      StateFunctions.maintainLongestRoad(
          state, r.previousRoadColor(), r.newRoadColor(), r.roadLengths());
    }
    return new ActionRecord(action, null);
  }

  private static ActionRecord applyBuildRoad(GameState state, Action action, Random rng) {
    EdgeId edge = (EdgeId) action.value();
    if (state.isInitialBuildPhase) {
      state.board.buildRoad(action.color(), edge);
      StateFunctions.buildRoad(state, action.color(), edge, true);

      int numBuildings = 0;
      for (Color c : state.colors) {
        numBuildings += StateFunctions.getPlayerBuildings(state, c, BuildingType.SETTLEMENT).size();
      }
      int numPlayers = state.colors.length;
      boolean goingForward = numBuildings < numPlayers;
      boolean atTheEnd = numBuildings == numPlayers;

      if (goingForward) {
        advanceTurn(state);
        state.currentPrompt = ActionPrompt.BUILD_INITIAL_SETTLEMENT;
      } else if (atTheEnd) {
        state.currentPrompt = ActionPrompt.BUILD_INITIAL_SETTLEMENT;
      } else if (numBuildings == 2 * numPlayers) {
        state.isInitialBuildPhase = false;
        state.currentPrompt = ActionPrompt.PLAY_TURN;
      } else {
        advanceTurn(state, -1);
        state.currentPrompt = ActionPrompt.BUILD_INITIAL_SETTLEMENT;
      }
    } else if (state.isRoadBuilding && state.freeRoadsAvailable > 0) {
      Board.RoadResult r = state.board.buildRoad(action.color(), edge);
      StateFunctions.buildRoad(state, action.color(), edge, true);
      StateFunctions.maintainLongestRoad(
          state, r.previousRoadColor(), r.newRoadColor(), r.roadLengths());

      state.freeRoadsAvailable--;
      if (state.freeRoadsAvailable == 0
          || roadBuildingPossibilities(state, action.color(), false).isEmpty()) {
        state.isRoadBuilding = false;
        state.freeRoadsAvailable = 0;
      }
    } else {
      Board.RoadResult r = state.board.buildRoad(action.color(), edge);
      StateFunctions.buildRoad(state, action.color(), edge, false);
      StateFunctions.maintainLongestRoad(
          state, r.previousRoadColor(), r.newRoadColor(), r.roadLengths());
    }
    return new ActionRecord(action, null);
  }

  private static ActionRecord applyBuildCity(GameState state, Action action) {
    int nodeId = (int) action.value();
    state.board.buildCity(action.color(), nodeId);
    StateFunctions.buildCity(state, action.color(), nodeId);
    state.resourceFreqdeck = Decks.freqdeckAdd(state.resourceFreqdeck, Decks.CITY_COST);
    return new ActionRecord(action, null);
  }

  private static ActionRecord applyBuyDevelopmentCard(
      GameState state, Action action, ActionRecord record, Random rng) {
    if (state.developmentListdeck.isEmpty())
      throw new IllegalStateException("No more development cards");
    if (!StateFunctions.playerCanAffordDevCard(state, action.color()))
      throw new IllegalStateException("Cannot afford development card");

    DevCard card;
    if (record == null) {
      card = state.developmentListdeck.remove(state.developmentListdeck.size() - 1);
    } else {
      card = (DevCard) record.result();
      Decks.drawFromListdeck(state.developmentListdeck, 1, card);
    }

    StateFunctions.buyDevCard(state, action.color(), card);
    state.resourceFreqdeck = Decks.freqdeckAdd(state.resourceFreqdeck, Decks.DEVELOPMENT_CARD_COST);

    Action fullAction = new Action(action.color(), action.actionType(), card);
    return new ActionRecord(fullAction, card);
  }

  private static ActionRecord applyRoll(
      GameState state, Action action, ActionRecord record, Random rng) {
    state.playerState(action.color()).hasRolled = true;

    int[] dice = (record != null) ? (int[]) record.result() : rollDice(rng);
    int number = dice[0] + dice[1];
    Action fullAction = new Action(action.color(), action.actionType(), dice);

    if (number == 7) {
      int firstDiscarderIndex = -1;
      for (int i = 0; i < state.colors.length; i++) {
        int num = StateFunctions.playerNumResourceCards(state, state.colors[i]);
        int count = num > state.discardLimit ? num / 2 : 0;
        state.discardCounts[i] = count;
        if (count > 0 && firstDiscarderIndex < 0) firstDiscarderIndex = i;
      }
      if (firstDiscarderIndex >= 0) {
        state.currentPlayerIndex = firstDiscarderIndex;
        state.currentPrompt = ActionPrompt.DISCARD;
        state.isDiscarding = true;
      } else {
        state.discardCounts = new int[state.colors.length];
        state.currentPrompt = ActionPrompt.MOVE_ROBBER;
        state.isMovingKnight = true;
      }
    } else {
      YieldResult yr = yieldResources(state.board, state.resourceFreqdeck, number);
      for (Color c : yr.payout.keySet()) {
        int[] payout = yr.payout.get(c);
        StateFunctions.playerFreqdeckAdd(state, c, payout);
        state.resourceFreqdeck = Decks.freqdeckSubtract(state.resourceFreqdeck, payout);
      }
      state.currentPrompt = ActionPrompt.PLAY_TURN;
    }
    return new ActionRecord(fullAction, dice);
  }

  private static ActionRecord applyDiscard(GameState state, Action action) {
    Resource discarded = (Resource) action.value();
    int playerIndex = state.playerIndex(action.color());

    int[] toDiscard = new int[5];
    toDiscard[discarded.ordinal()] = 1;
    StateFunctions.playerFreqdeckSubtract(state, action.color(), toDiscard);
    state.resourceFreqdeck = Decks.freqdeckAdd(state.resourceFreqdeck, toDiscard);
    state.discardCounts[playerIndex]--;

    if (state.discardCounts[playerIndex] > 0) {
      // same player keeps discarding
    } else {
      // advance to next player who still needs to discard
      int nextDiscarder = -1;
      for (int i = playerIndex + 1; i < state.colors.length; i++) {
        if (state.discardCounts[i] > 0) {
          nextDiscarder = i;
          break;
        }
      }
      if (nextDiscarder >= 0) {
        state.currentPlayerIndex = nextDiscarder;
      } else {
        state.currentPlayerIndex = state.currentTurnIndex;
        state.currentPrompt = ActionPrompt.MOVE_ROBBER;
        state.isDiscarding = false;
        state.isMovingKnight = true;
        state.discardCounts = new int[state.colors.length];
      }
    }

    return new ActionRecord(action, discarded);
  }

  private static ActionRecord applyMoveRobber(
      GameState state, Action action, ActionRecord record, Random rng) {
    Object[] value = (Object[]) action.value();
    Coordinate coordinate = (Coordinate) value[0];
    Color robbedColor = (Color) value[1];

    Resource robbedResource = null;
    if (robbedColor != null) {
      robbedResource =
          (record != null)
              ? (Resource) record.result()
              : StateFunctions.playerDeckRandomSelect(state, robbedColor, rng);
      StateFunctions.playerDeckDraw(state, robbedColor, robbedResource, 1);
      StateFunctions.playerDeckReplenish(state, action.color(), robbedResource, 1);
    }
    state.board.robberCoordinate = coordinate;
    state.currentPrompt = ActionPrompt.PLAY_TURN;
    state.isMovingKnight = false;

    return new ActionRecord(action, robbedResource);
  }

  private static ActionRecord applyPlayKnightCard(GameState state, Action action) {
    if (!StateFunctions.playerCanPlayDev(state, action.color(), DevCard.KNIGHT))
      throw new IllegalStateException("Cannot play knight card now");
    StateFunctions.playDevCard(state, action.color(), DevCard.KNIGHT);
    state.currentPrompt = ActionPrompt.MOVE_ROBBER;
    state.isMovingKnight = true;
    return new ActionRecord(action, null);
  }

  private static ActionRecord applyPlayYearOfPlenty(GameState state, Action action) {
    Resource[] cards = (Resource[]) action.value();
    int[] selected = new int[5];
    for (Resource r : cards) selected[r.ordinal()]++;

    if (!StateFunctions.playerCanPlayDev(state, action.color(), DevCard.YEAR_OF_PLENTY))
      throw new IllegalStateException("Cannot play year of plenty now");
    if (!Decks.freqdeckContains(state.resourceFreqdeck, selected))
      throw new IllegalStateException("Bank lacks those resources");

    StateFunctions.playerFreqdeckAdd(state, action.color(), selected);
    state.resourceFreqdeck = Decks.freqdeckSubtract(state.resourceFreqdeck, selected);
    StateFunctions.playDevCard(state, action.color(), DevCard.YEAR_OF_PLENTY);
    state.currentPrompt = ActionPrompt.PLAY_TURN;
    return new ActionRecord(action, null);
  }

  private static ActionRecord applyPlayMonopoly(GameState state, Action action) {
    Resource monoResource = (Resource) action.value();
    if (!StateFunctions.playerCanPlayDev(state, action.color(), DevCard.MONOPOLY))
      throw new IllegalStateException("Cannot play monopoly now");

    int[] stolen = new int[5];
    for (Color c : state.colors) {
      if (c == action.color()) continue;
      int count = state.playerState(c).resourcesInHand[monoResource.ordinal()];
      stolen[monoResource.ordinal()] += count;
      StateFunctions.playerDeckDraw(state, c, monoResource, count);
    }
    StateFunctions.playerFreqdeckAdd(state, action.color(), stolen);
    StateFunctions.playDevCard(state, action.color(), DevCard.MONOPOLY);
    state.currentPrompt = ActionPrompt.PLAY_TURN;
    return new ActionRecord(action, null);
  }

  private static ActionRecord applyPlayRoadBuilding(GameState state, Action action) {
    if (!StateFunctions.playerCanPlayDev(state, action.color(), DevCard.ROAD_BUILDING))
      throw new IllegalStateException("Cannot play road building now");
    StateFunctions.playDevCard(state, action.color(), DevCard.ROAD_BUILDING);
    state.isRoadBuilding = true;
    state.freeRoadsAvailable = 2;
    state.currentPrompt = ActionPrompt.PLAY_TURN;
    return new ActionRecord(action, null);
  }

  private static ActionRecord applyMaritimeTrade(GameState state, Action action) {
    Resource[] tradeOffer = (Resource[]) action.value(); // length 5; last = asking
    int[] offering = new int[5];
    for (int i = 0; i < tradeOffer.length - 1; i++) {
      if (tradeOffer[i] != null) offering[tradeOffer[i].ordinal()]++;
    }
    int[] asking = new int[5];
    asking[tradeOffer[tradeOffer.length - 1].ordinal()]++;

    if (!StateFunctions.playerResourceFreqdeckContains(state, action.color(), offering))
      throw new IllegalStateException("Cannot afford maritime trade");
    if (!Decks.freqdeckContains(state.resourceFreqdeck, asking))
      throw new IllegalStateException("Bank lacks those resources");

    StateFunctions.playerFreqdeckSubtract(state, action.color(), offering);
    state.resourceFreqdeck = Decks.freqdeckAdd(state.resourceFreqdeck, offering);
    StateFunctions.playerFreqdeckAdd(state, action.color(), asking);
    state.resourceFreqdeck = Decks.freqdeckSubtract(state.resourceFreqdeck, asking);
    state.currentPrompt = ActionPrompt.PLAY_TURN;
    return new ActionRecord(action, null);
  }

  private static ActionRecord applyOfferTrade(GameState state, Action action) {
    state.isResolvingTrade = true;
    int[] tradeValue = (int[]) action.value();
    state.currentTrade = Arrays.copyOf(tradeValue, 11);
    state.currentTrade[10] = state.currentTurnIndex;

    // first opponent in seating order
    for (int i = 0; i < state.colors.length; i++) {
      if (state.colors[i] != action.color()) {
        state.currentPlayerIndex = i;
        break;
      }
    }
    state.currentPrompt = ActionPrompt.DECIDE_TRADE;
    return new ActionRecord(action, null);
  }

  private static ActionRecord applyAcceptTrade(GameState state, Action action) {
    int idx = state.playerIndex(action.color());
    state.acceptees[idx] = true;

    boolean foundNext = false;
    for (int i = state.currentPlayerIndex + 1; i < state.colors.length; i++) {
      if (state.colors[i] != action.color()) {
        state.currentPlayerIndex = i;
        foundNext = true;
        break;
      }
    }
    if (!foundNext) {
      state.currentPlayerIndex = state.currentTurnIndex;
      state.currentPrompt = ActionPrompt.DECIDE_ACCEPTEES;
    }
    return new ActionRecord(action, null);
  }

  private static ActionRecord applyRejectTrade(GameState state, Action action) {
    boolean foundNext = false;
    for (int i = state.currentPlayerIndex + 1; i < state.colors.length; i++) {
      if (state.colors[i] != action.color()) {
        state.currentPlayerIndex = i;
        foundNext = true;
        break;
      }
    }
    if (!foundNext) {
      int accepteeCount = 0;
      for (boolean a : state.acceptees) if (a) accepteeCount++;
      if (accepteeCount == 0) {
        resetTradingState(state);
        state.currentPlayerIndex = state.currentTurnIndex;
        state.currentPrompt = ActionPrompt.PLAY_TURN;
      } else {
        state.currentPlayerIndex = state.currentTurnIndex;
        state.currentPrompt = ActionPrompt.DECIDE_ACCEPTEES;
      }
    }
    return new ActionRecord(action, null);
  }

  private static ActionRecord applyConfirmTrade(GameState state, Action action) {
    int[] value = (int[]) action.value();
    int[] offering = Arrays.copyOfRange(value, 0, 5);
    int[] asking = Arrays.copyOfRange(value, 5, 10);
    Color enemyColor = state.colors[value[10]];

    StateFunctions.playerFreqdeckSubtract(state, action.color(), offering);
    StateFunctions.playerFreqdeckAdd(state, action.color(), asking);
    StateFunctions.playerFreqdeckSubtract(state, enemyColor, asking);
    StateFunctions.playerFreqdeckAdd(state, enemyColor, offering);

    resetTradingState(state);
    state.currentPlayerIndex = state.currentTurnIndex;
    state.currentPrompt = ActionPrompt.PLAY_TURN;
    return new ActionRecord(action, null);
  }

  private static ActionRecord applyCancelTrade(GameState state, Action action) {
    resetTradingState(state);
    state.currentPlayerIndex = state.currentTurnIndex;
    state.currentPrompt = ActionPrompt.PLAY_TURN;
    return new ActionRecord(action, null);
  }

  // ===== Helpers =====

  public static int[] rollDice(Random rng) {
    return new int[] {rng.nextInt(6) + 1, rng.nextInt(6) + 1};
  }

  public static YieldResult yieldResources(Board board, int[] resourceFreqdeck, int number) {
    java.util.Map<Color, int[]> intendedPayout = new java.util.HashMap<>();
    java.util.Map<Resource, Integer> resourceTotals = new java.util.HashMap<>();

    for (java.util.Map.Entry<Coordinate, LandTile> entry : board.map.landTiles.entrySet()) {
      LandTile tile = entry.getValue();
      if (tile.number == null || tile.number != number) continue;
      if (board.robberCoordinate.equals(entry.getKey())) continue;

      for (int nodeId : tile.nodes.values()) {
        Color nodeColor = board.getNodeColor(nodeId);
        if (nodeColor == null) continue;
        BuildingType bt = board.getBuildingType(nodeId);
        int amount = (bt == BuildingType.CITY) ? 2 : 1;
        intendedPayout.computeIfAbsent(nodeColor, k -> new int[5])[tile.resource.ordinal()] +=
            amount;
        resourceTotals.merge(tile.resource, amount, Integer::sum);
      }
    }

    // Check depletion
    java.util.Set<Resource> depleted = new java.util.HashSet<>();
    for (java.util.Map.Entry<Resource, Integer> e : resourceTotals.entrySet()) {
      if (!Decks.freqdeckCanDraw(resourceFreqdeck, e.getValue(), e.getKey())) {
        depleted.add(e.getKey());
      }
    }

    // Remove depleted resources from payout
    java.util.Map<Color, int[]> payout = new java.util.HashMap<>();
    for (java.util.Map.Entry<Color, int[]> e : intendedPayout.entrySet()) {
      int[] p = e.getValue().clone();
      for (Resource r : depleted) p[r.ordinal()] = 0;
      payout.put(e.getKey(), p);
    }

    return new YieldResult(payout, new ArrayList<>(depleted));
  }

  public record YieldResult(java.util.Map<Color, int[]> payout, List<Resource> depleted) {}

  private static void advanceTurn(GameState state) {
    advanceTurn(state, 1);
  }

  private static void advanceTurn(GameState state, int direction) {
    int next = Math.floorMod(state.currentPlayerIndex + direction, state.colors.length);
    state.currentPlayerIndex = next;
    state.currentTurnIndex = next;
    state.numTurns++;
  }

  private static void resetTradingState(GameState state) {
    state.isResolvingTrade = false;
    state.currentTrade = new int[11];
    state.acceptees = new boolean[state.colors.length];
  }

  private static List<Action> roadBuildingPossibilities(
      GameState state, Color color, boolean checkMoney) {
    PlayerState ps = state.playerState(color);
    if (ps.roadsAvailable <= 0) return List.of();
    if (checkMoney && !StateFunctions.playerResourceFreqdeckContains(state, color, Decks.ROAD_COST))
      return List.of();
    List<EdgeId> edges = state.board.buildableEdges(color);
    List<Action> actions = new ArrayList<>(edges.size());
    for (EdgeId e : edges) actions.add(new Action(color, ActionType.BUILD_ROAD, e));
    return actions;
  }
}

package com.catanatron.core.game;

import com.catanatron.core.enums.ActionPrompt;
import com.catanatron.core.enums.ActionType;
import com.catanatron.core.enums.BuildingType;
import com.catanatron.core.enums.Color;
import com.catanatron.core.enums.DevCard;
import com.catanatron.core.enums.Resource;
import com.catanatron.core.models.Coordinate;
import com.catanatron.core.models.Decks;
import com.catanatron.core.models.EdgeId;
import com.catanatron.core.models.LandTile;
import com.catanatron.core.state.Action;
import com.catanatron.core.state.GameState;
import com.catanatron.core.state.PlayerState;
import com.catanatron.core.state.StateFunctions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Move-generation. Mirrors models/actions.py generate_playable_actions and helpers. */
public final class Actions {

  private Actions() {}

  public static List<Action> generatePlayableActions(GameState state) {
    ActionPrompt prompt = state.currentPrompt;
    Color color = state.currentColor();

    return switch (prompt) {
      case BUILD_INITIAL_SETTLEMENT -> settlementPossibilities(state, color, true);
      case BUILD_INITIAL_ROAD -> initialRoadPossibilities(state, color);
      case MOVE_ROBBER -> robberPossibilities(state, color);
      case DISCARD -> discardPossibilities(state, color);
      case DECIDE_TRADE -> decideTradePossibilities(state, color);
      case DECIDE_ACCEPTEES -> decideAccepteesPossibilities(state, color);
      case PLAY_TURN -> playTurnPossibilities(state, color);
    };
  }

  // ===== PLAY_TURN =====

  private static List<Action> playTurnPossibilities(GameState state, Color color) {
    if (state.isRoadBuilding) return roadBuildingPossibilities(state, color, false);

    List<Action> actions = new ArrayList<>();

    if (StateFunctions.playerCanPlayDev(state, color, DevCard.YEAR_OF_PLENTY))
      actions.addAll(yearOfPlentyPossibilities(color, state.resourceFreqdeck));
    if (StateFunctions.playerCanPlayDev(state, color, DevCard.MONOPOLY))
      actions.addAll(monopolyPossibilities(color));
    if (StateFunctions.playerCanPlayDev(state, color, DevCard.KNIGHT))
      actions.add(new Action(color, ActionType.PLAY_KNIGHT_CARD, null));
    if (StateFunctions.playerCanPlayDev(state, color, DevCard.ROAD_BUILDING)
        && !roadBuildingPossibilities(state, color, false).isEmpty())
      actions.add(new Action(color, ActionType.PLAY_ROAD_BUILDING, null));

    if (!StateFunctions.playerHasRolled(state, color)) {
      actions.add(new Action(color, ActionType.ROLL, null));
    } else {
      actions.add(new Action(color, ActionType.END_TURN, null));
      actions.addAll(roadBuildingPossibilities(state, color, true));
      actions.addAll(settlementPossibilities(state, color, false));
      actions.addAll(cityPossibilities(state, color));
      if (StateFunctions.playerCanAffordDevCard(state, color)
          && !state.developmentListdeck.isEmpty())
        actions.add(new Action(color, ActionType.BUY_DEVELOPMENT_CARD, null));
      actions.addAll(maritimeTradePossibilities(state, color));
    }
    return actions;
  }

  // ===== Settlement / Road / City =====

  public static List<Action> settlementPossibilities(
      GameState state, Color color, boolean initialBuildPhase) {
    if (initialBuildPhase) {
      List<Action> actions = new ArrayList<>();
      for (int nodeId : state.board.buildableNodeIds(color, true))
        actions.add(new Action(color, ActionType.BUILD_SETTLEMENT, nodeId));
      return actions;
    }
    PlayerState ps = state.playerState(color);
    if (!StateFunctions.playerResourceFreqdeckContains(state, color, Decks.SETTLEMENT_COST)
        || ps.settlementsAvailable <= 0) return List.of();
    List<Action> actions = new ArrayList<>();
    for (int nodeId : state.board.buildableNodeIds(color, false))
      actions.add(new Action(color, ActionType.BUILD_SETTLEMENT, nodeId));
    return actions;
  }

  public static List<Action> roadBuildingPossibilities(
      GameState state, Color color, boolean checkMoney) {
    if (state.playerState(color).roadsAvailable <= 0) return List.of();
    if (checkMoney && !StateFunctions.playerResourceFreqdeckContains(state, color, Decks.ROAD_COST))
      return List.of();
    List<Action> actions = new ArrayList<>();
    for (EdgeId e : state.board.buildableEdges(color))
      actions.add(new Action(color, ActionType.BUILD_ROAD, e));
    return actions;
  }

  public static List<Action> cityPossibilities(GameState state, Color color) {
    if (!StateFunctions.playerResourceFreqdeckContains(state, color, Decks.CITY_COST))
      return List.of();
    if (state.playerState(color).citiesAvailable <= 0) return List.of();
    List<Action> actions = new ArrayList<>();
    for (int nodeId : StateFunctions.getPlayerBuildings(state, color, BuildingType.SETTLEMENT))
      actions.add(new Action(color, ActionType.BUILD_CITY, nodeId));
    return actions;
  }

  public static List<Action> initialRoadPossibilities(GameState state, Color color) {
    List<Integer> settlements =
        StateFunctions.getPlayerBuildings(state, color, BuildingType.SETTLEMENT);
    int lastSettlement = settlements.get(settlements.size() - 1);
    List<Action> actions = new ArrayList<>();
    for (EdgeId edge : state.board.buildableEdges(color)) {
      if (edge.a() == lastSettlement || edge.b() == lastSettlement)
        actions.add(new Action(color, ActionType.BUILD_ROAD, edge));
    }
    return actions;
  }

  // ===== Robber =====

  public static List<Action> robberPossibilities(GameState state, Color color) {
    List<Action> actions = robberPossibilitiesRaw(state, color);
    if (!state.friendlyRobber) return actions;

    List<Action> filtered = new ArrayList<>();
    for (Action a : actions) {
      if (!robberActionBlocksLowVpEnemy(state, color, a)) filtered.add(a);
    }
    return filtered.isEmpty() ? actions : filtered;
  }

  private static List<Action> robberPossibilitiesRaw(GameState state, Color color) {
    List<Action> actions = new ArrayList<>();
    for (var entry : state.board.map.landTiles.entrySet()) {
      Coordinate coord = entry.getKey();
      LandTile tile = entry.getValue();
      if (coord.equals(state.board.robberCoordinate)) continue;

      Set<Color> toStealFrom = new HashSet<>();
      for (int nodeId : tile.nodes.values()) {
        Color nc = state.board.getNodeColor(nodeId);
        if (nc != null && nc != color && StateFunctions.playerNumResourceCards(state, nc) >= 1)
          toStealFrom.add(nc);
      }
      if (toStealFrom.isEmpty()) {
        actions.add(new Action(color, ActionType.MOVE_ROBBER, new Object[] {coord, null}));
      } else {
        for (Color enemy : toStealFrom)
          actions.add(new Action(color, ActionType.MOVE_ROBBER, new Object[] {coord, enemy}));
      }
    }
    return actions;
  }

  private static boolean robberActionBlocksLowVpEnemy(GameState state, Color color, Action action) {
    Object[] val = (Object[]) action.value();
    Coordinate coord = (Coordinate) val[0];
    LandTile tile = state.board.map.landTiles.get(coord);
    for (int nodeId : tile.nodes.values()) {
      Color nc = state.board.getNodeColor(nodeId);
      if (nc == null || nc == color) continue;
      if (StateFunctions.getActualVictoryPoints(state, nc) < 3) return true;
    }
    return false;
  }

  // ===== Dev card plays =====

  public static List<Action> monopolyPossibilities(Color color) {
    List<Action> actions = new ArrayList<>();
    for (Resource r : Resource.ALL) actions.add(new Action(color, ActionType.PLAY_MONOPOLY, r));
    return actions;
  }

  public static List<Action> yearOfPlentyPossibilities(Color color, int[] bankFreqdeck) {
    Set<String> seen = new HashSet<>();
    List<Action> actions = new ArrayList<>();
    Resource[] all = Resource.ALL;
    for (int i = 0; i < all.length; i++) {
      for (int j = i; j < all.length; j++) {
        Resource first = all[i], second = all[j];
        int[] toDraw = new int[5];
        toDraw[first.ordinal()]++;
        toDraw[second.ordinal()]++;
        if (Decks.freqdeckContains(bankFreqdeck, toDraw)) {
          String key = first.ordinal() + "," + second.ordinal();
          if (seen.add(key))
            actions.add(
                new Action(color, ActionType.PLAY_YEAR_OF_PLENTY, new Resource[] {first, second}));
        } else {
          if (Decks.freqdeckCanDraw(bankFreqdeck, 1, first)) {
            String k = first.ordinal() + ",";
            if (seen.add(k))
              actions.add(
                  new Action(color, ActionType.PLAY_YEAR_OF_PLENTY, new Resource[] {first}));
          }
          if (Decks.freqdeckCanDraw(bankFreqdeck, 1, second)) {
            String k = "," + second.ordinal();
            if (seen.add(k))
              actions.add(
                  new Action(color, ActionType.PLAY_YEAR_OF_PLENTY, new Resource[] {second}));
          }
        }
      }
    }
    return actions;
  }

  // ===== Maritime trade =====

  public static List<Action> maritimeTradePossibilities(GameState state, Color color) {
    int[] hand = state.playerState(color).resourcesInHand;
    Set<Resource> portResources = state.board.getPlayerPortResources(color);
    boolean hasGenericPort = state.board.hasGenericPort(color);
    List<Resource[]> offers =
        innerMaritimeTradePossibilities(
            hand, state.resourceFreqdeck, portResources, hasGenericPort);
    List<Action> actions = new ArrayList<>();
    for (Resource[] offer : offers)
      actions.add(new Action(color, ActionType.MARITIME_TRADE, offer));
    return actions;
  }

  static List<Resource[]> innerMaritimeTradePossibilities(
      int[] hand, int[] bank, Set<Resource> portResources, boolean hasGenericPort) {
    int[] rates = new int[5];
    Arrays.fill(rates, hasGenericPort ? 3 : 4);
    for (Resource r : portResources) rates[r.ordinal()] = 2;

    Set<String> seen = new HashSet<>();
    List<Resource[]> offers = new ArrayList<>();
    for (Resource giving : Resource.ALL) {
      int amount = hand[giving.ordinal()];
      int rate = rates[giving.ordinal()];
      if (amount < rate) continue;
      Resource[] offering = new Resource[5];
      Arrays.fill(offering, null);
      for (int k = 0; k < rate; k++) offering[k] = giving;
      for (Resource asking : Resource.ALL) {
        if (asking == giving) continue;
        if (Decks.freqdeckCount(bank, asking) <= 0) continue;
        String key = giving.ordinal() + "-" + asking.ordinal();
        if (seen.add(key)) {
          Resource[] offer = Arrays.copyOf(offering, 5);
          offer[4] = asking;
          offers.add(offer);
        }
      }
    }
    return offers;
  }

  // ===== Trade decisions =====

  private static List<Action> discardPossibilities(GameState state, Color color) {
    int remaining = state.discardCounts[state.playerIndex(color)];
    if (remaining <= 0) return List.of();
    List<Action> actions = new ArrayList<>();
    for (Resource r : Resource.ALL) {
      if (StateFunctions.playerNumResourceCards(state, color, r) > 0)
        actions.add(new Action(color, ActionType.DISCARD_RESOURCE, r));
    }
    return actions;
  }

  private static List<Action> decideTradePossibilities(GameState state, Color color) {
    List<Action> actions = new ArrayList<>();
    actions.add(new Action(color, ActionType.REJECT_TRADE, state.currentTrade.clone()));

    int[] hand = state.playerState(color).resourcesInHand;
    int[] asked = Arrays.copyOfRange(state.currentTrade, 5, 10);
    if (Decks.freqdeckContains(hand, asked))
      actions.add(new Action(color, ActionType.ACCEPT_TRADE, state.currentTrade.clone()));
    return actions;
  }

  private static List<Action> decideAccepteesPossibilities(GameState state, Color color) {
    List<Action> actions = new ArrayList<>();
    actions.add(new Action(color, ActionType.CANCEL_TRADE, null));
    for (int i = 0; i < state.colors.length; i++) {
      if (state.acceptees[i]) {
        int[] val = Arrays.copyOf(state.currentTrade, 11);
        val[10] = i;
        actions.add(new Action(color, ActionType.CONFIRM_TRADE, val));
      }
    }
    return actions;
  }

  // ===== Validation =====

  public static boolean isValidAction(
      List<Action> playableActions, GameState state, Action action) {
    if (action.actionType() == ActionType.OFFER_TRADE) {
      return state.currentColor() == action.color()
          && state.currentPrompt == ActionPrompt.PLAY_TURN
          && StateFunctions.playerHasRolled(state, action.color())
          && isValidTrade((int[]) action.value());
    }
    return playableActions.contains(action);
  }

  public static boolean isValidTrade(int[] value) {
    int[] offering = Arrays.copyOfRange(value, 0, 5);
    int[] asking = Arrays.copyOfRange(value, 5, 10);
    int offerSum = 0, askSum = 0;
    for (int v : offering) offerSum += v;
    for (int v : asking) askSum += v;
    if (offerSum == 0 || askSum == 0) return false;
    for (int i = 0; i < 5; i++) if (offering[i] > 0 && asking[i] > 0) return false;
    return true;
  }
}

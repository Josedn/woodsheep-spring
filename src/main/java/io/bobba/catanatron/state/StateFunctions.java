package io.bobba.catanatron.state;

import io.bobba.catanatron.enums.BuildingType;
import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.enums.DevCard;
import io.bobba.catanatron.enums.Resource;
import io.bobba.catanatron.models.Decks;
import io.bobba.catanatron.models.EdgeId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Pure static helpers that read/mutate GameState. Mirrors state_functions.py exactly. */
public final class StateFunctions {

  private StateFunctions() {}

  // ===== Longest Road =====

  public static void maintainLongestRoad(
      GameState state,
      Color previousRoadColor,
      Color roadColor,
      java.util.Map<Color, Integer> roadLengths) {

    for (Color c : state.colors) {
      state.playerState(c).longestRoadLength = roadLengths.getOrDefault(c, 0);
    }

    if (roadColor == null || roadColor == previousRoadColor) return;

    PlayerState winner = state.playerState(roadColor);
    winner.hasRoad = true;
    winner.victoryPoints += 2;
    winner.actualVictoryPoints += 2;

    if (previousRoadColor != null) {
      PlayerState loser = state.playerState(previousRoadColor);
      loser.hasRoad = false;
      loser.victoryPoints -= 2;
      loser.actualVictoryPoints -= 2;
    }
  }

  // ===== Largest Army =====

  public static void maintainLargestArmy(
      GameState state, Color color, Color previousArmyColor, int previousArmySize) {

    int candidateSize = getPlayedDevCards(state, color, DevCard.KNIGHT);
    if (candidateSize < 3) return;

    if (previousArmyColor == null) {
      PlayerState winner = state.playerState(color);
      winner.hasArmy = true;
      winner.victoryPoints += 2;
      winner.actualVictoryPoints += 2;
    } else if (previousArmySize < candidateSize && previousArmyColor != color) {
      PlayerState winner = state.playerState(color);
      winner.hasArmy = true;
      winner.victoryPoints += 2;
      winner.actualVictoryPoints += 2;

      PlayerState loser = state.playerState(previousArmyColor);
      loser.hasArmy = false;
      loser.victoryPoints -= 2;
      loser.actualVictoryPoints -= 2;
    }
  }

  // ===== Getters =====

  public static int getActualVictoryPoints(GameState state, Color color) {
    return state.playerState(color).actualVictoryPoints;
  }

  public static int getVisibleVictoryPoints(GameState state, Color color) {
    return state.playerState(color).victoryPoints;
  }

  public static Color getLongestRoadColor(GameState state) {
    for (Color c : state.colors) {
      if (state.playerState(c).hasRoad) return c;
    }
    return null;
  }

  /** Returns {color, knightsPlayed} or {null, 0} if no army. */
  public static Object[] getLargestArmy(GameState state) {
    for (Color c : state.colors) {
      PlayerState ps = state.playerState(c);
      if (ps.hasArmy) {
        return new Object[] {c, ps.devCardsPlayed[DevCard.KNIGHT.ordinal()]};
      }
    }
    return new Object[] {null, 0};
  }

  public static boolean playerHasRolled(GameState state, Color color) {
    return state.playerState(color).hasRolled;
  }

  public static int getLongestRoadLength(GameState state, Color color) {
    return state.playerState(color).longestRoadLength;
  }

  public static int getPlayedDevCards(GameState state, Color color, DevCard card) {
    return state.playerState(color).devCardsPlayed[card.ordinal()];
  }

  public static int getPlayedDevCardsTotal(GameState state, Color color) {
    int[] played = state.playerState(color).devCardsPlayed;
    // VP cards don't count toward "played" total (they're revealed on win, not played)
    return played[DevCard.KNIGHT.ordinal()]
        + played[DevCard.MONOPOLY.ordinal()]
        + played[DevCard.ROAD_BUILDING.ordinal()]
        + played[DevCard.YEAR_OF_PLENTY.ordinal()];
  }

  public static int getDevCardsInHand(GameState state, Color color, DevCard card) {
    return state.playerState(color).devCardsInHand[card.ordinal()];
  }

  public static int getDevCardsInHandTotal(GameState state, Color color) {
    int total = 0;
    for (int v : state.playerState(color).devCardsInHand) total += v;
    return total;
  }

  public static List<Integer> getPlayerBuildings(GameState state, Color color, BuildingType type) {
    return state.buildingsByColor.get(color).get(type);
  }

  public static int[] getPlayerFreqdeck(GameState state, Color color) {
    return state.playerState(color).resourcesInHand.clone();
  }

  public static int getStateIndex(GameState state) {
    return state.actionRecords.size();
  }

  public static int playerNumResourceCards(GameState state, Color color) {
    int total = 0;
    for (int v : state.playerState(color).resourcesInHand) total += v;
    return total;
  }

  public static int playerNumResourceCards(GameState state, Color color, Resource resource) {
    return state.playerState(color).resourcesInHand[resource.ordinal()];
  }

  public static int playerNumDevCards(GameState state, Color color) {
    return getDevCardsInHandTotal(state, color);
  }

  /** Expand the resource hand into a flat list of Resource values. */
  public static List<Resource> playerDeckToArray(GameState state, Color color) {
    int[] hand = state.playerState(color).resourcesInHand;
    List<Resource> result = new ArrayList<>();
    for (Resource r : Resource.ALL) {
      int count = hand[r.ordinal()];
      for (int i = 0; i < count; i++) result.add(r);
    }
    return result;
  }

  public static Resource playerDeckRandomSelect(GameState state, Color color, Random rng) {
    List<Resource> deck = playerDeckToArray(state, color);
    if (deck.isEmpty()) throw new IllegalStateException("Player has no resources");
    return deck.get(rng.nextInt(deck.size()));
  }

  public static boolean playerResourceFreqdeckContains(
      GameState state, Color color, int[] freqdeck) {
    int[] hand = state.playerState(color).resourcesInHand;
    for (int i = 0; i < 5; i++) if (hand[i] < freqdeck[i]) return false;
    return true;
  }

  public static boolean playerCanAffordDevCard(GameState state, Color color) {
    int[] hand = state.playerState(color).resourcesInHand;
    return hand[Resource.SHEEP.ordinal()] >= 1
        && hand[Resource.WHEAT.ordinal()] >= 1
        && hand[Resource.ORE.ordinal()] >= 1;
  }

  public static boolean playerCanPlayDev(GameState state, Color color, DevCard card) {
    PlayerState ps = state.playerState(color);
    if (ps.hasPlayedDevelopmentCardInTurn) return false;
    if (ps.devCardsInHand[card.ordinal()] < 1) return false;
    return switch (card) {
      case KNIGHT -> ps.knightOwnedAtStart;
      case MONOPOLY -> ps.monopolyOwnedAtStart;
      case YEAR_OF_PLENTY -> ps.yearOfPlentyOwnedAtStart;
      case ROAD_BUILDING -> ps.roadBuildingOwnedAtStart;
      default -> false;
    };
  }

  // ===== Mutators =====

  public static void buildSettlement(GameState state, Color color, int nodeId, boolean isFree) {
    state.buildingsByColor.get(color).get(BuildingType.SETTLEMENT).add(nodeId);
    PlayerState ps = state.playerState(color);
    ps.settlementsAvailable--;
    ps.victoryPoints++;
    ps.actualVictoryPoints++;
    if (!isFree) {
      ps.resourcesInHand[Resource.WOOD.ordinal()]--;
      ps.resourcesInHand[Resource.BRICK.ordinal()]--;
      ps.resourcesInHand[Resource.SHEEP.ordinal()]--;
      ps.resourcesInHand[Resource.WHEAT.ordinal()]--;
    }
  }

  public static void buildRoad(GameState state, Color color, EdgeId edge, boolean isFree) {
    state.buildingsByColor.get(color).get(BuildingType.ROAD).add(edge.a() * 100 + edge.b());
    PlayerState ps = state.playerState(color);
    ps.roadsAvailable--;
    if (!isFree) {
      ps.resourcesInHand[Resource.WOOD.ordinal()]--;
      ps.resourcesInHand[Resource.BRICK.ordinal()]--;
      // replenish bank
      state.resourceFreqdeck = Decks.freqdeckAdd(state.resourceFreqdeck, Decks.ROAD_COST);
    }
  }

  public static void buildCity(GameState state, Color color, int nodeId) {
    state.buildingsByColor.get(color).get(BuildingType.SETTLEMENT).remove((Integer) nodeId);
    state.buildingsByColor.get(color).get(BuildingType.CITY).add(nodeId);
    PlayerState ps = state.playerState(color);
    ps.settlementsAvailable++;
    ps.citiesAvailable--;
    ps.victoryPoints++;
    ps.actualVictoryPoints++;
    ps.resourcesInHand[Resource.WHEAT.ordinal()] -= 2;
    ps.resourcesInHand[Resource.ORE.ordinal()] -= 3;
  }

  public static void buyDevCard(GameState state, Color color, DevCard card) {
    PlayerState ps = state.playerState(color);
    ps.devCardsInHand[card.ordinal()]++;
    if (card == DevCard.VICTORY_POINT) ps.actualVictoryPoints++;
    ps.resourcesInHand[Resource.SHEEP.ordinal()]--;
    ps.resourcesInHand[Resource.WHEAT.ordinal()]--;
    ps.resourcesInHand[Resource.ORE.ordinal()]--;
  }

  public static void playerFreqdeckAdd(GameState state, Color color, int[] freqdeck) {
    int[] hand = state.playerState(color).resourcesInHand;
    for (int i = 0; i < 5; i++) hand[i] += freqdeck[i];
  }

  public static void playerFreqdeckSubtract(GameState state, Color color, int[] freqdeck) {
    int[] hand = state.playerState(color).resourcesInHand;
    for (int i = 0; i < 5; i++) hand[i] -= freqdeck[i];
  }

  public static void playerDeckDraw(GameState state, Color color, Resource resource, int amount) {
    state.playerState(color).resourcesInHand[resource.ordinal()] -= amount;
  }

  public static void playerDeckReplenish(
      GameState state, Color color, Resource resource, int amount) {
    state.playerState(color).resourcesInHand[resource.ordinal()] += amount;
  }

  public static void playDevCard(GameState state, Color color, DevCard card) {
    Color previousArmyColor = null;
    int previousArmySize = 0;
    if (card == DevCard.KNIGHT) {
      Object[] army = getLargestArmy(state);
      previousArmyColor = (Color) army[0];
      previousArmySize = (int) army[1];
    }

    PlayerState ps = state.playerState(color);
    ps.devCardsInHand[card.ordinal()]--;
    ps.hasPlayedDevelopmentCardInTurn = true;
    ps.devCardsPlayed[card.ordinal()]++;

    if (card == DevCard.KNIGHT) {
      maintainLargestArmy(state, color, previousArmyColor, previousArmySize);
    }
  }

  public static void playerCleanTurn(GameState state, Color color) {
    PlayerState ps = state.playerState(color);
    ps.hasPlayedDevelopmentCardInTurn = false;
    ps.hasRolled = false;
    ps.knightOwnedAtStart = ps.devCardsInHand[DevCard.KNIGHT.ordinal()] > 0;
    ps.monopolyOwnedAtStart = ps.devCardsInHand[DevCard.MONOPOLY.ordinal()] > 0;
    ps.yearOfPlentyOwnedAtStart = ps.devCardsInHand[DevCard.YEAR_OF_PLENTY.ordinal()] > 0;
    ps.roadBuildingOwnedAtStart = ps.devCardsInHand[DevCard.ROAD_BUILDING.ordinal()] > 0;
  }
}

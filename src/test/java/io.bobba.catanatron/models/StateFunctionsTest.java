package io.bobba.catanatron.models;

import static org.junit.jupiter.api.Assertions.*;

import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.enums.DevCard;
import io.bobba.catanatron.enums.Resource;
import io.bobba.catanatron.state.Action;
import io.bobba.catanatron.state.GameState;
import io.bobba.catanatron.state.Player;
import io.bobba.catanatron.state.PlayerState;
import io.bobba.catanatron.state.StateFunctions;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateFunctionsTest {

  static class StubPlayer implements Player {
    private final Color color;

    StubPlayer(Color c) {
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

  private GameState state;

  @BeforeEach
  void setUp() {
    CatanMap map = CatanMap.fromTemplate(MapTemplates.baseMap(), true, new Random(42));
    state =
        new GameState(
            List.of(new StubPlayer(Color.RED), new StubPlayer(Color.BLUE)),
            map,
            7,
            false,
            true,
            new Random(42));
  }

  // ===== Build actions =====

  @Test
  void buildSettlementAwardsVP() {
    int node = state.board.buildableNodeIds(Color.RED, true).get(0);
    state.board.buildSettlement(Color.RED, node, true);
    StateFunctions.buildSettlement(state, Color.RED, node, true);

    assertEquals(1, StateFunctions.getActualVictoryPoints(state, Color.RED));
    assertEquals(4, state.playerState(Color.RED).settlementsAvailable);
  }

  @Test
  void buildSettlementCostsResources() {
    int node = state.board.buildableNodeIds(Color.RED, true).get(0);
    state.board.buildSettlement(Color.RED, node, true);
    // Give RED enough resources
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.WOOD, 1);
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.BRICK, 1);
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.SHEEP, 1);
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.WHEAT, 1);

    StateFunctions.buildSettlement(state, Color.RED, node, false);
    int[] hand = state.playerState(Color.RED).resourcesInHand;
    assertEquals(0, hand[Resource.WOOD.ordinal()]);
    assertEquals(0, hand[Resource.BRICK.ordinal()]);
  }

  @Test
  void buildRoadDecrementsAvailable() {
    int node = state.board.buildableNodeIds(Color.RED, true).get(0);
    state.board.buildSettlement(Color.RED, node, true);
    StateFunctions.buildSettlement(state, Color.RED, node, true);
    EdgeId edge = state.board.buildableEdges(Color.RED).get(0);
    state.board.buildRoad(Color.RED, edge);
    StateFunctions.buildRoad(state, Color.RED, edge, true);

    assertEquals(14, state.playerState(Color.RED).roadsAvailable);
  }

  @Test
  void buildCityAwardsVPAndRefundsSettlement() {
    int node = state.board.buildableNodeIds(Color.RED, true).get(0);
    state.board.buildSettlement(Color.RED, node, true);
    StateFunctions.buildSettlement(state, Color.RED, node, true);

    // Give resources for city
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.WHEAT, 2);
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.ORE, 3);
    state.board.buildCity(Color.RED, node);
    StateFunctions.buildCity(state, Color.RED, node);

    assertEquals(2, StateFunctions.getActualVictoryPoints(state, Color.RED));
    assertEquals(5, state.playerState(Color.RED).settlementsAvailable);
    assertEquals(3, state.playerState(Color.RED).citiesAvailable);
  }

  // ===== Dev cards =====

  @Test
  void buyDevCardCostsResources() {
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.SHEEP, 1);
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.WHEAT, 1);
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.ORE, 1);
    StateFunctions.buyDevCard(state, Color.RED, DevCard.KNIGHT);

    assertEquals(1, state.playerState(Color.RED).devCardsInHand[DevCard.KNIGHT.ordinal()]);
    assertEquals(0, state.playerState(Color.RED).resourcesInHand[Resource.SHEEP.ordinal()]);
  }

  @Test
  void victoryPointDevCardUpdatesActualVP() {
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.SHEEP, 1);
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.WHEAT, 1);
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.ORE, 1);
    StateFunctions.buyDevCard(state, Color.RED, DevCard.VICTORY_POINT);

    assertEquals(1, StateFunctions.getActualVictoryPoints(state, Color.RED));
    assertEquals(0, StateFunctions.getVisibleVictoryPoints(state, Color.RED));
  }

  @Test
  void playDevCardMovesFromHandToPlayed() {
    state.playerState(Color.RED).devCardsInHand[DevCard.KNIGHT.ordinal()] = 1;
    state.playerState(Color.RED).knightOwnedAtStart = true;
    StateFunctions.playDevCard(state, Color.RED, DevCard.KNIGHT);

    assertEquals(0, state.playerState(Color.RED).devCardsInHand[DevCard.KNIGHT.ordinal()]);
    assertEquals(1, state.playerState(Color.RED).devCardsPlayed[DevCard.KNIGHT.ordinal()]);
    assertTrue(state.playerState(Color.RED).hasPlayedDevelopmentCardInTurn);
  }

  @Test
  void largestArmyAwardedAt3Knights() {
    state.playerState(Color.RED).devCardsPlayed[DevCard.KNIGHT.ordinal()] = 2;
    state.playerState(Color.RED).devCardsInHand[DevCard.KNIGHT.ordinal()] = 1;
    state.playerState(Color.RED).knightOwnedAtStart = true;
    StateFunctions.playDevCard(state, Color.RED, DevCard.KNIGHT);

    assertTrue(state.playerState(Color.RED).hasArmy);
    assertEquals(2, StateFunctions.getActualVictoryPoints(state, Color.RED));
  }

  // ===== Longest road =====

  @Test
  void maintainLongestRoadAwards2VP() {
    StateFunctions.maintainLongestRoad(state, null, Color.RED, Map.of(Color.RED, 5, Color.BLUE, 3));
    assertTrue(state.playerState(Color.RED).hasRoad);
    assertEquals(2, StateFunctions.getVisibleVictoryPoints(state, Color.RED));
  }

  @Test
  void maintainLongestRoadTransfersVP() {
    state.playerState(Color.RED).hasRoad = true;
    state.playerState(Color.RED).victoryPoints = 2;
    state.playerState(Color.RED).actualVictoryPoints = 2;

    StateFunctions.maintainLongestRoad(
        state, Color.RED, Color.BLUE, Map.of(Color.RED, 4, Color.BLUE, 6));
    assertFalse(state.playerState(Color.RED).hasRoad);
    assertEquals(0, StateFunctions.getVisibleVictoryPoints(state, Color.RED));
    assertTrue(state.playerState(Color.BLUE).hasRoad);
    assertEquals(2, StateFunctions.getVisibleVictoryPoints(state, Color.BLUE));
  }

  // ===== Clean turn =====

  @Test
  void playerCleanTurnResetsFlags() {
    PlayerState ps = state.playerState(Color.RED);
    ps.hasRolled = true;
    ps.hasPlayedDevelopmentCardInTurn = true;
    ps.devCardsInHand[DevCard.KNIGHT.ordinal()] = 2;

    StateFunctions.playerCleanTurn(state, Color.RED);

    assertFalse(ps.hasRolled);
    assertFalse(ps.hasPlayedDevelopmentCardInTurn);
    assertTrue(ps.knightOwnedAtStart);
  }

  // ===== Resource helpers =====

  @Test
  void playerDeckToArrayReflectsHand() {
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.WOOD, 3);
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.ORE, 1);
    List<Resource> deck = StateFunctions.playerDeckToArray(state, Color.RED);
    assertEquals(4, deck.size());
    assertEquals(3, deck.stream().filter(r -> r == Resource.WOOD).count());
  }

  @Test
  void playerCanAffordDevCard() {
    assertFalse(StateFunctions.playerCanAffordDevCard(state, Color.RED));
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.SHEEP, 1);
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.WHEAT, 1);
    StateFunctions.playerDeckReplenish(state, Color.RED, Resource.ORE, 1);
    assertTrue(StateFunctions.playerCanAffordDevCard(state, Color.RED));
  }
}

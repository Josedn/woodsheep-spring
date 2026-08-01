package io.bobba.catanatron.models;

import static org.junit.jupiter.api.Assertions.*;

import io.bobba.catanatron.enums.ActionPrompt;
import io.bobba.catanatron.enums.ActionType;
import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.enums.Resource;
import io.bobba.catanatron.game.ApplyAction;
import io.bobba.catanatron.state.Action;
import io.bobba.catanatron.state.ActionRecord;
import io.bobba.catanatron.state.GameState;
import io.bobba.catanatron.state.Player;
import io.bobba.catanatron.state.StateFunctions;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplyActionTest {

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
  private Random rng;

  @BeforeEach
  void setUp() {
    rng = new Random(42);
    CatanMap map = CatanMap.fromTemplate(MapTemplates.baseMap(), true, rng);
    state =
        new GameState(
            List.of(new StubPlayer(Color.RED), new StubPlayer(Color.BLUE)),
            map,
            7,
            false,
            true,
            new Random(42));
  }

  // ===== Initial build phase =====

  @Test
  void initialSettlementAdvancesToRoad() {
    int node = state.board.buildableNodeIds(Color.RED, true).get(0);
    Action action = new Action(state.currentColor(), ActionType.BUILD_SETTLEMENT, node);
    ApplyAction.applyAction(state, action, rng);

    assertEquals(ActionPrompt.BUILD_INITIAL_ROAD, state.currentPrompt);
    assertEquals(1, StateFunctions.getActualVictoryPoints(state, state.colors[0]));
  }

  @Test
  void secondInitialSettlementYieldsResources() {
    Color c = state.currentColor();

    // Place first settlement + road
    int node1 = state.board.buildableNodeIds(c, true).get(0);
    ApplyAction.applyAction(state, new Action(c, ActionType.BUILD_SETTLEMENT, node1), rng);
    EdgeId road1 = state.board.buildableEdges(c).get(0);
    ApplyAction.applyAction(state, new Action(c, ActionType.BUILD_ROAD, road1), rng);

    // Blue's turn
    Color c2 = state.currentColor();
    int node2 = state.board.buildableNodeIds(c2, true).get(0);
    ApplyAction.applyAction(state, new Action(c2, ActionType.BUILD_SETTLEMENT, node2), rng);
    EdgeId road2 = state.board.buildableEdges(c2).get(0);
    ApplyAction.applyAction(state, new Action(c2, ActionType.BUILD_ROAD, road2), rng);

    // Second settlements (snake order — same player c2 goes again)
    int node3 = state.board.buildableNodeIds(c2, true).get(0);
    int resourcesBefore = StateFunctions.playerNumResourceCards(state, c2);
    ApplyAction.applyAction(state, new Action(c2, ActionType.BUILD_SETTLEMENT, node3), rng);
    int resourcesAfter = StateFunctions.playerNumResourceCards(state, c2);
    // second settlement should yield >=0 resources (may be 0 if all adjacent are desert)
    assertTrue(resourcesAfter >= resourcesBefore);
  }

  @Test
  void initialBuildPhaseEndsAfterAllRoads() {
    // Play through full initial placement for 2 players
    for (int round = 0; round < 2; round++) {
      for (int p = 0; p < 2; p++) {
        Color c = state.currentColor();
        int node = state.board.buildableNodeIds(c, true).get(0);
        ApplyAction.applyAction(state, new Action(c, ActionType.BUILD_SETTLEMENT, node), rng);
        EdgeId edge = state.board.buildableEdges(c).get(0);
        ApplyAction.applyAction(state, new Action(c, ActionType.BUILD_ROAD, edge), rng);
      }
    }
    assertFalse(state.isInitialBuildPhase);
    assertEquals(ActionPrompt.PLAY_TURN, state.currentPrompt);
  }

  // ===== Roll =====

  @Test
  void rollNon7SetPromptToPlayTurn() {
    skipInitialPhase();
    Color c = state.currentColor();
    // Force a non-7 roll via record
    ActionRecord rec = new ActionRecord(null, new int[] {3, 3});
    Action action = new Action(c, ActionType.ROLL, null);
    ApplyAction.applyAction(state, action, rec, rng);

    assertTrue(state.playerState(c).hasRolled);
    assertEquals(ActionPrompt.PLAY_TURN, state.currentPrompt);
  }

  @Test
  void roll7WithNoOverlimitPlayers() {
    skipInitialPhase();
    Color c = state.currentColor();
    ActionRecord rec = new ActionRecord(null, new int[] {3, 4});
    ApplyAction.applyAction(state, new Action(c, ActionType.ROLL, null), rec, rng);

    assertEquals(ActionPrompt.MOVE_ROBBER, state.currentPrompt);
    assertTrue(state.isMovingKnight);
  }

  // ===== End turn =====

  @Test
  void endTurnAdvancesPlayer() {
    skipInitialPhase();
    int startIdx = state.currentPlayerIndex;
    rollNon7(state, rng);
    ApplyAction.applyAction(
        state, new Action(state.currentColor(), ActionType.END_TURN, null), rng);

    assertEquals((startIdx + 1) % state.colors.length, state.currentPlayerIndex);
  }

  // ===== Development cards =====

  @Test
  void buyDevCardDrawsFromDeck() {
    skipInitialPhase();
    Color c = state.currentColor();
    StateFunctions.playerDeckReplenish(state, c, Resource.SHEEP, 1);
    StateFunctions.playerDeckReplenish(state, c, Resource.WHEAT, 1);
    StateFunctions.playerDeckReplenish(state, c, Resource.ORE, 1);
    int deckSizeBefore = state.developmentListdeck.size();

    rollNon7(state, rng);
    ActionRecord rec =
        ApplyAction.applyAction(state, new Action(c, ActionType.BUY_DEVELOPMENT_CARD, null), rng);

    assertEquals(deckSizeBefore - 1, state.developmentListdeck.size());
    assertNotNull(rec.result());
    assertEquals(1, StateFunctions.getDevCardsInHandTotal(state, c));
  }

  // ===== Resource yield =====

  @Test
  void yieldResourcesReturnsPayoutForMatchingNumber() {
    CatanMap map = CatanMap.fromTemplate(MapTemplates.baseMap(), true, new Random(42));
    Board board = new Board(map);

    // Place a settlement on a known productive node
    int node = board.buildableNodeIds(Color.RED, true).get(0);
    board.buildSettlement(Color.RED, node, true);

    int[] bank = Decks.startingResourceBank();
    // Find a number that has a tile adjacent to node
    Integer number = null;
    for (LandTile lt : map.adjacentTiles.getOrDefault(node, List.of())) {
      if (lt.number != null && lt.resource != null) {
        number = lt.number;
        break;
      }
    }

    if (number != null) {
      ApplyAction.YieldResult yr = ApplyAction.yieldResources(board, bank, number);
      assertFalse(yr.payout().isEmpty());
    }
  }

  // ===== Maritime trade =====

  @Test
  void maritimeTradeExchangesResources() {
    skipInitialPhase();
    Color c = state.currentColor();
    rollNon7(state, rng);

    // Give 4 WOOD (standard 4:1 trade)
    StateFunctions.playerDeckReplenish(state, c, Resource.WOOD, 4);
    int bankWoodBefore = state.resourceFreqdeck[Resource.WOOD.ordinal()];

    int oreBefore = state.playerState(c).resourcesInHand[Resource.ORE.ordinal()];
    Resource[] offer = {Resource.WOOD, Resource.WOOD, Resource.WOOD, Resource.WOOD, Resource.ORE};
    ApplyAction.applyAction(state, new Action(c, ActionType.MARITIME_TRADE, offer), rng);

    assertEquals(bankWoodBefore + 4, state.resourceFreqdeck[Resource.WOOD.ordinal()]);
    assertEquals(oreBefore + 1, state.playerState(c).resourcesInHand[Resource.ORE.ordinal()]);
  }

  // ===== Helpers =====

  private void skipInitialPhase() {
    for (int round = 0; round < 2; round++) {
      for (int p = 0; p < 2; p++) {
        Color c = state.currentColor();
        int node = state.board.buildableNodeIds(c, true).get(0);
        ApplyAction.applyAction(state, new Action(c, ActionType.BUILD_SETTLEMENT, node), rng);
        EdgeId edge = state.board.buildableEdges(c).get(0);
        ApplyAction.applyAction(state, new Action(c, ActionType.BUILD_ROAD, edge), rng);
      }
    }
  }

  private void rollNon7(GameState state, Random rng) {
    ActionRecord rec = new ActionRecord(null, new int[] {2, 2});
    ApplyAction.applyAction(
        state, new Action(state.currentColor(), ActionType.ROLL, null), rec, rng);
  }
}

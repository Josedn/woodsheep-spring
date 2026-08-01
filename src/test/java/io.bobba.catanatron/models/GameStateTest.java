package io.bobba.catanatron.models;

import static org.junit.jupiter.api.Assertions.*;

import io.bobba.catanatron.enums.ActionPrompt;
import io.bobba.catanatron.enums.BuildingType;
import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.enums.DevCard;
import io.bobba.catanatron.state.Action;
import io.bobba.catanatron.state.GameState;
import io.bobba.catanatron.state.Player;
import io.bobba.catanatron.state.PlayerState;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameStateTest {

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
  private CatanMap map;

  @BeforeEach
  void setUp() {
    map = CatanMap.fromTemplate(MapTemplates.baseMap(), true, new Random(42));
    List<Player> players = List.of(new StubPlayer(Color.RED), new StubPlayer(Color.BLUE));
    state = new GameState(players, map, 7, false, true, new Random(42));
  }

  @Test
  void colorsContainsAllPlayers() {
    assertEquals(2, state.colors.length);
    boolean hasRed = false, hasBlue = false;
    for (Color c : state.colors) {
      if (c == Color.RED) hasRed = true;
      if (c == Color.BLUE) hasBlue = true;
    }
    assertTrue(hasRed && hasBlue);
  }

  @Test
  void colorToIndexIsConsistent() {
    for (int i = 0; i < state.colors.length; i++) {
      assertEquals(i, state.colorToIndex.get(state.colors[i]));
    }
  }

  @Test
  void initialPromptIsBuildInitialSettlement() {
    assertEquals(ActionPrompt.BUILD_INITIAL_SETTLEMENT, state.currentPrompt);
    assertTrue(state.isInitialBuildPhase);
  }

  @Test
  void initialBankHas19OfEachResource() {
    for (int count : state.resourceFreqdeck) assertEquals(19, count);
  }

  @Test
  void developmentListdeckHas25Cards() {
    assertEquals(25, state.developmentListdeck.size());
  }

  @Test
  void playerStatesInitialised() {
    for (Color c : state.colors) {
      PlayerState ps = state.playerState(c);
      assertNotNull(ps);
      assertEquals(0, ps.victoryPoints);
      assertEquals(15, ps.roadsAvailable);
      assertEquals(5, ps.settlementsAvailable);
      assertEquals(4, ps.citiesAvailable);
      assertFalse(ps.hasRolled);
    }
  }

  @Test
  void buildingsByColorInitialisedEmpty() {
    for (Color c : state.colors) {
      for (BuildingType bt : BuildingType.values()) {
        assertTrue(state.buildingsByColor.get(c).get(bt).isEmpty());
      }
    }
  }

  @Test
  void currentColorReturnsFirstPlayer() {
    assertEquals(state.colors[0], state.currentColor());
  }

  @Test
  void copyIsIndependent() {
    GameState copy = state.copy();

    // Mutate original
    state.playerState(state.colors[0]).victoryPoints = 10;
    state.resourceFreqdeck[0] = 0;
    state.numTurns = 5;

    // Copy unaffected
    assertEquals(0, copy.playerState(copy.colors[0]).victoryPoints);
    assertEquals(19, copy.resourceFreqdeck[0]);
    assertEquals(0, copy.numTurns);
  }

  @Test
  void copySharesColorArrayReference() {
    // colors is immutable — copy can share it
    GameState copy = state.copy();
    assertArrayEquals(state.colors, copy.colors);
  }

  @Test
  void playerStateDevCardsCopyIndependent() {
    state.playerState(state.colors[0]).devCardsInHand[DevCard.KNIGHT.ordinal()] = 3;
    GameState copy = state.copy();
    copy.playerState(copy.colors[0]).devCardsInHand[DevCard.KNIGHT.ordinal()] = 0;
    assertEquals(3, state.playerState(state.colors[0]).devCardsInHand[DevCard.KNIGHT.ordinal()]);
  }
}

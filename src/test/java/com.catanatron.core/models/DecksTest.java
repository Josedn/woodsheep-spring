package com.catanatron.core.models;

import static org.junit.jupiter.api.Assertions.*;

import com.catanatron.core.enums.DevCard;
import com.catanatron.core.enums.Resource;
import java.util.List;
import org.junit.jupiter.api.Test;

class DecksTest {

  @Test
  void startingResourceBankHas19OfEach() {
    int[] bank = Decks.startingResourceBank();
    assertEquals(5, bank.length);
    for (int count : bank) assertEquals(19, count);
  }

  @Test
  void freqdeckDrawReducesCount() {
    int[] bank = Decks.startingResourceBank();
    Decks.freqdeckDraw(bank, 3, Resource.WOOD);
    assertEquals(16, Decks.freqdeckCount(bank, Resource.WOOD));
  }

  @Test
  void freqdeckReplenishIncreasesCount() {
    int[] bank = new int[] {0, 0, 0, 0, 0};
    Decks.freqdeckReplenish(bank, 2, Resource.ORE);
    assertEquals(2, Decks.freqdeckCount(bank, Resource.ORE));
  }

  @Test
  void freqdeckCanDrawReturnsFalseWhenInsufficient() {
    int[] bank = new int[] {1, 0, 0, 0, 0};
    assertFalse(Decks.freqdeckCanDraw(bank, 2, Resource.WOOD));
    assertTrue(Decks.freqdeckCanDraw(bank, 1, Resource.WOOD));
  }

  @Test
  void freqdeckFromListdeck() {
    List<Resource> hand = List.of(Resource.WOOD, Resource.WOOD, Resource.ORE);
    int[] freq = Decks.freqdeckFromListdeck(hand);
    assertEquals(2, freq[Resource.WOOD.ordinal()]);
    assertEquals(1, freq[Resource.ORE.ordinal()]);
    assertEquals(0, freq[Resource.BRICK.ordinal()]);
  }

  @Test
  void freqdeckAdd() {
    int[] a = {1, 2, 0, 0, 0};
    int[] b = {0, 1, 3, 0, 0};
    int[] result = Decks.freqdeckAdd(a, b);
    assertArrayEquals(new int[] {1, 3, 3, 0, 0}, result);
  }

  @Test
  void freqdeckSubtract() {
    int[] a = {3, 3, 3, 3, 3};
    int[] b = {1, 1, 1, 1, 1};
    assertArrayEquals(new int[] {2, 2, 2, 2, 2}, Decks.freqdeckSubtract(a, b));
  }

  @Test
  void freqdeckContains() {
    int[] hand = {2, 1, 1, 1, 0};
    assertTrue(Decks.freqdeckContains(hand, Decks.SETTLEMENT_COST));
    assertFalse(Decks.freqdeckContains(hand, Decks.CITY_COST));
  }

  @Test
  void startingDevCardBankHas25Cards() {
    List<DevCard> deck = Decks.startingDevCardBank();
    assertEquals(25, deck.size());
  }

  @Test
  void startingDevCardBankComposition() {
    List<DevCard> deck = Decks.startingDevCardBank();
    assertEquals(14, deck.stream().filter(c -> c == DevCard.KNIGHT).count());
    assertEquals(2, deck.stream().filter(c -> c == DevCard.YEAR_OF_PLENTY).count());
    assertEquals(2, deck.stream().filter(c -> c == DevCard.ROAD_BUILDING).count());
    assertEquals(2, deck.stream().filter(c -> c == DevCard.MONOPOLY).count());
    assertEquals(5, deck.stream().filter(c -> c == DevCard.VICTORY_POINT).count());
  }

  @Test
  void drawFromListdeck() {
    List<DevCard> deck = Decks.startingDevCardBank();
    Decks.drawFromListdeck(deck, 2, DevCard.KNIGHT);
    assertEquals(12, deck.stream().filter(c -> c == DevCard.KNIGHT).count());
    assertEquals(23, deck.size());
  }

  @Test
  void startingDevCardProbaKnight() {
    double proba = Decks.startingDevCardProba(DevCard.KNIGHT);
    assertEquals(14.0 / 25.0, proba, 1e-9);
  }

  @Test
  void buildCostsAreCorrect() {
    assertArrayEquals(new int[] {1, 1, 0, 0, 0}, Decks.ROAD_COST);
    assertArrayEquals(new int[] {1, 1, 1, 1, 0}, Decks.SETTLEMENT_COST);
    assertArrayEquals(new int[] {0, 0, 0, 2, 3}, Decks.CITY_COST);
    assertArrayEquals(new int[] {0, 0, 1, 1, 1}, Decks.DEVELOPMENT_CARD_COST);
  }
}

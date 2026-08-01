package io.bobba.catanatron.models;

import io.bobba.catanatron.enums.DevCard;
import io.bobba.catanatron.enums.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for the two deck representations used throughout the engine:
 *
 * <p>freqdeck — int[5] histogram over Resource.ordinal() (WOOD=0 … ORE=4) listdeck — List<DevCard>
 * for the development card draw pile
 */
public final class Decks {

  // Build costs as freqdecks [WOOD, BRICK, SHEEP, WHEAT, ORE]
  public static final int[] ROAD_COST = {1, 1, 0, 0, 0};
  public static final int[] SETTLEMENT_COST = {1, 1, 1, 1, 0};
  public static final int[] CITY_COST = {0, 0, 0, 2, 3};
  public static final int[] DEVELOPMENT_CARD_COST = {0, 0, 1, 1, 1};

  private Decks() {}

  // ===== Resource freqdeck =====

  public static int[] startingResourceBank() {
    return new int[] {19, 19, 19, 19, 19};
  }

  public static boolean freqdeckCanDraw(int[] deck, int amount, Resource card) {
    return deck[card.ordinal()] >= amount;
  }

  public static void freqdeckDraw(int[] deck, int amount, Resource card) {
    deck[card.ordinal()] -= amount;
  }

  public static void freqdeckReplenish(int[] deck, int amount, Resource card) {
    deck[card.ordinal()] += amount;
  }

  public static int freqdeckCount(int[] deck, Resource card) {
    return deck[card.ordinal()];
  }

  public static int[] freqdeckFromListdeck(Iterable<Resource> listdeck) {
    int[] freq = new int[5];
    for (Resource r : listdeck) {
      freq[r.ordinal()]++;
    }
    return freq;
  }

  public static int[] freqdeckAdd(int[] a, int[] b) {
    int[] result = new int[5];
    for (int i = 0; i < 5; i++) result[i] = a[i] + b[i];
    return result;
  }

  public static int[] freqdeckSubtract(int[] a, int[] b) {
    int[] result = new int[5];
    for (int i = 0; i < 5; i++) result[i] = a[i] - b[i];
    return result;
  }

  /** True if a >= b element-wise (a can afford b). */
  public static boolean freqdeckContains(int[] a, int[] b) {
    for (int i = 0; i < 5; i++) {
      if (a[i] < b[i]) return false;
    }
    return true;
  }

  // ===== Dev card listdeck =====

  public static List<DevCard> startingDevCardBank() {
    List<DevCard> deck = new ArrayList<>(25);
    for (int i = 0; i < 14; i++) deck.add(DevCard.KNIGHT);
    for (int i = 0; i < 2; i++) deck.add(DevCard.YEAR_OF_PLENTY);
    for (int i = 0; i < 2; i++) deck.add(DevCard.ROAD_BUILDING);
    for (int i = 0; i < 2; i++) deck.add(DevCard.MONOPOLY);
    for (int i = 0; i < 5; i++) deck.add(DevCard.VICTORY_POINT);
    return deck;
  }

  public static double startingDevCardProba(DevCard card) {
    List<DevCard> deck = startingDevCardBank();
    long count = deck.stream().filter(c -> c == card).count();
    return (double) count / deck.size();
  }

  /** Removes {@code amount} copies of {@code card} from {@code listdeck} in place. */
  public static void drawFromListdeck(List<DevCard> listdeck, int amount, DevCard card) {
    for (int i = 0; i < amount; i++) {
      listdeck.remove(card);
    }
  }
}

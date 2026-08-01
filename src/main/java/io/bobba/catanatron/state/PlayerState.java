package io.bobba.catanatron.state;

/**
 * Per-player mutable state, mirroring the PLAYER_INITIAL_STATE dict. Stored flat (no map) for
 * performance.
 */
public class PlayerState {

  // Piece counts
  public int victoryPoints = 0;
  public int actualVictoryPoints = 0;
  public int roadsAvailable = 15;
  public int settlementsAvailable = 5;
  public int citiesAvailable = 4;

  // Flags
  public boolean hasRoad = false;
  public boolean hasArmy = false;
  public boolean hasRolled = false;
  public boolean hasPlayedDevelopmentCardInTurn = false;

  // De-normalised features
  public int longestRoadLength = 0;
  public boolean knightOwnedAtStart = false;
  public boolean monopolyOwnedAtStart = false;
  public boolean yearOfPlentyOwnedAtStart = false;
  public boolean roadBuildingOwnedAtStart = false;

  // Resources in hand [WOOD, BRICK, SHEEP, WHEAT, ORE]
  public final int[] resourcesInHand = new int[5];

  // Dev cards in hand [KNIGHT, YEAR_OF_PLENTY, MONOPOLY, ROAD_BUILDING, VICTORY_POINT]
  public final int[] devCardsInHand = new int[5];

  // Played dev cards [same order]
  public final int[] devCardsPlayed = new int[5];

  public PlayerState copy() {
    PlayerState c = new PlayerState();
    c.victoryPoints = victoryPoints;
    c.actualVictoryPoints = actualVictoryPoints;
    c.roadsAvailable = roadsAvailable;
    c.settlementsAvailable = settlementsAvailable;
    c.citiesAvailable = citiesAvailable;
    c.hasRoad = hasRoad;
    c.hasArmy = hasArmy;
    c.hasRolled = hasRolled;
    c.hasPlayedDevelopmentCardInTurn = hasPlayedDevelopmentCardInTurn;
    c.longestRoadLength = longestRoadLength;
    c.knightOwnedAtStart = knightOwnedAtStart;
    c.monopolyOwnedAtStart = monopolyOwnedAtStart;
    c.yearOfPlentyOwnedAtStart = yearOfPlentyOwnedAtStart;
    c.roadBuildingOwnedAtStart = roadBuildingOwnedAtStart;
    System.arraycopy(resourcesInHand, 0, c.resourcesInHand, 0, 5);
    System.arraycopy(devCardsInHand, 0, c.devCardsInHand, 0, 5);
    System.arraycopy(devCardsPlayed, 0, c.devCardsPlayed, 0, 5);
    return c;
  }
}

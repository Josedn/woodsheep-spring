package io.bobba.woodsheep.game.model;

import java.util.EnumMap;

public class PlayerState {
  public int victoryPoints = 0;
  public int roadsAvailable = 15;
  public int settlementsAvailable = 5;
  public int citiesAvailable = 4;
  public boolean hasRoad = false;
  public boolean hasArmy = false;
  public boolean hasRolled = false;
  public boolean hasPlayerDevCardInTurn = false;
  // de-normalized features (for performance since we think they are good features)
  public int actualVictoryPoints = 0;
  public int longestRoadLength = 0;

  public final EnumMap<Resource, Integer> resourcesInHand;
  public final EnumMap<DevCard, Integer> devCardsInHand;
  public final EnumMap<DevCard, Boolean> devCardsOwnedAtStart;

  public PlayerState() {
    this.resourcesInHand = new EnumMap<>(Resource.class);
    this.devCardsInHand = new EnumMap<>(DevCard.class);
    this.devCardsOwnedAtStart = new EnumMap<>(DevCard.class);
    for (Resource resource : Resource.values()) {
      resourcesInHand.put(resource, 0);
    }
    for (DevCard devCard : DevCard.values()) {
      devCardsInHand.put(devCard, 0);
      devCardsOwnedAtStart.put(devCard, false);
    }
  }

  public void addResource(Resource resource, int amount) {
    this.resourcesInHand.compute(resource, (k, currentValue) -> currentValue + amount);
  }
}

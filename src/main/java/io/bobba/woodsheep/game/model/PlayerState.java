package io.bobba.woodsheep.game.model;

import lombok.Value;

@Value
public class PlayerState {
  int victoryPoints = 0;
  int roadsAvailable = 15;
  int settlementsAvailable = 5;
  int citiesAvailable = 4;
  boolean hasRoad = false;
  boolean hasArmy = false;
  boolean hasRolled = false;
  boolean hasPlayerDevCardInTurn = false;
  // de-normalized features (for performance since we think they are good features)
  int actualVictoryPoints = 0;
  int longestRoadLength = 0;
  boolean knightOwnedAtStart = false;
  boolean monopolyOwnedAtStart = false;
  boolean yearOfPlentyOwnedAtStart = false;
  boolean roadBuildingOwnedAtStart = false;
  int woodInHand = 0;
  int brickInHand = 0;
  int sheepInHand = 0;
  int wheatInHand = 0;
  int oreInHand = 0;
}

package com.catanatron.core.enums;

public enum ActionType {
  // value: null
  ROLL,
  // value: coordinate + Color (nullable)
  MOVE_ROBBER,
  // value: Resource (one resource to discard; repeated per required discard count)
  DISCARD_RESOURCE,

  // Building/Buying — value: edge_id or node_id
  BUILD_ROAD,
  BUILD_SETTLEMENT,
  BUILD_CITY,
  // value: null
  BUY_DEVELOPMENT_CARD,

  // Dev card plays
  // value: null
  PLAY_KNIGHT_CARD,
  // value: Resource[2]
  PLAY_YEAR_OF_PLENTY,
  // value: Resource
  PLAY_MONOPOLY,
  // value: null
  PLAY_ROAD_BUILDING,

  // Maritime trade — value: Resource[5], index 4 is resource requested; indices 2-3 may be null
  MARITIME_TRADE,
  // Domestic trade — value: int[10] freqdeck pair (offered + receiving)
  OFFER_TRADE,
  ACCEPT_TRADE,
  REJECT_TRADE,
  // value: int[10] freqdeck pair + accepting player color
  CONFIRM_TRADE,
  // value: null
  CANCEL_TRADE,

  // value: null
  END_TURN;

  @Override
  public String toString() {
    return "AT." + name();
  }
}

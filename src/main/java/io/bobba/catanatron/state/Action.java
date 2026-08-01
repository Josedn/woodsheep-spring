package io.bobba.catanatron.state;

import io.bobba.catanatron.enums.ActionType;
import io.bobba.catanatron.enums.Color;

/**
 * Represents a player's intent: which action type and its parameters (value). Immutable by design —
 * value is polymorphic based on actionType.
 */
public record Action(Color color, ActionType actionType, Object value) {

  public static Action of(Color color, ActionType actionType) {
    return new Action(color, actionType, null);
  }
}

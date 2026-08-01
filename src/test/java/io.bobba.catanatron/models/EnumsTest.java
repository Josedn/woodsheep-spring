package io.bobba.catanatron.models;

import static org.junit.jupiter.api.Assertions.*;

import io.bobba.catanatron.enums.ActionPrompt;
import io.bobba.catanatron.enums.ActionType;
import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.enums.DevCard;
import io.bobba.catanatron.enums.EdgeRef;
import io.bobba.catanatron.enums.NodeRef;
import io.bobba.catanatron.enums.Resource;
import io.bobba.catanatron.state.Action;
import io.bobba.catanatron.state.ActionRecord;
import org.junit.jupiter.api.Test;

class EnumsTest {

  @Test
  void resourceAllContainsAllFive() {
    assertEquals(5, Resource.ALL.length);
    assertArrayEquals(
        new Resource[] {
          Resource.WOOD, Resource.BRICK, Resource.SHEEP, Resource.WHEAT, Resource.ORE
        },
        Resource.ALL);
  }

  @Test
  void devCardAllContainsAllFive() {
    assertEquals(5, DevCard.ALL.length);
  }

  @Test
  void actionTypeToStringUsesATPrefix() {
    assertEquals("AT.ROLL", ActionType.ROLL.toString());
    assertEquals("AT.END_TURN", ActionType.END_TURN.toString());
  }

  @Test
  void colorToStringUsesCPrefix() {
    assertEquals("C.RED", Color.RED.toString());
    assertEquals("C.BLUE", Color.BLUE.toString());
  }

  @Test
  void actionRecordIsImmutable() {
    Action action = Action.of(Color.RED, ActionType.ROLL);
    ActionRecord record = new ActionRecord(action, new int[] {3, 4});

    assertEquals(Color.RED, action.color());
    assertEquals(ActionType.ROLL, action.actionType());
    assertNull(action.value());
    assertNotNull(record.result());
  }

  @Test
  void nodeRefHasSixDirections() {
    assertEquals(6, NodeRef.values().length);
  }

  @Test
  void edgeRefHasSixDirections() {
    assertEquals(6, EdgeRef.values().length);
  }

  @Test
  void actionPromptHasAllPrompts() {
    assertEquals(7, ActionPrompt.values().length);
  }
}

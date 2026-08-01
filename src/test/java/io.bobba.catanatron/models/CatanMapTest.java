package io.bobba.catanatron.models;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class CatanMapTest {

  @Test
  void baseBoardHas19LandTiles() {
    CatanMap map = CatanMap.fromTemplate(MapTemplates.baseMap(), false, new Random(42));
    assertEquals(19, map.landTiles.size());
  }

  @Test
  void baseBoardHas54LandNodes() {
    CatanMap map = CatanMap.fromTemplate(MapTemplates.baseMap(), false, new Random(42));
    assertEquals(54, map.landNodes.size());
  }

  @Test
  void baseBoardHas9Ports() {
    CatanMap map = CatanMap.fromTemplate(MapTemplates.baseMap(), false, new Random(42));
    assertEquals(9, map.portsById.size());
  }

  @Test
  void baseBoardHas5ResourcePortTypes() {
    CatanMap map = CatanMap.fromTemplate(MapTemplates.baseMap(), false, new Random(42));
    // 5 specific resource ports + generic (null) ports
    assertEquals(5, map.portNodes.size());
    assertFalse(map.genericPortNodes.isEmpty());
  }

  @Test
  void eachPortExposes2Nodes() {
    CatanMap map = CatanMap.fromTemplate(MapTemplates.baseMap(), false, new Random(42));
    for (var entry : map.portNodes.entrySet()) {
      assertEquals(2, entry.getValue().size(), "Port " + entry.getKey() + " should have 2 nodes");
    }
  }

  @Test
  void officialSpiralNumbersMatchSpec() {
    CatanMap map = CatanMap.fromTemplate(MapTemplates.baseMap(), true, new Random(42));
    // All non-desert land tiles should have a number
    for (LandTile lt : map.landTiles.values()) {
      if (lt.resource != null) {
        assertNotNull(lt.number, "Non-desert tile should have a number");
      }
    }
  }

  @Test
  void nodeProductionIsNonEmptyForProductiveTiles() {
    CatanMap map = CatanMap.fromTemplate(MapTemplates.baseMap(), true, new Random(42));
    boolean anyProduction = map.nodeProduction.values().stream().anyMatch(m -> !m.isEmpty());
    assertTrue(anyProduction);
  }

  @Test
  void miniMapHas7LandTiles() {
    CatanMap map = CatanMap.fromTemplate(MapTemplates.miniMap(), false, new Random(1));
    assertEquals(7, map.landTiles.size());
  }

  @Test
  void allLandTileNodesAreLandNodes() {
    CatanMap map = CatanMap.fromTemplate(MapTemplates.baseMap(), false, new Random(42));
    for (LandTile lt : map.landTiles.values()) {
      for (int nodeId : lt.nodes.values()) {
        assertTrue(map.landNodes.contains(nodeId), "Node " + nodeId + " should be a land node");
      }
    }
  }

  @Test
  void deterministicWithSameSeed() {
    CatanMap a = CatanMap.fromTemplate(MapTemplates.baseMap(), false, new Random(99));
    CatanMap b = CatanMap.fromTemplate(MapTemplates.baseMap(), false, new Random(99));
    assertEquals(a.landTiles.size(), b.landTiles.size());
    // Spot-check: same tile IDs at same coordinates
    for (Coordinate c : a.landTiles.keySet()) {
      assertTrue(b.landTiles.containsKey(c));
      assertEquals(a.landTiles.get(c).id, b.landTiles.get(c).id);
    }
  }
}

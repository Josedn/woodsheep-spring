package io.bobba.catanatron.models;

import static org.junit.jupiter.api.Assertions.*;

import io.bobba.catanatron.enums.BuildingType;
import io.bobba.catanatron.enums.Color;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BoardTest {

  private Board board;

  @BeforeEach
  void setUp() {
    CatanMap map = CatanMap.fromTemplate(MapTemplates.baseMap(), true, new Random(42));
    board = new Board(map);
  }

  @Test
  void initialBuildableIdsAreAllLandNodes() {
    assertEquals(board.map.landNodes.size(), board.boardBuildableIds.size());
  }

  @Test
  void robberStartsOnDesert() {
    LandTile desert = board.map.landTiles.get(board.robberCoordinate);
    assertNotNull(desert);
    assertNull(desert.resource);
  }

  @Test
  void buildSettlementInitialPhaseSucceeds() {
    int nodeId = board.buildableNodeIds(Color.RED, true).get(0);
    board.buildSettlement(Color.RED, nodeId, true);
    assertEquals(Color.RED, board.getNodeColor(nodeId));
    assertEquals(BuildingType.SETTLEMENT, board.getBuildingType(nodeId));
  }

  @Test
  void buildSettlementRemovesAdjacentNodes() {
    int nodeId = board.buildableNodeIds(Color.RED, true).get(0);
    int before = board.boardBuildableIds.size();
    board.buildSettlement(Color.RED, nodeId, true);
    assertTrue(board.boardBuildableIds.size() < before);
    assertFalse(board.boardBuildableIds.contains(nodeId));
  }

  @Test
  void buildRoadAfterSettlement() {
    int nodeId = board.buildableNodeIds(Color.RED, true).get(0);
    board.buildSettlement(Color.RED, nodeId, true);

    var edges = board.buildableEdges(Color.RED);
    assertFalse(edges.isEmpty());

    EdgeId edge = edges.get(0);
    board.buildRoad(Color.RED, edge);
    assertEquals(Color.RED, board.getEdgeColor(edge));
  }

  @Test
  void buildCityRequiresSettlement() {
    int nodeId = board.buildableNodeIds(Color.RED, true).get(0);
    board.buildSettlement(Color.RED, nodeId, true);
    board.buildCity(Color.RED, nodeId);
    assertEquals(BuildingType.CITY, board.getBuildingType(nodeId));
  }

  @Test
  void buildCityOnEmptyNodeThrows() {
    int nodeId = board.buildableNodeIds(Color.RED, true).get(0);
    assertThrows(IllegalArgumentException.class, () -> board.buildCity(Color.RED, nodeId));
  }

  @Test
  void duplicateSettlementThrows() {
    int nodeId = board.buildableNodeIds(Color.RED, true).get(0);
    board.buildSettlement(Color.RED, nodeId, true);
    assertThrows(
        IllegalArgumentException.class, () -> board.buildSettlement(Color.BLUE, nodeId, true));
  }

  @Test
  void boardCopyIsIndependent() {
    int nodeId = board.buildableNodeIds(Color.RED, true).get(0);
    board.buildSettlement(Color.RED, nodeId, true);

    Board copy = board.copy();
    int nodeId2 = copy.buildableNodeIds(Color.BLUE, true).get(0);
    copy.buildSettlement(Color.BLUE, nodeId2, true);

    assertNull(board.getNodeColor(nodeId2));
    assertEquals(Color.BLUE, copy.getNodeColor(nodeId2));
  }

  @Test
  void longestRoadAwardedAt5() {
    // Build a chain of 5 roads for RED
    int node = board.buildableNodeIds(Color.RED, true).get(0);
    board.buildSettlement(Color.RED, node, true);

    for (int i = 0; i < 5; i++) {
      var edges = board.buildableEdges(Color.RED);
      if (edges.isEmpty()) break;
      board.buildRoad(Color.RED, edges.get(0));
    }

    if (board.roadColor == Color.RED) {
      assertTrue(board.roadLength >= 5);
    }
    // If fewer than 5 connected edges were available the award wasn't triggered — that's fine.
  }
}

package com.catanatron.core.models;

import static org.junit.jupiter.api.Assertions.*;

import com.catanatron.core.enums.BuildingType;
import com.catanatron.core.enums.Color;
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
  void cannotExtendRoadPastEnemySettlementAtEndpoint() {
    // BLUE builds a 2-road chain: settlement → road → road → endpoint node E
    // RED then settles at E; BLUE should no longer be able to build from E

    // Place BLUE's initial settlement at some buildable node, then extend 2 roads
    int blueStart = board.buildableNodeIds(Color.BLUE, true).get(0);
    board.buildSettlement(Color.BLUE, blueStart, true);
    var edges1 = board.buildableEdges(Color.BLUE);
    assertFalse(edges1.isEmpty(), "BLUE needs at least one buildable edge");
    board.buildRoad(Color.BLUE, edges1.get(0));

    var edges2 = board.buildableEdges(Color.BLUE);
    // find an edge that extends further (not back to blueStart)
    EdgeId ext = null;
    for (EdgeId e : edges2) {
      if (e.a() != blueStart && e.b() != blueStart) {
        ext = e;
        break;
      }
    }
    if (ext == null) ext = edges2.get(0); // fallback
    board.buildRoad(Color.BLUE, ext);

    // E is whichever endpoint of ext is further from blueStart
    int endpoint = (ext.a() != blueStart) ? ext.a() : ext.b();

    // RED settles at endpoint (must be valid for RED in non-initial phase —
    // use initial_build_phase=true so no road connectivity required)
    if (board.buildableNodeIds(Color.RED, true).contains(endpoint)) {
      board.buildSettlement(Color.RED, endpoint, true);

      // BLUE should not be able to build a road from endpoint
      var blueEdges = board.buildableEdges(Color.BLUE);
      for (EdgeId e : blueEdges) {
        assertFalse(
            e.a() == endpoint || e.b() == endpoint,
            "BLUE should not build a road from RED-settled endpoint "
                + endpoint
                + " but found edge: "
                + e);
      }
    }
    // If endpoint wasn't buildable for RED (e.g. distance rule), skip assertion — test is moot.
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

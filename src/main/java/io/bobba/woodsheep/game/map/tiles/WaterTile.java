package io.bobba.woodsheep.game.map.tiles;

import io.bobba.woodsheep.game.map.Edge;
import io.bobba.woodsheep.game.map.EdgeRef;
import io.bobba.woodsheep.game.map.NodeRef;
import java.util.Map;

public record WaterTile(Map<NodeRef, Integer> nodes, Map<EdgeRef, Edge> edges) implements Tile {}

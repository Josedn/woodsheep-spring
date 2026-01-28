package io.bobba.woodsheep.game.map.tiles;

import io.bobba.woodsheep.game.map.geometry.Edge;
import io.bobba.woodsheep.game.map.geometry.EdgeRef;
import io.bobba.woodsheep.game.map.geometry.NodeRef;
import java.util.Map;

public record WaterTile(Map<NodeRef, Integer> nodes, Map<EdgeRef, Edge> edges) implements Tile {}

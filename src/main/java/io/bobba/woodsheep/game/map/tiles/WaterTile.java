package io.bobba.woodsheep.game.map.tiles;

import io.bobba.woodsheep.game.model.Edge;
import io.bobba.woodsheep.game.model.EdgeRef;
import io.bobba.woodsheep.game.model.NodeRef;
import java.util.Map;

public record WaterTile(Map<NodeRef, Integer> nodes, Map<EdgeRef, Edge> edges) implements Tile {}

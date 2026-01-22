package io.bobba.woodsheep.game.map.tiles;

import io.bobba.woodsheep.game.map.Edge;
import io.bobba.woodsheep.game.map.EdgeRef;
import io.bobba.woodsheep.game.map.NodeRef;
import io.bobba.woodsheep.game.model.Resource;
import java.util.Map;

public record LandTile(
    int id, Resource resource, int number, Map<NodeRef, Integer> nodes, Map<EdgeRef, Edge> edges)
    implements Tile {}

package io.bobba.catanatron.models;

import io.bobba.catanatron.enums.EdgeRef;
import io.bobba.catanatron.enums.NodeRef;
import java.util.Map;

/** A water tile — no resource, no number, just topology. */
public record Water(Map<NodeRef, Integer> nodes, Map<EdgeRef, EdgeId> edges) {}

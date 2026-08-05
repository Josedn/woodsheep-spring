package com.catanatron.core.models;

import com.catanatron.core.enums.EdgeRef;
import com.catanatron.core.enums.NodeRef;
import java.util.Map;

/** A water tile — no resource, no number, just topology. */
public record Water(Map<NodeRef, Integer> nodes, Map<EdgeRef, EdgeId> edges) {}

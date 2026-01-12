package io.bobba.woodsheep.core.communication.incoming;

import tools.jackson.databind.JsonNode;

public record GenericIncomingMessage(String requestType, JsonNode payload) {}

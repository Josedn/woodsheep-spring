package io.bobba.woodsheep.core.communication.protocol;

import tools.jackson.databind.JsonNode;

public record GenericIncomingMessage(String requestType, JsonNode payload) {}

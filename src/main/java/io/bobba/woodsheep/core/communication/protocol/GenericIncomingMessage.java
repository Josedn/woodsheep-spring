package io.bobba.woodsheep.core.communication.protocol;

import com.fasterxml.jackson.databind.JsonNode;

public record GenericIncomingMessage(String requestType, JsonNode payload) {}

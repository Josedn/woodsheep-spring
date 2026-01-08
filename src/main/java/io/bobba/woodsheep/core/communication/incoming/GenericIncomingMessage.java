package io.bobba.woodsheep.core.communication.incoming;

public record GenericIncomingMessage(String requestType, Object payload) {}

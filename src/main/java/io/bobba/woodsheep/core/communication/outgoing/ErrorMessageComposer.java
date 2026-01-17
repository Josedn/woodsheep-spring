package io.bobba.woodsheep.core.communication.outgoing;

import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;

public class ErrorMessageComposer extends OutgoingMessage {

  private final String code;
  private final String message;

  public ErrorMessageComposer(String code, String message) {
    super("error");
    this.code = code;
    this.message = message;
  }

  @Override
  public Object getPayload() {
    return new Payload(code, message);
  }

  private record Payload(String code, String message) {}
}

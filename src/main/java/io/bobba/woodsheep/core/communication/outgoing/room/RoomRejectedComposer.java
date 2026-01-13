package io.bobba.woodsheep.core.communication.outgoing.room;

import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;

public class RoomRejectedComposer extends OutgoingMessage {
  String reason;

  public RoomRejectedComposer(String reason) {
    super("roomRejected");
    this.reason = reason;
  }

  @Override
  public Object getPayload() {
    return new Payload(this.reason);
  }

  private record Payload(String reason) {}
}

package io.bobba.woodsheep.core.communication.outgoing.room;

import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;

public class PrepareRoomComposer extends OutgoingMessage {

  private final String roomId;

  public PrepareRoomComposer(String roomId) {
    super("prepareRoom");
    this.roomId = roomId;
  }

  @Override
  public Object getPayload() {
    return new Payload(this.roomId);
  }

  private record Payload(String roomId) {}
}

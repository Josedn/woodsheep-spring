package io.bobba.woodsheep.core.communication.outgoing.room;

import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;

public class RemoveUserFromRoomComposer extends OutgoingMessage {

  private final int virtualId;

  public RemoveUserFromRoomComposer(int virtualId) {
    super("removeUserFromRoom");
    this.virtualId = virtualId;
  }

  @Override
  public Object getPayload() {
    return new Payload(this.virtualId);
  }

  private record Payload(int virtualId) {}
}

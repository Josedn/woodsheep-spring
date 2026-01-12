package io.bobba.woodsheep.core.communication.outgoing.room;

import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;

public class ChatMessageComposer extends OutgoingMessage {
  int virtualId;
  String message;

  public ChatMessageComposer(int virtualId, String message) {
    super("chatMessage");
    this.virtualId = virtualId;
    this.message = message;
  }

  @Override
  public Object getPayload() {
    return new Payload(this.virtualId, this.message);
  }

  private record Payload(int virtualId, String message) {}
}

package io.bobba.woodsheep.core.communication;

import io.bobba.woodsheep.core.gameclients.GameClient;

public interface IncomingEvent {
  void handle(GameClient client, Object payload);

  String getOpCode();
}

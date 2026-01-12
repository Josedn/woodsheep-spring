package io.bobba.woodsheep.core.communication;

import io.bobba.woodsheep.core.gameclients.GameClient;

public interface IncomingEventHandler<T> {
  void handle(GameClient session, T payload);

  Class<T> payloadType();
}

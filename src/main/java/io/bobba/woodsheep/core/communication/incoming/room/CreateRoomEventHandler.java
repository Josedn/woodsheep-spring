package io.bobba.woodsheep.core.communication.incoming.room;

import io.bobba.woodsheep.core.communication.protocol.EmptyObjectPayload;
import io.bobba.woodsheep.core.communication.protocol.IncomingEventHandler;
import io.bobba.woodsheep.core.communication.protocol.OpCode;
import io.bobba.woodsheep.core.gameclients.GameClient;
import io.bobba.woodsheep.core.rooms.RoomManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@OpCode("createRoom")
@RequiredArgsConstructor
public class CreateRoomEventHandler implements IncomingEventHandler<EmptyObjectPayload> {
  private final RoomManager roomManager;

  @Override
  public void handle(GameClient session, EmptyObjectPayload payload) {
    this.roomManager.createRoom(session);
  }

  @Override
  public Class<EmptyObjectPayload> payloadType() {
    return EmptyObjectPayload.class;
  }
}

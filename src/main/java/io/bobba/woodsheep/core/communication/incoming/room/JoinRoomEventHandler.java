package io.bobba.woodsheep.core.communication.incoming.room;

import io.bobba.woodsheep.core.communication.incoming.room.JoinRoomEventHandler.JoinRoomEventMessage;
import io.bobba.woodsheep.core.communication.protocol.IncomingEventHandler;
import io.bobba.woodsheep.core.communication.protocol.OpCode;
import io.bobba.woodsheep.core.gameclients.GameClient;
import io.bobba.woodsheep.core.rooms.RoomManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@OpCode("joinRoom")
@RequiredArgsConstructor
public class JoinRoomEventHandler implements IncomingEventHandler<JoinRoomEventMessage> {
  private final RoomManager roomManager;

  public record JoinRoomEventMessage(String roomId) {}

  @Override
  public void handle(GameClient session, JoinRoomEventMessage payload) {
    this.roomManager.prepareRoomForUser(session, payload.roomId());
  }

  @Override
  public Class<JoinRoomEventMessage> payloadType() {
    return JoinRoomEventMessage.class;
  }
}

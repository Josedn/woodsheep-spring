package io.bobba.woodsheep.core.communication.incoming.room;

import io.bobba.woodsheep.core.communication.protocol.EmptyObjectPayload;
import io.bobba.woodsheep.core.communication.protocol.IncomingEventHandler;
import io.bobba.woodsheep.core.communication.protocol.OpCode;
import io.bobba.woodsheep.core.gameclients.GameClient;
import io.bobba.woodsheep.core.rooms.RoomManager;
import io.bobba.woodsheep.core.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@OpCode(io.bobba.woodsheep.core.communication.protocol.Op.ROOM_LIST)
@RequiredArgsConstructor
public class GetRoomListEventHandler implements IncomingEventHandler<EmptyObjectPayload> {
  private final RoomManager roomManager;

  @Override
  public void handle(GameClient session, EmptyObjectPayload payload) {
    final User user = session.getUser();
    if (user != null) {
      this.roomManager.sendRoomList(user);
      user.leaveRoom();
    }
  }

  @Override
  public Class<EmptyObjectPayload> payloadType() {
    return EmptyObjectPayload.class;
  }
}

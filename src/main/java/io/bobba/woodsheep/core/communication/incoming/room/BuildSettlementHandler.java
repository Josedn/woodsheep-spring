package io.bobba.woodsheep.core.communication.incoming.room;

import io.bobba.woodsheep.core.communication.protocol.IncomingEventHandler;
import io.bobba.woodsheep.core.communication.protocol.OpCode;
import io.bobba.woodsheep.core.gameclients.GameClient;
import io.bobba.woodsheep.core.rooms.Room;
import io.bobba.woodsheep.core.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@OpCode("buildSettlement")
@RequiredArgsConstructor
public class BuildSettlementHandler
    implements IncomingEventHandler<BuildSettlementHandler.BuildSettlementMessage> {
  public record BuildSettlementMessage(int nodeId) {}

  @Override
  public void handle(GameClient session, BuildSettlementMessage payload) {
    final User user = session.getUser();
    if (user != null) {
      final Room room = user.getCurrentRoom();
      if (room != null) {
        room.handleBuildSettlement(user, payload.nodeId());
      }
    }
  }

  @Override
  public Class<BuildSettlementMessage> payloadType() {
    return BuildSettlementMessage.class;
  }
}

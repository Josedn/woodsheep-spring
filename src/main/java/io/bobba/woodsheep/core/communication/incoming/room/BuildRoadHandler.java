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
@OpCode("buildRoad")
@RequiredArgsConstructor
public class BuildRoadHandler implements IncomingEventHandler<BuildRoadHandler.BuildRoadMessage> {
  public record BuildRoadMessage(int nodeA, int nodeB) {}

  @Override
  public void handle(GameClient session, BuildRoadMessage payload) {
    final User user = session.getUser();
    if (user != null) {
      final Room room = user.getCurrentRoom();
      if (room != null) {
        room.handleBuildRoad(user, payload.nodeA(), payload.nodeB());
      }
    }
  }

  @Override
  public Class<BuildRoadMessage> payloadType() {
    return BuildRoadMessage.class;
  }
}

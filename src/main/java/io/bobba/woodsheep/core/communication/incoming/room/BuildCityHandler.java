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
@OpCode("buildCity")
@RequiredArgsConstructor
public class BuildCityHandler implements IncomingEventHandler<BuildCityHandler.BuildCityMessage> {
  public record BuildCityMessage(int nodeId) {}

  @Override
  public void handle(GameClient session, BuildCityMessage payload) {
    final User user = session.getUser();
    if (user != null) {
      final Room room = user.getCurrentRoom();
      if (room != null) {
        room.handleBuildCity(user, payload.nodeId());
      }
    }
  }

  @Override
  public Class<BuildCityMessage> payloadType() {
    return BuildCityMessage.class;
  }
}

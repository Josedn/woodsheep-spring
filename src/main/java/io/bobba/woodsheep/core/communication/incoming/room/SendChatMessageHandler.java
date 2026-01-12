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
@OpCode("chatMessage")
@RequiredArgsConstructor
public class SendChatMessageHandler
    implements IncomingEventHandler<SendChatMessageHandler.SendChatMessageMessage> {
  public record SendChatMessageMessage(String message) {}

  @Override
  public void handle(GameClient session, SendChatMessageMessage payload) {
    final User user = session.getUser();
    if (user != null) {
      final Room room = user.getCurrentRoom();
      room.handleChatMessage(user, payload.message());
    }
  }

  @Override
  public Class<SendChatMessageMessage> payloadType() {
    return SendChatMessageMessage.class;
  }
}

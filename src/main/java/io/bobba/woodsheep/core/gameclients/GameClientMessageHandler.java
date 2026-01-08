package io.bobba.woodsheep.core.gameclients;

import io.bobba.woodsheep.core.communication.IncomingEvent;
import io.bobba.woodsheep.core.communication.incoming.GenericIncomingMessage;
import io.bobba.woodsheep.core.communication.incoming.handshake.LoginEvent;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class GameClientMessageHandler {
  private final Map<String, IncomingEvent> requestHandlers;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public GameClientMessageHandler() {
    this.requestHandlers = new HashMap<>();
    this.registerHandler(new LoginEvent());
  }

  public void handleMessage(GameClient session, String message) {
    try {
      final GenericIncomingMessage data =
          OBJECT_MAPPER.readValue(message, GenericIncomingMessage.class);
      final String opCode = data.requestType();
      final IncomingEvent handler = this.requestHandlers.getOrDefault(opCode, null);
      if (handler == null) {
        log.warn("Invalid OpCode {}", opCode);
      } else {
        log.debug("Handled {} with {}", opCode, handler.getClass().getSimpleName());
        handler.handle(session, data.payload());
      }
    } catch (JacksonException e) {
      log.warn("Invalid message object");
    }
  }

  private void registerHandler(IncomingEvent incomingEvent) {
    this.requestHandlers.put(incomingEvent.getOpCode(), incomingEvent);
  }
}

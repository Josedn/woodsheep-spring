package io.bobba.woodsheep.core.gameclients;

import io.bobba.woodsheep.core.communication.protocol.GenericIncomingMessage;
import io.bobba.woodsheep.core.communication.protocol.IncomingEventHandler;
import io.bobba.woodsheep.core.communication.protocol.OpCode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class GameClientMessageHandler {

  private final Map<String, IncomingEventHandler<?>> requestHandlers = new HashMap<>();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public GameClientMessageHandler(List<IncomingEventHandler<?>> discoveredHandlers) {
    for (IncomingEventHandler<?> handler : discoveredHandlers) {
      OpCode opCode = handler.getClass().getAnnotation(OpCode.class);

      if (opCode == null) {
        throw new IllegalStateException(
            "Handler " + handler.getClass().getName() + " is missing @OpCode");
      }

      if (requestHandlers.containsKey(opCode.value())) {
        throw new IllegalStateException("Duplicate OpCode: " + opCode.value());
      }

      requestHandlers.put(opCode.value(), handler);
    }
  }

  public void handleMessage(GameClient session, String message) {
    try {
      GenericIncomingMessage data = OBJECT_MAPPER.readValue(message, GenericIncomingMessage.class);

      IncomingEventHandler<?> handler = requestHandlers.get(data.requestType());

      if (handler == null) {
        log.warn("Invalid OpCode {}", data.requestType());
        return;
      }
      log.debug("Handled {} with {}", data.requestType(), handler.getClass().getSimpleName());

      handleTyped(session, data, handler);

    } catch (Exception e) {
      log.warn("Invalid message {}", message, e);
      session.stop();
    }
  }

  private <T> void handleTyped(
      GameClient session, GenericIncomingMessage data, IncomingEventHandler<T> handler)
      throws JacksonException {

    T payload = OBJECT_MAPPER.treeToValue(data.payload(), handler.payloadType());

    handler.handle(session, payload);
  }
}

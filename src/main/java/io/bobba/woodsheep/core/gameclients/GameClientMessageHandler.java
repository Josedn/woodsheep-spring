package io.bobba.woodsheep.core.gameclients;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.bobba.woodsheep.core.communication.outgoing.ErrorMessageComposer;
import io.bobba.woodsheep.core.communication.protocol.GenericIncomingMessage;
import io.bobba.woodsheep.core.communication.protocol.IncomingEventHandler;
import io.bobba.woodsheep.core.communication.protocol.Op;
import io.bobba.woodsheep.core.communication.protocol.OpCode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GameClientMessageHandler {

  private final Map<Op, IncomingEventHandler<?>> requestHandlers = new HashMap<>();
  private final ObjectMapper OBJECT_MAPPER;

  public GameClientMessageHandler(
      ObjectMapper objectMapper, List<IncomingEventHandler<?>> discoveredHandlers) {
    this.OBJECT_MAPPER = objectMapper;
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

      // Map wire string to enum
      Op op = null;
      for (Op candidate : Op.values()) {
        if (candidate.wire().equals(data.requestType())) {
          op = candidate;
          break;
        }
      }

      if (op == null) {
        log.warn("Invalid OpCode {}", data.requestType());
        session.sendMessage(
            new ErrorMessageComposer(
                "unknown_opcode", "Unknown requestType: " + data.requestType()));
        return;
      }

      IncomingEventHandler<?> handler = requestHandlers.get(op);

      if (handler == null) {
        log.warn("Invalid OpCode {}", data.requestType());
        return;
      }
      log.debug("Handled {} with {}", data.requestType(), handler.getClass().getSimpleName());

      handleTyped(session, data, handler);

    } catch (Exception e) {
      log.warn("Invalid message {}", message, e);
      session.sendMessage(new ErrorMessageComposer("bad_request", "Invalid message format"));
    }
  }

  private <T> void handleTyped(
      GameClient session, GenericIncomingMessage data, IncomingEventHandler<T> handler)
      throws JacksonException {

    T payload = OBJECT_MAPPER.treeToValue(data.payload(), handler.payloadType());

    handler.handle(session, payload);
  }
}

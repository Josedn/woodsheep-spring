package io.bobba.woodsheep.core.communication.outgoing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public abstract class OutgoingMessage {
  @JsonProperty("requestType")
  @Getter
  private final String requestType;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public String stringify() throws JacksonException {
    return OBJECT_MAPPER.writeValueAsString(this);
  }

  @JsonProperty("payload")
  public abstract Object getPayload();
}

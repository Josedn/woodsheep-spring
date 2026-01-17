package io.bobba.woodsheep.core.communication.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

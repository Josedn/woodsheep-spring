package io.bobba.woodsheep.core.communication.protocol;

public enum Op {
  LOGIN("login"),
  CREATE_ROOM("createRoom"),
  JOIN_ROOM("joinRoom"),
  ROOM_LIST("roomList"),
  CHAT_MESSAGE("chatMessage");

  private final String wire;

  Op(String wire) {
    this.wire = wire;
  }

  public String wire() {
    return wire;
  }
}

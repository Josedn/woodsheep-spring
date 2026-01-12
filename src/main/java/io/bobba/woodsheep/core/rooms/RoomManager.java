package io.bobba.woodsheep.core.rooms;

import java.util.HashMap;
import java.util.Map;

public class RoomManager {
  private Map<String, Room> rooms;

  public RoomManager() {
    this.rooms = new HashMap<>();
  }
}

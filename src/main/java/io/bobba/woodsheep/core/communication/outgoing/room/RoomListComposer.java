package io.bobba.woodsheep.core.communication.outgoing.room;

import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;
import io.bobba.woodsheep.core.rooms.Room;
import java.util.List;

public class RoomListComposer extends OutgoingMessage {

  private final List<RoomRecord> roomList;

  public RoomListComposer(List<Room> roomList) {
    super("roomList");
    this.roomList =
        roomList.stream()
            .map(room -> new RoomRecord(room.getId(), "Room", 4, room.getUnSyncUsers().size()))
            .toList();
  }

  @Override
  public Object getPayload() {
    return this.roomList;
  }

  private record RoomRecord(String roomId, String name, int maxPlayers, int currentPlayers) {}
}

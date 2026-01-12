package io.bobba.woodsheep.core.communication.outgoing.room;

import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;
import io.bobba.woodsheep.core.rooms.RoomUser;
import java.util.List;

public class AddUserToRoomComposer extends OutgoingMessage {

  List<UserRecord> roomUsers;

  public AddUserToRoomComposer(List<RoomUser> roomUsers) {
    super("addUserToRoom");
    this.roomUsers =
        roomUsers.stream()
            .map(
                roomUser ->
                    new UserRecord(roomUser.getVirtualId(), roomUser.getUser().getUsername()))
            .toList();
  }

  public AddUserToRoomComposer(RoomUser roomUser) {
    this(List.of(roomUser));
  }

  @Override
  public Object getPayload() {
    return this.roomUsers;
  }

  private record UserRecord(int virtualId, String username) {}
}

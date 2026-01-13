package io.bobba.woodsheep.core.users;

import io.bobba.woodsheep.core.gameclients.GameClient;
import io.bobba.woodsheep.core.rooms.Room;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class User {
  private final String id;
  private String username;
  private GameClient session;
  private Room currentRoom;

  public void onStop() {
    this.leaveRoom();
  }

  public void leaveRoom() {
    if (currentRoom != null) {
      currentRoom.removeUserFromRoom(this);
    }
  }

  public void onRoomLeave() {
    this.currentRoom = null;
  }
}

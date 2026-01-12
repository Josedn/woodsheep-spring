package io.bobba.woodsheep.core.rooms;

import io.bobba.woodsheep.core.users.User;
import io.bobba.woodsheep.misc.WoodsheepUUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomManager {
  private final Map<String, Room> rooms = new ConcurrentHashMap<>();

  public void createRoom(User owner) {
    Room room = new Room(WoodsheepUUID.generateUUID());
    this.rooms.put(room.getId(), room);
    prepareRoomForUser(owner, room.getId());
  }

  public void prepareRoomForUser(User user, String roomId) {
    Room currentRoom = user.getCurrentRoom();
    if (currentRoom != null) {
      currentRoom.removeUserFromRoom(user);
    }
    Room newRoom = this.rooms.get(roomId);
    if (newRoom != null) {
      newRoom.addUserToRoom(user);
    }
  }
}

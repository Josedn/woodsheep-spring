package io.bobba.woodsheep.core.rooms;

import io.bobba.woodsheep.core.communication.outgoing.room.RoomListComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.RoomRejectedComposer;
import io.bobba.woodsheep.core.users.User;
import io.bobba.woodsheep.misc.WoodsheepUUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomManager {
  private final Map<String, Room> rooms = new ConcurrentHashMap<>();

  public void createRoom(User user) {
    Room room = new Room(WoodsheepUUID.generateUUID());
    this.rooms.put(room.getId(), room);
    prepareRoomForUser(user, room.getId());
  }

  public void prepareRoomForUser(User user, String roomId) {
    Room currentRoom = user.getCurrentRoom();
    if (currentRoom != null) {
      if (currentRoom.getId().equals(roomId)) {
        currentRoom.serializeRoomInfo(user);
        return;
      }
      currentRoom.removeUserFromRoom(user);
    }
    Room newRoom = this.rooms.get(roomId);
    if (newRoom != null) {
      newRoom.addUserToRoom(user);
    } else {
      if (user.getSession() != null) {
        user.getSession().sendMessage(new RoomRejectedComposer("invalid"));
      }
    }
  }

  public void sendRoomList(User user) {
    List<Room> roomsCopy = this.getUnSyncRooms();
    if (user.getSession() != null) {
      user.getSession().sendMessage(new RoomListComposer(roomsCopy));
    }
  }

  private List<Room> getUnSyncRooms() {
    return new ArrayList<>(rooms.values());
  }
}

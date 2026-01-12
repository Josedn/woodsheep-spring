package io.bobba.woodsheep.core.rooms;

import io.bobba.woodsheep.core.communication.outgoing.room.RoomListComposer;
import io.bobba.woodsheep.core.gameclients.GameClient;
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

  public void createRoom(GameClient session) {
    if (session.getUser() != null) {
      Room room = new Room(WoodsheepUUID.generateUUID());
      this.rooms.put(room.getId(), room);
      prepareRoomForUser(session, room.getId());
    }
  }

  public void prepareRoomForUser(GameClient session, String roomId) {
    User user = session.getUser();
    if (user != null) {
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

  public void sendRoomList(GameClient session) {
    List<Room> roomsCopy = this.getUnSyncRooms();
    session.sendMessage(new RoomListComposer(roomsCopy));
  }

  private List<Room> getUnSyncRooms() {
    List<Room> roomsCopy;
    synchronized (rooms) {
      roomsCopy = new ArrayList<>(rooms.values());
    }
    return roomsCopy;
  }
}

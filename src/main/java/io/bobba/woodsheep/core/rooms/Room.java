package io.bobba.woodsheep.core.rooms;

import io.bobba.woodsheep.core.communication.outgoing.room.AddUserToRoomComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.RemoveUserFromRoomComposer;
import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;
import io.bobba.woodsheep.core.users.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class Room {

  private final String id;
  private final Map<Integer, RoomUser> users = new ConcurrentHashMap<>();
  private int userCounter = 0;

  public void removeUserFromRoom(User user) {
    RoomUser roomUser = this.getRoomUserByUser(user);
    if (roomUser != null) {
      users.remove(roomUser.getVirtualId());
      user.onRoomLeave();
      sendMessage(new RemoveUserFromRoomComposer(roomUser.getVirtualId()));
    }
  }

  public void addUserToRoom(User user) {
    if (user.getSession() != null) {
      RoomUser roomUser = new RoomUser(this.userCounter++, user);
      user.setCurrentRoom(this);
      this.sendMessage(new AddUserToRoomComposer(roomUser));
      this.users.put(roomUser.getVirtualId(), roomUser);
      List<RoomUser> usersCopy = getUnSyncUsers();
      user.getSession().sendMessage(new AddUserToRoomComposer(usersCopy));
    }
  }

  public void sendMessage(OutgoingMessage outgoingMessage) {
    List<RoomUser> usersCopy = getUnSyncUsers();
    for (RoomUser user : usersCopy) {
      user.getUser().getSession().sendMessage(outgoingMessage);
    }
  }

  private RoomUser getRoomUserByUser(User user) {
    List<RoomUser> usersCopy = getUnSyncUsers();
    for (RoomUser roomUser : usersCopy) {
      if (roomUser.getUser() == user) {
        return roomUser;
      }
    }
    return null;
  }

  private List<RoomUser> getUnSyncUsers() {
    List<RoomUser> usersCopy;
    synchronized (users) {
      usersCopy = new ArrayList<>(users.values());
    }
    return usersCopy;
  }
}

package io.bobba.woodsheep.core.rooms;

import io.bobba.woodsheep.core.communication.outgoing.room.AddUserToRoomComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.ChatMessageComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.RemoveUserFromRoomComposer;
import io.bobba.woodsheep.core.communication.outgoing.room.RoomInfoComposer;
import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;
import io.bobba.woodsheep.core.users.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Data
public class Room {

  private final String id;
  private final Map<Integer, RoomUser> users = new ConcurrentHashMap<>();
  private java.util.concurrent.atomic.AtomicInteger userCounter =
      new java.util.concurrent.atomic.AtomicInteger(0);

  public void removeUserFromRoom(User user) {
    RoomUser roomUser = this.getRoomUserByUser(user);
    if (roomUser != null) {
      users.remove(roomUser.getVirtualId());
      user.onRoomLeave();
      sendMessage(new RemoveUserFromRoomComposer(roomUser.getVirtualId()));
      log.debug("User removed from room: {}", user.getUsername());
    }
  }

  public void addUserToRoom(User user) {
    if (user.getSession() != null) {
      RoomUser roomUser = new RoomUser(this.userCounter.getAndIncrement(), user);
      user.setCurrentRoom(this);
      this.sendMessage(new AddUserToRoomComposer(roomUser));
      this.users.put(roomUser.getVirtualId(), roomUser);
      this.serializeRoomInfo(user);
      log.debug("User added to room: {}", user.getUsername());
    }
  }

  public void serializeRoomInfo(User user) {
    List<RoomUser> usersCopy = getUnSyncUsers();
    user.getSession().sendMessage(new RoomInfoComposer(this.id, "base", false, true, 4, 30, 7, 10));
    user.getSession().sendMessage(new AddUserToRoomComposer(usersCopy));
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

  public void handleChatMessage(User user, String message) {
    RoomUser roomUser = this.getRoomUserByUser(user);
    if (roomUser != null) {
      this.sendMessage(new ChatMessageComposer(roomUser.getVirtualId(), message));
    }
  }

  public List<RoomUser> getUnSyncUsers() {
    List<RoomUser> usersCopy;
    synchronized (users) {
      usersCopy = new ArrayList<>(users.values());
    }
    return usersCopy;
  }
}

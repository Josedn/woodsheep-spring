package io.bobba.woodsheep.core.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.bobba.woodsheep.core.rooms.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserTest {

  private User user;

  @BeforeEach
  void setUp() {
    user = new User("user-1");
    user.setUsername("Alice");
  }

  @Test
  void leaveRoom_withRoom_removesUserFromRoom() {
    Room room = mock(Room.class);
    user.setCurrentRoom(room);

    user.leaveRoom();

    verify(room).removeUserFromRoom(user);
  }

  @Test
  void leaveRoom_noRoom_doesNothing() {
    user.leaveRoom(); // should not throw
  }

  @Test
  void onStop_delegatesToLeaveRoom() {
    Room room = mock(Room.class);
    user.setCurrentRoom(room);

    user.onStop();

    verify(room).removeUserFromRoom(user);
  }

  @Test
  void onRoomLeave_clearsCurrentRoom() {
    Room room = mock(Room.class);
    user.setCurrentRoom(room);

    user.onRoomLeave();

    assertThat(user.getCurrentRoom()).isNull();
  }
}

package io.bobba.woodsheep.core.rooms;

import io.bobba.woodsheep.core.users.User;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class RoomUser {
  private final int virtualId;
  private final User user;
}

package io.bobba.woodsheep.core.rooms;

import io.bobba.catanatron.enums.Color;
import io.bobba.catanatron.state.Player;
import io.bobba.woodsheep.core.users.User;
import lombok.Data;

@Data
public class RoomUser implements Player {
  private final int virtualId;
  private final User user;
  private final Color color;

  protected RoomUser(int virtualId, User user, Color color) {
    this.virtualId = virtualId;
    this.user = user;
    this.color = color;
  }

  @Override
  public Color getColor() {
    return this.color;
  }

  @Override
  public boolean isBot() {
    return false;
  }
}

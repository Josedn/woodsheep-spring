package io.bobba.woodsheep.core.rooms;

import io.bobba.woodsheep.core.users.User;
import io.bobba.woodsheep.game.engine.Game;
import io.bobba.woodsheep.game.engine.Player;
import io.bobba.woodsheep.game.model.PlayerColor;
import io.bobba.woodsheep.game.model.actions.Action;
import java.util.List;
import lombok.Data;

@Data
public class RoomUser extends Player {
  private final int virtualId;
  private final User user;

  protected RoomUser(int virtualId, User user, PlayerColor color) {
    super(color, false);
    this.virtualId = virtualId;
    this.user = user;
  }

  @Override
  public Action<?> decide(Game game, List<Action<?>> playable) {
    return null;
  }
}

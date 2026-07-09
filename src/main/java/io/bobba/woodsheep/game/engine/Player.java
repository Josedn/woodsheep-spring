package io.bobba.woodsheep.game.engine;

import io.bobba.woodsheep.game.model.PlayerColor;
import io.bobba.woodsheep.game.model.actions.Action;
import java.util.List;
import lombok.Getter;

@Getter
public abstract class Player {
  public final PlayerColor color;
  public final boolean isBot;

  protected Player(PlayerColor color, boolean isBot) {
    this.color = color;
    this.isBot = isBot;
  }

  public abstract Action<?> decide(Game game, List<Action<?>> playable);

  public void resetState() {}

  @Override
  public String toString() {
    return getClass().getSimpleName() + ":" + color;
  }
}

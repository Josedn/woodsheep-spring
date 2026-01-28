package io.bobba.woodsheep.game.model.actions;

import io.bobba.woodsheep.game.engine.State;
import io.bobba.woodsheep.game.model.PlayerColor;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ActionGenerator {

  public static List<Action<?>> generatePlayable(State state) {
    List<Action<?>> actions = new ArrayList<>();
    PlayerColor color = state.currentColor();
    switch (state.currentPrompt) {
    }

    return actions;
  }
}

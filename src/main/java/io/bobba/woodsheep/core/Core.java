package io.bobba.woodsheep.core;

import io.bobba.woodsheep.core.gameclients.GameClientRegistry;
import io.bobba.woodsheep.core.rooms.RoomManager;
import io.bobba.woodsheep.core.users.UserManager;
import org.springframework.stereotype.Component;

@Component
public class Core {
  private RoomManager roomManager;
  private UserManager userManager;
  private GameClientRegistry gameClientRegistry;
}

package io.bobba.woodsheep.core.users;

import io.bobba.woodsheep.core.gameclients.GameClient;
import io.bobba.woodsheep.misc.UsernameGenerator;
import io.bobba.woodsheep.misc.WoodsheepUUID;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserManager {
  private final Map<String, User> users;

  public UserManager() {
    this.users = new HashMap<>();
  }

  public void tryLogin(GameClient session, String sso) {
    log.debug("Logging in with sso {}", sso);
    if (session.getUser() != null) {
      log.debug("Client already logged in!");
      session.stop();
      return;
    }
    User user = this.users.get(sso);
    if (user == null) {
      user = new User(WoodsheepUUID.generateUUID(), UsernameGenerator.generateUsername());
      this.users.put(user.id(), user);
    }
    session.setUser(user);
  }
}

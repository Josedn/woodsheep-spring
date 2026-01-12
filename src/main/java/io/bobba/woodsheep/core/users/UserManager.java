package io.bobba.woodsheep.core.users;

import io.bobba.woodsheep.core.gameclients.GameClient;
import io.bobba.woodsheep.misc.UsernameGenerator;
import io.bobba.woodsheep.misc.WoodsheepUUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserManager {
  private final Map<String, User> users = new ConcurrentHashMap<>();

  public void tryLogin(GameClient session, String sso) {
    log.debug("Logging in with sso {}", sso);
    if (session.getUser() != null) {
      log.debug("Client already logged in!");
      session.stop();
      return;
    }
    User user = this.users.get(sso);
    if (user == null) {
      user = new User(WoodsheepUUID.generateUUID());
      user.setUsername(UsernameGenerator.generateUsername());
      this.users.put(user.getId(), user);
    }
    user.setSession(session);
    session.setUser(user);
  }
}

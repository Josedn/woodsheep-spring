package io.bobba.woodsheep.core.communication.outgoing.user;

import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;
import io.bobba.woodsheep.core.users.User;

public class LoginOkEventComposer extends OutgoingMessage {

  private final String userId;
  private final String username;

  public LoginOkEventComposer(User user) {
    super("loginOk");
    this.userId = user.id();
    this.username = user.username();
  }

  @Override
  public Object getPayload() {
    return new Payload(userId, username);
  }

  private record Payload(String userId, String username) {}
}

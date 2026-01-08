package io.bobba.woodsheep.misc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WoodsheepUUID {
  private static final Set<String> usedAlready = new HashSet<>();

  /**
   * @return a random string, distinct from any other string so far returned by this method.
   */
  public static String generateUUID() {
    String uuid = UUID.randomUUID().toString();
    while (usedAlready.contains(uuid)) {
      uuid = UUID.randomUUID().toString();
    }
    usedAlready.add(uuid);
    return uuid;
  }
}

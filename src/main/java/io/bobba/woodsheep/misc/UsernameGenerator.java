package io.bobba.woodsheep.misc;

import java.util.Random;

public class UsernameGenerator {
  private static final Random RANDOM = new Random();

  private static final String[] ADJECTIVES = {"Fast", "Dark", "Silent", "Crazy", "Lucky", "Brave"};

  private static final String[] NOUNS = {"Wolf", "Tiger", "Falcon", "Knight", "Dragon", "Hacker"};

  public static String generateUsername() {
    String adjective = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
    String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
    int number = 100 + RANDOM.nextInt(900);

    return adjective + noun + String.format("%03d", number);
  }
}

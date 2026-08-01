package io.bobba.catanatron.enums;

public enum Color {
  RED,
  BLUE,
  ORANGE,
  WHITE;

  @Override
  public String toString() {
    return "C." + name();
  }
}

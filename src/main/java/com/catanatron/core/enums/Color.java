package com.catanatron.core.enums;

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

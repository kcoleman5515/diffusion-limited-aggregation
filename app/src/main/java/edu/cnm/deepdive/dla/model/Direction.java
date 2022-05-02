package edu.cnm.deepdive.dla.model;

import java.util.Random;

public enum Direction {
  NORTH(0, -1),
  EAST(1, 0),
  SOUTH(0, 1),
  WEST(-1, 0);

  private final int offsetX;
  private final int offsetY;

  Direction(int offsetX, int offsetY) {
    this.offsetX = offsetX;
    this.offsetY = offsetY;
  }

  public static Direction random(Random rng) {
    Direction[] values = values();
    return values[rng.nextInt(values.length)];
  }

  public int getOffsetX() {
    return offsetX;
  }

  public int getOffsetY() {
    return offsetY;
  }

}

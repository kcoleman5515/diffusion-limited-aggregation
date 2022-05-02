/*
 *  Copyright 2022 CNM Ingenuity, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.dla.model;

import java.util.BitSet;
import java.util.Random;

public class Lattice {

  public static final int DEFAULT_SIZE = 250;
  private static final double TAU = 2 * Math.PI;

  private final int size;
  private final Random rng;
  private final double centerX;
  private final double centerY;
  private final int escapeRadiusSquared;
  private final BitSet grid;

  private int mass;
  private boolean boundaryReached;

  private Lattice(int size, Random rng) {
    this.size = size;
    this.rng = rng;
    centerX = centerY = size / 2d;
    escapeRadiusSquared = 2 * size * size;
    grid = new BitSet(size * size);
    mass = 0;
    boundaryReached = false;
  }

  public int getSize() {
    return size;
  }

  public synchronized void set(int x, int y, boolean value) {
    boolean alreadySet = get(x, y);
    if (value && !alreadySet) {
      mass++;
    } else if (!value && alreadySet) {
      mass--;
    }
    grid.set(y * size + x, value);
  }

  public void set(int x, int y) {
    set(x, y, true);
  }

  public void clear(int x, int y) {
    set(x, y, false);
  }

  public void clear() {
    grid.clear();
    mass = 0;
    boundaryReached = false;
  }

  public boolean get(int x, int y) {
    return grid.get(y * size + x);
  }

  public BitSet get() {
    return grid;
  }

  public int getMass() {
    return mass;
  }

  public boolean isBoundaryReached() {
    return boundaryReached;
  }

  public void accumulate() {
    if (mass > 0) {
      boolean accumulated = false;
      do {
        double theta = rng.nextDouble() * TAU;
        int x = (int) Math.round(centerX + size * Math.cos(theta));
        int y = (int) Math.round(centerY + size * Math.sin(theta));
        while (!isEscaped(x, y)) {
          if (isAdjacentToAggregate(x, y)) {
            accumulated = true;
            set(x, y);
            boundaryReached = isOnBoundary(x, y);
            break;
          }
          Direction d = Direction.random(rng);
          x += d.getOffsetX();
          y += d.getOffsetY();
        }
      } while (!accumulated);
    }
  }

  private boolean isAdjacentToAggregate(int x, int y) {
    boolean adjacent = false;
    if (isInBounds(x, y)) {
      for (Direction d : Direction.values()) {
        int neighborX = x + d.getOffsetX();
        int neighborY = y + d.getOffsetY();
        if (isInBounds(neighborX, neighborY) && get(neighborX, neighborY)) {
          adjacent = true;
          break;
        }
      }
    }
    return adjacent;
  }

  private boolean isEscaped(int x, int y) {
    double deltaX = x - centerX;
    double deltaY = y - centerY;
    return (deltaX * deltaX + deltaY * deltaY > escapeRadiusSquared);
  }

  private boolean isInBounds(int x, int y) {
    return (x >= 0 && x < size && y >= 0 && y < size);
  }

  private boolean isOnBoundary(int x, int y) {
    return (x == 0 || x == size - 1 || y == 0 || y == size - 1);
  }

  public static class Builder {

    private int size = DEFAULT_SIZE;
    private Random rng;

    public Builder size(int size) {
      this.size = size;
      return this;
    }

    public Builder rng(Random rng) {
      this.rng = rng;
      return this;
    }

    public Lattice build() {
      return new Lattice(size, (rng != null) ? rng : new Random());
    }

  }

}

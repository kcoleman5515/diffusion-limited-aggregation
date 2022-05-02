package edu.cnm.deepdive.dla.model

import java.util.*

enum class Direction(val offsetX: Int, val offsetY: Int) {
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    companion object {
        @JvmStatic
        fun random(rng: Random): Direction {
            val values = values()
            return values[rng.nextInt(values.size)]
        }
    }
}
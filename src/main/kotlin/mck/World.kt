package mck

import kotlin.math.sin
import kotlin.random.Random

/**
 * A 2D grid of blocks. Row 0 is the top of the world; y increases downward.
 * Positions outside the left, right or bottom edges are solid, so the player
 * cannot leave the world. Positions above the top edge are air.
 */
class World(val width: Int = 240, val height: Int = 96, seed: Long = 42L) {

    private val tiles = Array(width * height) { Block.AIR }
    private val rng = Random(seed)

    init {
        generate()
    }

    fun get(x: Int, y: Int): Block {
        if (x < 0 || x >= width || y < 0 || y >= height) return Block.AIR
        return tiles[y * width + x]
    }

    fun set(x: Int, y: Int, block: Block) {
        if (x < 0 || x >= width || y < 0 || y >= height) return
        tiles[y * width + x] = block
    }

    fun isSolid(x: Int, y: Int): Boolean {
        if (x < 0 || x >= width || y >= height) return true
        if (y < 0) return false
        return tiles[y * width + x].solid
    }

    /** Returns the row of the highest solid block in column [x], or height if none. */
    fun surfaceAt(x: Int): Int {
        for (y in 0 until height) {
            if (get(x, y).solid) return y
        }
        return height
    }

    private fun generate() {
        val baseLine = height / 2
        val phaseA = rng.nextDouble() * 6.0
        val phaseB = rng.nextDouble() * 6.0

        val surfaces = IntArray(width) { x ->
            baseLine + (sin(x * 0.07 + phaseA) * 6.0 + sin(x * 0.19 + phaseB) * 2.0).toInt()
        }

        for (x in 0 until width) {
            val surface = surfaces[x]
            for (y in 0 until height) {
                val block = when {
                    y < surface -> Block.AIR
                    y == surface -> Block.GRASS
                    y < surface + 4 -> Block.DIRT
                    else -> Block.STONE
                }
                set(x, y, block)
            }
        }

        // Sand patches in low-lying columns.
        for (x in 0 until width) {
            if (surfaces[x] >= baseLine + 3 && rng.nextInt(4) == 0) {
                set(x, surfaces[x], Block.SAND)
                set(x, surfaces[x] + 1, Block.SAND)
            }
        }

        // Trees, kept away from the world edges.
        var x = 3
        while (x < width - 3) {
            if (rng.nextInt(9) == 0 && get(x, surfaces[x]) == Block.GRASS) {
                plantTree(x, surfaces[x])
                x += 4
            } else {
                x += 1
            }
        }
    }

    private fun plantTree(x: Int, groundY: Int) {
        val trunkHeight = 4
        for (i in 1..trunkHeight) {
            set(x, groundY - i, Block.WOOD)
        }
        val top = groundY - trunkHeight
        for (dy in -2..0) {
            for (dx in -2..2) {
                val lx = x + dx
                val ly = top + dy
                if (get(lx, ly) == Block.AIR) {
                    set(lx, ly, Block.LEAVES)
                }
            }
        }
    }
}

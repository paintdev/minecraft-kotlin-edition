package mck

import java.awt.Color

/** Block types. [color] is null for air, which is drawn as sky. */
enum class Block(val color: Color?, val solid: Boolean) {
    AIR(null, false),
    GRASS(Color(94, 160, 66), true),
    DIRT(Color(134, 96, 67), true),
    STONE(Color(128, 128, 128), true),
    WOOD(Color(122, 88, 48), true),
    LEAVES(Color(60, 130, 50), true),
    SAND(Color(219, 205, 143), true)
}

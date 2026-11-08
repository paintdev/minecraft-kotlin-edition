Made entirely by Claude (not code) 

pbbbbbbbbbbbbrrrrrrrtttttttttttt (Sorry, I'm just so gassy today!)

# Minecraft: Kotlin Edition

A small 2D block-building sandbox written in Kotlin. It uses only the Kotlin
standard library and the JDK's built-in Swing, so there are no external dependencies.

## Run

Requires JDK 17 or newer.

    ./gradlew run

## Controls

| Input              | Action                       |
|--------------------|------------------------------|
| A / Left arrow     | Move left                    |
| D / Right arrow    | Move right                   |
| Space / W / Up     | Jump                         |
| Left click         | Break block (adds to stock)  |
| Right click        | Place selected block         |
| 1 - 6              | Select hotbar slot           |

Blocks can only be reached within 5 tiles of the player.

## Project layout

- `src/main/kotlin/mck/Block.kt`  block types and colours
- `src/main/kotlin/mck/World.kt`  terrain generation and tile storage
- `src/main/kotlin/mck/Game.kt`   window, physics, input and rendering

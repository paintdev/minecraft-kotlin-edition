package mck

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

const val TILE_PX = 16
const val PLAYER_WIDTH = 0.8
const val PLAYER_HEIGHT = 1.8
const val GRAVITY = 30.0
const val MAX_FALL_SPEED = 25.0
const val JUMP_SPEED = 10.5
const val MOVE_SPEED = 6.0
const val REACH = 5.0
const val START_STOCK = 20

val HOTBAR = listOf(Block.GRASS, Block.DIRT, Block.STONE, Block.WOOD, Block.LEAVES, Block.SAND)

class Player(var x: Double, var y: Double) {
    /** Feet position is [y]; the body extends upward from it. */
    var vx = 0.0
    var vy = 0.0
    var onGround = false
}

class GamePanel(private val world: World) : JPanel(), ActionListener {

    private val player: Player
    private val stock = IntArray(Block.values().size)
    private var selected = 0

    private var moveLeft = false
    private var moveRight = false
    private var jumpHeld = false

    private var lastNanos = System.nanoTime()
    private val timer = Timer(16, this)

    init {
        val spawnX = world.width / 2
        val spawnY = world.surfaceAt(spawnX).toDouble()
        player = Player(spawnX + 0.5, spawnY)

        for (block in HOTBAR) stock[block.ordinal] = START_STOCK

        preferredSize = Dimension(960, 600)
        background = Color(135, 206, 235)
        isFocusable = true

        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) = handleKey(e.keyCode, true)
            override fun keyReleased(e: KeyEvent) = handleKey(e.keyCode, false)
        })

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                when (e.button) {
                    MouseEvent.BUTTON1 -> breakBlock(e.x, e.y)
                    MouseEvent.BUTTON3 -> placeBlock(e.x, e.y)
                }
            }
        })

        timer.start()
    }

    private fun handleKey(code: Int, down: Boolean) {
        when (code) {
            KeyEvent.VK_A, KeyEvent.VK_LEFT -> moveLeft = down
            KeyEvent.VK_D, KeyEvent.VK_RIGHT -> moveRight = down
            KeyEvent.VK_SPACE, KeyEvent.VK_W, KeyEvent.VK_UP -> jumpHeld = down
        }
        if (down && code >= KeyEvent.VK_1 && code <= KeyEvent.VK_6) {
            selected = code - KeyEvent.VK_1
        }
    }

    // ---- Camera and coordinate helpers ----

    private fun cameraX(): Double = player.x * TILE_PX - width / 2.0
    private fun cameraY(): Double = (player.y - PLAYER_HEIGHT / 2) * TILE_PX - height / 2.0

    private fun tileAt(screenX: Int, screenY: Int): Pair<Int, Int> {
        val tx = floor((screenX + cameraX()) / TILE_PX).toInt()
        val ty = floor((screenY + cameraY()) / TILE_PX).toInt()
        return Pair(tx, ty)
    }

    private fun inReach(tx: Int, ty: Int): Boolean {
        val cx = player.x
        val cy = player.y - PLAYER_HEIGHT / 2
        val dx = tx + 0.5 - cx
        val dy = ty + 0.5 - cy
        return sqrt(dx * dx + dy * dy) <= REACH
    }

    // ---- Interaction ----

    private fun breakBlock(screenX: Int, screenY: Int) {
        val (tx, ty) = tileAt(screenX, screenY)
        if (!inReach(tx, ty)) return
        val block = world.get(tx, ty)
        if (block == Block.AIR) return
        world.set(tx, ty, Block.AIR)
        stock[block.ordinal]++
        repaint()
    }

    private fun placeBlock(screenX: Int, screenY: Int) {
        val (tx, ty) = tileAt(screenX, screenY)
        if (!inReach(tx, ty)) return
        if (world.get(tx, ty) != Block.AIR) return
        val block = HOTBAR[selected]
        if (stock[block.ordinal] <= 0) return
        if (overlapsPlayer(tx, ty)) return
        world.set(tx, ty, block)
        stock[block.ordinal]--
        repaint()
    }

    private fun overlapsPlayer(tx: Int, ty: Int): Boolean {
        val left = player.x - PLAYER_WIDTH / 2
        val right = player.x + PLAYER_WIDTH / 2
        val top = player.y - PLAYER_HEIGHT
        val bottom = player.y
        return tx + 1 > left && tx < right && ty + 1 > top && ty < bottom
    }

    // ---- Physics ----

    private fun collides(cx: Double, feet: Double): Boolean {
        val left = cx - PLAYER_WIDTH / 2
        val right = cx + PLAYER_WIDTH / 2
        val top = feet - PLAYER_HEIGHT
        val bottom = feet
        val x0 = floor(left).toInt()
        val x1 = floor(right - 1e-9).toInt()
        val y0 = floor(top).toInt()
        val y1 = floor(bottom - 1e-9).toInt()
        for (ty in y0..y1) {
            for (tx in x0..x1) {
                if (world.isSolid(tx, ty)) return true
            }
        }
        return false
    }

    private fun moveX(distance: Double) {
        val steps = (abs(distance) / 0.05).toInt() + 1
        val step = distance / steps
        repeat(steps) {
            if (collides(player.x + step, player.y)) {
                player.vx = 0.0
                return
            }
            player.x += step
        }
    }

    private fun moveY(distance: Double) {
        val steps = (abs(distance) / 0.05).toInt() + 1
        val step = distance / steps
        repeat(steps) {
            if (collides(player.x, player.y + step)) {
                player.vy = 0.0
                return
            }
            player.y += step
        }
    }

    private fun update(dt: Double) {
        val direction = (if (moveRight) 1 else 0) - (if (moveLeft) 1 else 0)
        player.vx = direction * MOVE_SPEED

        if (jumpHeld && player.onGround) {
            player.vy = -JUMP_SPEED
        }
        player.vy = min(player.vy + GRAVITY * dt, MAX_FALL_SPEED)

        moveX(player.vx * dt)
        moveY(player.vy * dt)

        player.onGround = collides(player.x, player.y + 0.02)
        if (player.onGround && player.vy > 0) player.vy = 0.0

        // Respawn if the player somehow falls out of the world.
        if (player.y > world.height + 10) {
            val spawnX = world.width / 2
            player.x = spawnX + 0.5
            player.y = world.surfaceAt(spawnX).toDouble()
            player.vy = 0.0
        }
    }

    // ---- Timing ----

    override fun actionPerformed(e: ActionEvent) {
        val now = System.nanoTime()
        val dt = min(0.05, (now - lastNanos) / 1e9)
        lastNanos = now
        update(dt)
        repaint()
    }

    // ---- Rendering ----

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val camX = cameraX()
        val camY = cameraY()

        g2.paint = GradientPaint(0f, 0f, Color(110, 180, 235), 0f, height.toFloat(), Color(200, 230, 250))
        g2.fillRect(0, 0, width, height)

        val firstX = max(0, floor(camX / TILE_PX).toInt())
        val lastX = min(world.width - 1, floor((camX + width) / TILE_PX).toInt())
        val firstY = max(0, floor(camY / TILE_PX).toInt())
        val lastY = min(world.height - 1, floor((camY + height) / TILE_PX).toInt())

        for (ty in firstY..lastY) {
            for (tx in firstX..lastX) {
                val block = world.get(tx, ty)
                val color = block.color ?: continue
                val sx = (tx * TILE_PX - camX).roundToInt()
                val sy = (ty * TILE_PX - camY).roundToInt()
                g2.color = color
                g2.fillRect(sx, sy, TILE_PX, TILE_PX)
            }
        }

        drawPlayer(g2, camX, camY)
        drawHotbar(g2)
        drawHud(g2)
    }

    private fun drawPlayer(g2: Graphics2D, camX: Double, camY: Double) {
        val left = (player.x - PLAYER_WIDTH / 2) * TILE_PX - camX
        val top = (player.y - PLAYER_HEIGHT) * TILE_PX - camY
        val w = PLAYER_WIDTH * TILE_PX
        val h = PLAYER_HEIGHT * TILE_PX
        g2.color = Color(40, 90, 200)
        g2.fillRect(left.roundToInt(), top.roundToInt(), w.roundToInt(), h.roundToInt())
        g2.color = Color(240, 200, 160)
        g2.fillRect(left.roundToInt() + 2, top.roundToInt() + 2, w.roundToInt() - 4, 8)
        g2.color = Color(30, 30, 30)
        g2.stroke = BasicStroke(1f)
        g2.drawRect(left.roundToInt(), top.roundToInt(), w.roundToInt(), h.roundToInt())
    }

    private fun drawHotbar(g2: Graphics2D) {
        val slot = 48
        val pad = 6
        val startX = 16
        val startY = height - slot - 16
        for ((i, block) in HOTBAR.withIndex()) {
            val x = startX + i * (slot + pad)
            g2.color = Color(0, 0, 0, 120)
            g2.fillRect(x, startY, slot, slot)
            g2.color = block.color ?: Color.BLACK
            g2.fillRect(x + 8, startY + 8, slot - 16, slot - 16)
            g2.color = if (i == selected) Color.YELLOW else Color.WHITE
            g2.stroke = BasicStroke(if (i == selected) 3f else 1f)
            g2.drawRect(x, startY, slot, slot)
            g2.font = Font(Font.SANS_SERIF, Font.BOLD, 12)
            g2.drawString("${i + 1}", x + 4, startY + 14)
            g2.drawString("${stock[block.ordinal]}", x + 4, startY + slot - 6)
        }
    }

    private fun drawHud(g2: Graphics2D) {
        g2.color = Color.WHITE
        g2.font = Font(Font.SANS_SERIF, Font.BOLD, 14)
        g2.drawString("Minecraft: Kotlin Edition", 16, 24)
        g2.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        g2.drawString("A/D move  Space jump  Left click break  Right click place  1-6 select", 16, 42)
    }
}

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("Minecraft: Kotlin Edition")
        val panel = GamePanel(World())
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.contentPane.add(panel)
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
        panel.requestFocusInWindow()
    }
}

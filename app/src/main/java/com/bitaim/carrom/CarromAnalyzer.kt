package com.bitaim.carrom

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.*

data class CoinInfo(
    val x: Float,
    val y: Float,
    val color: CoinColor,
    val radius: Float = 28f
)

data class ShotInfo(
    val strikerX: Float,
    val strikerY: Float,
    val targetCoin: CoinInfo,
    val pocketX: Float,
    val pocketY: Float,
    val hitX: Float,
    val hitY: Float,
    val power: Float,
    val bouncePoints: List<Pair<Float, Float>>,
    val allCoins: List<CoinInfo>,
    val boardBounds: BoardBounds,
    val isBestShot: Boolean
)

data class BoardBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

enum class CoinColor { BLACK, WHITE, RED, STRIKER, UNKNOWN }

object CarromAnalyzer {

    fun analyze(bitmap: Bitmap, screenW: Int, screenH: Int): ShotInfo? {
        val board = detectBoard(screenW, screenH)
        val coins = detectCoins(bitmap, board)
        val striker = detectStriker(bitmap, board) ?: estimateStriker(board)
        val pockets = getPockets(board)

        if (coins.isEmpty()) return null

        return findBestShot(striker, coins, pockets, board)
    }

    private fun detectBoard(screenW: Int, screenH: Int): BoardBounds {
        // Carrom Pool board is roughly center square of screen
        val size = minOf(screenW, screenH) * 0.82f
        val left = (screenW - size) / 2f
        val top = (screenH - size) / 2f
        return BoardBounds(left, top, left + size, top + size)
    }

    private fun detectCoins(bitmap: Bitmap, board: BoardBounds): List<CoinInfo> {
        val coins = mutableListOf<CoinInfo>()
        val step = 12
        val bLeft = board.left.toInt().coerceAtLeast(0)
        val bTop = board.top.toInt().coerceAtLeast(0)
        val bRight = board.right.toInt().coerceAtMost(bitmap.width - 1)
        val bBottom = board.bottom.toInt().coerceAtMost(bitmap.height - 1)

        val visited = HashSet<Long>()

        var y = bTop
        while (y < bBottom) {
            var x = bLeft
            while (x < bRight) {
                val pixel = bitmap.getPixel(x, y)
                val coinColor = classifyPixelColor(pixel)

                if (coinColor != CoinColor.UNKNOWN && coinColor != CoinColor.STRIKER) {
                    val key = (x / 40).toLong() * 10000 + (y / 40).toLong()
                    if (!visited.contains(key)) {
                        visited.add(key)
                        val cx = x.toFloat()
                        val cy = y.toFloat()
                        // avoid duplicates close together
                        val tooClose = coins.any {
                            sqrt((it.x - cx).pow(2) + (it.y - cy).pow(2)) < 45f
                        }
                        if (!tooClose) {
                            coins.add(CoinInfo(cx, cy, coinColor))
                        }
                    }
                }
                x += step
            }
            y += step
        }
        return coins
    }

    private fun classifyPixelColor(pixel: Int): CoinColor {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)

        // Black coin: dark pixel
        if (r < 60 && g < 60 && b < 60) return CoinColor.BLACK

        // White coin: bright pixel
        if (r > 200 && g > 200 && b > 200) return CoinColor.WHITE

        // Red queen: red dominant
        if (r > 150 && g < 80 && b < 80) return CoinColor.RED

        return CoinColor.UNKNOWN
    }

    private fun detectStriker(bitmap: Bitmap, board: BoardBounds): CoinInfo? {
        // Striker is usually at bottom center of board, larger and lighter color
        val searchY = (board.bottom - (board.bottom - board.top) * 0.15f).toInt()
        val searchYEnd = board.bottom.toInt().coerceAtMost(bitmap.height - 1)
        val searchXStart = board.left.toInt()
        val searchXEnd = board.right.toInt().coerceAtMost(bitmap.width - 1)

        var y = searchY
        while (y < searchYEnd) {
            var x = searchXStart
            while (x < searchXEnd) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Striker: yellowish or distinct color
                if (r > 180 && g > 150 && b < 100) {
                    return CoinInfo(x.toFloat(), y.toFloat(), CoinColor.STRIKER, 35f)
                }
                x += 8
            }
            y += 8
        }
        return null
    }

    private fun estimateStriker(board: BoardBounds): CoinInfo {
        // Fallback: bottom center of board
        val cx = (board.left + board.right) / 2f
        val cy = board.bottom - (board.bottom - board.top) * 0.1f
        return CoinInfo(cx, cy, CoinColor.STRIKER, 35f)
    }

    private fun getPockets(board: BoardBounds): List<Pair<Float, Float>> {
        val margin = 20f
        return listOf(
            Pair(board.left + margin, board.top + margin),       // top-left
            Pair(board.right - margin, board.top + margin),      // top-right
            Pair(board.left + margin, board.bottom - margin),    // bottom-left
            Pair(board.right - margin, board.bottom - margin)    // bottom-right
        )
    }

    private fun findBestShot(
        striker: CoinInfo,
        coins: List<CoinInfo>,
        pockets: List<Pair<Float, Float>>,
        board: BoardBounds
    ): ShotInfo? {

        data class ShotCandidate(
            val coin: CoinInfo,
            val pocket: Pair<Float, Float>,
            val hitX: Float,
            val hitY: Float,
            val score: Float,
            val bounces: List<Pair<Float, Float>>
        )

        val candidates = mutableListOf<ShotCandidate>()

        for (coin in coins) {
            if (coin.color == CoinColor.STRIKER) continue

            for (pocket in pockets) {
                // Direction from coin to pocket
                val toPocketDx = pocket.first - coin.x
                val toPocketDy = pocket.second - coin.y
                val toPocketLen = sqrt(toPocketDx.pow(2) + toPocketDy.pow(2))
                if (toPocketLen < 1f) continue

                val normPX = toPocketDx / toPocketLen
                val normPY = toPocketDy / toPocketLen

                // Hit point: striker must hit coin from opposite direction of pocket
                val hitX = coin.x - normPX * (striker.radius + coin.radius)
                val hitY = coin.y - normPY * (striker.radius + coin.radius)

                // Check if hit point is reachable from striker
                val toHitDx = hitX - striker.x
                val toHitDy = hitY - striker.y
                val toHitLen = sqrt(toHitDx.pow(2) + toHitDy.pow(2))

                if (toHitLen < 10f) continue

                // Check path is clear
                if (!isPathClear(striker.x, striker.y, hitX, hitY, coins, coin)) continue

                // Score: prefer closer coins, prefer pockets closer to coin
                val distScore = 1f / (toHitLen + 1f)
                val pocketDist = toPocketLen
                val pocketScore = 1f / (pocketDist + 1f)

                // Bonus for red queen
                val colorBonus = if (coin.color == CoinColor.RED) 2f else 1f

                val score = (distScore + pocketScore) * colorBonus

                val bounces = calculateBounces(hitX, hitY, normPX, normPY, board, 2)

                candidates.add(ShotCandidate(coin, pocket, hitX, hitY, score, bounces))
            }
        }

        if (candidates.isEmpty()) return null

        val best = candidates.maxByOrNull { it.score }!!
        val power = calculatePower(striker.x, striker.y, best.hitX, best.hitY, best.coin, best.pocket)

        return ShotInfo(
            strikerX = striker.x,
            strikerY = striker.y,
            targetCoin = best.coin,
            pocketX = best.pocket.first,
            pocketY = best.pocket.second,
            hitX = best.hitX,
            hitY = best.hitY,
            power = power,
            bouncePoints = best.bounces,
            allCoins = coins,
            boardBounds = board,
            isBestShot = true
        )
    }

    private fun isPathClear(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        coins: List<CoinInfo>,
        excludeCoin: CoinInfo
    ): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        val len = sqrt(dx.pow(2) + dy.pow(2))
        if (len < 1f) return true

        val nx = dx / len
        val ny = dy / len

        for (coin in coins) {
            if (coin == excludeCoin || coin.color == CoinColor.STRIKER) continue

            // Distance from coin center to line
            val tx = coin.x - x1
            val ty = coin.y - y1
            val proj = tx * nx + ty * ny
            if (proj < 0 || proj > len) continue

            val perpDist = abs(tx * ny - ty * nx)
            if (perpDist < coin.radius + 20f) return false
        }
        return true
    }

    private fun calculateBounces(
        startX: Float, startY: Float,
        dirX: Float, dirY: Float,
        board: BoardBounds,
        maxBounces: Int
    ): List<Pair<Float, Float>> {
        val points = mutableListOf<Pair<Float, Float>>()
        var x = startX
        var y = startY
        var vx = dirX
        var vy = dirY
        val stepLen = 20f

        repeat(maxBounces * 30) {
            x += vx * stepLen
            y += vy * stepLen

            var bounced = false
            if (x < board.left) { x = board.left; vx = -vx; bounced = true }
            if (x > board.right) { x = board.right; vx = -vx; bounced = true }
            if (y < board.top) { y = board.top; vy = -vy; bounced = true }
            if (y > board.bottom) { y = board.bottom; vy = -vy; bounced = true }

            if (bounced) {
                points.add(Pair(x, y))
                if (points.size >= maxBounces) return points
            }
        }
        return points
    }

    private fun calculatePower(
        sx: Float, sy: Float,
        hx: Float, hy: Float,
        coin: CoinInfo,
        pocket: Pair<Float, Float>
    ): Float {
        val strikerToCoin = sqrt((hx - sx).pow(2) + (hy - sy).pow(2))
        val coinToPocket = sqrt((pocket.first - coin.x).pow(2) + (pocket.second - coin.y).pow(2))
        val totalDist = strikerToCoin + coinToPocket

        // Normalize to 0-100 power scale
        val maxDist = 1500f
        return (totalDist / maxDist * 100f).coerceIn(20f, 100f)
    }
}

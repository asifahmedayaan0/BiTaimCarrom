package com.bitaim.carrom

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*

class CarromOverlayView(context: Context, private val screenW: Int, private val screenH: Int) : View(context) {

    private var currentShot: ShotInfo? = null

    // ── Aim line ──────────────────────────────────────────────
    private val aimLinePaint = Paint().apply {
        color = Color.argb(230, 255, 50, 50)
        strokeWidth = 3.5f
        style = Paint.Style.STROKE
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(22f, 10f), 0f)
    }

    // ── Coin path after hit ───────────────────────────────────
    private val coinPathPaint = Paint().apply {
        color = Color.argb(200, 50, 255, 100)
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(16f, 8f), 0f)
    }

    // ── Bounce line ───────────────────────────────────────────
    private val bouncePaint = Paint().apply {
        color = Color.argb(160, 100, 180, 255)
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    // ── Ghost circle at hit point ─────────────────────────────
    private val ghostPaint = Paint().apply {
        color = Color.argb(180, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    // ── Pocket highlight ──────────────────────────────────────
    private val pocketPaint = Paint().apply {
        color = Color.argb(200, 255, 220, 0)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    // ── Coin outline ──────────────────────────────────────────
    private val coinOutlinePaint = Paint().apply {
        color = Color.argb(180, 255, 100, 100)
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    // ── Power bar ─────────────────────────────────────────────
    private val powerBgPaint = Paint().apply {
        color = Color.argb(180, 30, 30, 30)
        style = Paint.Style.FILL
    }
    private val powerFillPaint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val powerTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 26f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    // ── Info text ─────────────────────────────────────────────
    private val infoTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 1f, 1f, Color.BLACK)
    }

    private val labelBgPaint = Paint().apply {
        color = Color.argb(160, 0, 0, 0)
        style = Paint.Style.FILL
    }

    fun updateAnalysis(shot: ShotInfo?) {
        currentShot = shot
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val shot = currentShot
        if (shot == null) {
            drawWaiting(canvas)
            return
        }

        // 1. Highlight all detected coins
        drawDetectedCoins(canvas, shot)

        // 2. Highlight target pocket
        drawPocketHighlight(canvas, shot)

        // 3. Striker → hit point aim line
        drawAimLine(canvas, shot)

        // 4. Ghost striker circle at hit point
        drawGhostCircle(canvas, shot)

        // 5. Coin path → pocket
        drawCoinPath(canvas, shot)

        // 6. Bounce prediction lines
        drawBounceLines(canvas, shot)

        // 7. Power bar
        drawPowerBar(canvas, shot)

        // 8. Info labels
        drawInfoLabels(canvas, shot)
    }

    private fun drawDetectedCoins(canvas: Canvas, shot: ShotInfo) {
        for (coin in shot.allCoins) {
            val paint = when (coin.color) {
                CoinColor.BLACK -> Paint().apply { color = Color.argb(180, 80, 80, 80); style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
                CoinColor.WHITE -> Paint().apply { color = Color.argb(180, 220, 220, 220); style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
                CoinColor.RED   -> Paint().apply { color = Color.argb(220, 255, 50, 50); style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true }
                else -> continue
            }
            canvas.drawCircle(coin.x, coin.y, coin.radius + 6f, paint)

            // Red queen label
            if (coin.color == CoinColor.RED) {
                canvas.drawText("Q", coin.x - 8f, coin.y + 8f, infoTextPaint)
            }

            // Highlight best target coin
            if (coin == shot.targetCoin) {
                canvas.drawCircle(coin.x, coin.y, coin.radius + 12f, coinOutlinePaint)
            }
        }
    }

    private fun drawPocketHighlight(canvas: Canvas, shot: ShotInfo) {
        canvas.drawCircle(shot.pocketX, shot.pocketY, 28f, pocketPaint)
        // Pulsing inner dot
        val innerPaint = Paint().apply {
            color = Color.argb(180, 255, 220, 0)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(shot.pocketX, shot.pocketY, 10f, innerPaint)
    }

    private fun drawAimLine(canvas: Canvas, shot: ShotInfo) {
        canvas.drawLine(shot.strikerX, shot.strikerY, shot.hitX, shot.hitY, aimLinePaint)

        // Arrowhead at hit point
        val dx = shot.hitX - shot.strikerX
        val dy = shot.hitY - shot.strikerY
        val len = sqrt(dx * dx + dy * dy)
        if (len > 0) {
            val nx = dx / len
            val ny = dy / len
            val arrowSize = 18f
            val ax = shot.hitX - nx * arrowSize
            val ay = shot.hitY - ny * arrowSize
            val arrowPaint = Paint().apply {
                color = Color.argb(230, 255, 80, 80)
                strokeWidth = 3f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawLine(shot.hitX, shot.hitY, ax - ny * arrowSize * 0.5f, ay + nx * arrowSize * 0.5f, arrowPaint)
            canvas.drawLine(shot.hitX, shot.hitY, ax + ny * arrowSize * 0.5f, ay - nx * arrowSize * 0.5f, arrowPaint)
        }
    }

    private fun drawGhostCircle(canvas: Canvas, shot: ShotInfo) {
        canvas.drawCircle(shot.hitX, shot.hitY, shot.targetCoin.radius, ghostPaint)
    }

    private fun drawCoinPath(canvas: Canvas, shot: ShotInfo) {
        canvas.drawLine(shot.targetCoin.x, shot.targetCoin.y, shot.pocketX, shot.pocketY, coinPathPaint)
    }

    private fun drawBounceLines(canvas: Canvas, shot: ShotInfo) {
        if (shot.bouncePoints.isEmpty()) return
        var prevX = shot.strikerX
        var prevY = shot.strikerY
        for ((bx, by) in shot.bouncePoints) {
            canvas.drawLine(prevX, prevY, bx, by, bouncePaint)
            canvas.drawCircle(bx, by, 6f, Paint().apply {
                color = Color.argb(180, 100, 180, 255)
                style = Paint.Style.FILL
                isAntiAlias = true
            })
            prevX = bx
            prevY = by
        }
    }

    private fun drawPowerBar(canvas: Canvas, shot: ShotInfo) {
        val barX = 40f
        val barY = screenH * 0.5f
        val barW = 28f
        val barH = 180f

        // Background
        canvas.drawRoundRect(barX, barY, barX + barW, barY + barH, 8f, 8f, powerBgPaint)

        // Fill based on power
        val fillH = barH * (shot.power / 100f)
        val fillY = barY + barH - fillH
        powerFillPaint.color = when {
            shot.power < 40f -> Color.argb(220, 50, 220, 50)
            shot.power < 70f -> Color.argb(220, 255, 180, 0)
            else             -> Color.argb(220, 255, 50, 50)
        }
        canvas.drawRoundRect(barX, fillY, barX + barW, barY + barH, 8f, 8f, powerFillPaint)

        // Label
        canvas.drawText("PWR", barX - 2f, barY - 10f, infoTextPaint)
        canvas.drawText("${shot.power.toInt()}%", barX - 2f, barY + barH + 28f, infoTextPaint)
    }

    private fun drawInfoLabels(canvas: Canvas, shot: ShotInfo) {
        val padding = 10f
        val lineH = 30f
        val startX = screenW - 220f
        val startY = screenH * 0.3f

        val coinName = when (shot.targetCoin.color) {
            CoinColor.BLACK -> "Black Coin"
            CoinColor.WHITE -> "White Coin"
            CoinColor.RED   -> "Red Queen"
            else            -> "Coin"
        }

        val angle = Math.toDegrees(
            atan2(
                (shot.hitY - shot.strikerY).toDouble(),
                (shot.hitX - shot.strikerX).toDouble()
            )
        )

        val lines = listOf(
            "Target: $coinName",
            "Angle: ${String.format("%.1f", angle)}°",
            "Power: ${shot.power.toInt()}%",
            "Coins: ${shot.allCoins.size}"
        )

        canvas.drawRoundRect(
            startX - padding, startY - lineH,
            startX + 210f, startY + lines.size * lineH + padding,
            12f, 12f, labelBgPaint
        )

        lines.forEachIndexed { i, text ->
            canvas.drawText(text, startX, startY + i * lineH, infoTextPaint)
        }
    }

    private fun drawWaiting(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.argb(180, 255, 50, 50)
            textSize = 32f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(4f, 1f, 1f, Color.BLACK)
        }
        canvas.drawText("BiTaim — Detecting board...", 60f, screenH * 0.1f, paint)
    }
}

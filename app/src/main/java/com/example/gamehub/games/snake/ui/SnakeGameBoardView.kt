package com.example.gamehub.games.snake.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.gamehub.R
import com.example.gamehub.games.snake.domain.SnakeEngine
import com.example.gamehub.games.snake.model.SnakeDirection
import com.example.gamehub.games.snake.model.SnakeFood
import com.example.gamehub.games.snake.model.SnakePosition
import kotlin.math.min

/**
 * Vista personalizada de Canvas de alto rendimiento para el renderizado del tablero de Snake.
 * Dibuja la cuadrícula, la comida con efectos visuales y la serpiente con cabeza orientada y ojos expresivos.
 */
class SnakeGameBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var gridWidth = SnakeEngine.DEFAULT_GRID_WIDTH
    private var gridHeight = SnakeEngine.DEFAULT_GRID_HEIGHT

    private var snake: List<SnakePosition> = emptyList()
    private var direction: SnakeDirection = SnakeDirection.RIGHT
    private var food: SnakeFood? = null

    // Pinceles y Colores
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val eyeWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val eyePupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val applePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val goldenApplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    init {
        updateThemeColors()
    }

    /**
     * Sincroniza los pinceles con la paleta de colores del tema actual (White opaco / Dark intermedio).
     */
    fun updateThemeColors() {
        bgPaint.color = ContextCompat.getColor(context, R.color.snake_board_bg)
        gridPaint.color = ContextCompat.getColor(context, R.color.snake_grid_line)
        headPaint.color = ContextCompat.getColor(context, R.color.snake_head)
        bodyPaint.color = ContextCompat.getColor(context, R.color.snake_body)
        applePaint.color = ContextCompat.getColor(context, R.color.snake_apple)
        goldenApplePaint.color = ContextCompat.getColor(context, R.color.snake_golden_apple)
        leafPaint.color = ContextCompat.getColor(context, R.color.win_glow)
        invalidate()
    }

    private val cellRect = RectF()
    private val boardRect = RectF()
    private val cornerRadius = 18f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        boardRect.set(0f, 0f, w, h)

        // 1. Fondo del tablero con esquinas redondeadas
        canvas.drawRoundRect(boardRect, cornerRadius, cornerRadius, bgPaint)

        val cellW = w / gridWidth
        val cellH = h / gridHeight

        // 2. Cuadrícula tenue
        for (col in 1 until gridWidth) {
            val x = col * cellW
            canvas.drawLine(x, 0f, x, h, gridPaint)
        }
        for (row in 1 until gridHeight) {
            val y = row * cellH
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        // 3. Dibujar Manzana / Alimento
        food?.let { f ->
            val fx = f.position.x * cellW
            val fy = f.position.y * cellH
            val padding = cellW * 0.12f
            cellRect.set(fx + padding, fy + padding, fx + cellW - padding, fy + cellH - padding)

            val paintToUse = if (f.isGolden) goldenApplePaint else applePaint
            val radius = (cellW - (padding * 2)) / 2f
            canvas.drawCircle(cellRect.centerX(), cellRect.centerY(), radius, paintToUse)

            // Tallo/Hoja pequeña de la manzana
            canvas.drawCircle(cellRect.centerX() + (radius * 0.3f), cellRect.top + (radius * 0.1f), radius * 0.28f, leafPaint)
        }

        // 4. Dibujar Gusanito
        if (snake.isNotEmpty()) {
            // A. Cuerpo (de la cola a antes de la cabeza)
            val bodyPadding = cellW * 0.08f
            for (i in (snake.size - 1) downTo 1) {
                val segment = snake[i]
                val sx = segment.x * cellW
                val sy = segment.y * cellH
                cellRect.set(sx + bodyPadding, sy + bodyPadding, sx + cellW - bodyPadding, sy + cellH - bodyPadding)
                canvas.drawRoundRect(cellRect, 10f, 10f, bodyPaint)
            }

            // B. Cabeza del Gusanito
            val head = snake[0]
            val hx = head.x * cellW
            val hy = head.y * cellH
            val headPadding = cellW * 0.05f
            cellRect.set(hx + headPadding, hy + headPadding, hx + cellW - headPadding, hy + cellH - headPadding)
            canvas.drawRoundRect(cellRect, 12f, 12f, headPaint)

            // C. Ojos Expresivos orientados hacia la dirección
            val eyeRadius = cellW * 0.12f
            val pupilRadius = cellW * 0.06f

            var eye1X: Float
            var eye1Y: Float
            var eye2X: Float
            var eye2Y: Float

            when (direction) {
                SnakeDirection.UP -> {
                    eye1X = cellRect.centerX() - (cellW * 0.22f)
                    eye1Y = cellRect.centerY() - (cellH * 0.2f)
                    eye2X = cellRect.centerX() + (cellW * 0.22f)
                    eye2Y = cellRect.centerY() - (cellH * 0.2f)
                }
                SnakeDirection.DOWN -> {
                    eye1X = cellRect.centerX() - (cellW * 0.22f)
                    eye1Y = cellRect.centerY() + (cellH * 0.2f)
                    eye2X = cellRect.centerX() + (cellW * 0.22f)
                    eye2Y = cellRect.centerY() + (cellH * 0.2f)
                }
                SnakeDirection.LEFT -> {
                    eye1X = cellRect.centerX() - (cellW * 0.2f)
                    eye1Y = cellRect.centerY() - (cellH * 0.22f)
                    eye2X = cellRect.centerX() - (cellW * 0.2f)
                    eye2Y = cellRect.centerY() + (cellH * 0.22f)
                }
                SnakeDirection.RIGHT -> {
                    eye1X = cellRect.centerX() + (cellW * 0.2f)
                    eye1Y = cellRect.centerY() - (cellH * 0.22f)
                    eye2X = cellRect.centerX() + (cellW * 0.2f)
                    eye2Y = cellRect.centerY() + (cellH * 0.22f)
                }
            }

            // Ojos blancos
            canvas.drawCircle(eye1X, eye1Y, eyeRadius, eyeWhitePaint)
            canvas.drawCircle(eye2X, eye2Y, eyeRadius, eyeWhitePaint)

            // Pupilas negras
            canvas.drawCircle(eye1X, eye1Y, pupilRadius, eyePupilPaint)
            canvas.drawCircle(eye2X, eye2Y, pupilRadius, eyePupilPaint)
        }
    }

    /**
     * Actualiza el estado visual y solicita redibujado instantáneo a 60/120 FPS.
     */
    fun renderState(
        snake: List<SnakePosition>,
        direction: SnakeDirection,
        food: SnakeFood?,
        gridWidth: Int = SnakeEngine.DEFAULT_GRID_WIDTH,
        gridHeight: Int = SnakeEngine.DEFAULT_GRID_HEIGHT
    ) {
        this.snake = snake
        this.direction = direction
        this.food = food
        this.gridWidth = gridWidth
        this.gridHeight = gridHeight
        invalidate()
    }
}
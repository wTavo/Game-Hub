package com.example.gamehub.games.snake.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.gamehub.R
import com.example.gamehub.games.snake.model.SnakeDirection
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Vista personalizada de D-Pad donde cada botón es un rectángulo con una cola triangular
 * que unifica a los 4 botones convergiendo en las puntas en el centro común.
 */
class SnakeDpadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onDirectionChanged: ((SnakeDirection) -> Unit)? = null

    private var centerX = 0f
    private var centerY = 0f
    private var dpadRadius = 0f
    private var rectWidth = 0f
    private var rectHeight = 0f
    private var tailLength = 0f
    private val tailGap = 3f // Unión central en las puntas de los triángulos

    private var activeDirection: SnakeDirection? = null

    // Pinceles
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        pathEffect = CornerPathEffect(12f)
    }

    private val buttonPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        pathEffect = CornerPathEffect(12f)
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        pathEffect = CornerPathEffect(12f)
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val arrowPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val upPath = Path()
    private val downPath = Path()
    private val leftPath = Path()
    private val rightPath = Path()
    private val arrowPath = Path()

    init {
        updateColors()
    }

    /**
     * Sincroniza los colores con el tema activo (Modo Claro / Modo Oscuro).
     */
    fun updateColors() {
        buttonPaint.color = ContextCompat.getColor(context, R.color.bg_card)
        buttonPressedPaint.color = ContextCompat.getColor(context, R.color.snake_joystick_thumb)
        strokePaint.color = ContextCompat.getColor(context, R.color.cell_stroke)
        arrowPaint.color = ContextCompat.getColor(context, R.color.text_primary)
        arrowPressedPaint.color = ContextCompat.getColor(context, R.color.bg_primary)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f

        dpadRadius = min(w, h) / 2.1f
        rectWidth = dpadRadius * 0.74f
        rectHeight = dpadRadius * 0.52f
        tailLength = dpadRadius * 0.42f

        val halfW = rectWidth / 2f

        // 1. Botón Arriba (Rectángulo exterior + Cola Triangular al centro)
        val upTop = centerY - dpadRadius
        val upBottom = upTop + rectHeight
        upPath.reset()
        upPath.moveTo(centerX, centerY - tailGap) // Punta de la cola triangular
        upPath.lineTo(centerX - halfW, upBottom) // Base izquierda del rectángulo
        upPath.lineTo(centerX - halfW, upTop)    // Esquina superior izquierda
        upPath.lineTo(centerX + halfW, upTop)    // Esquina superior derecha
        upPath.lineTo(centerX + halfW, upBottom) // Base derecha del rectángulo
        upPath.close()

        // 2. Botón Abajo (Rectángulo exterior + Cola Triangular al centro)
        val downBottom = centerY + dpadRadius
        val downTop = downBottom - rectHeight
        downPath.reset()
        downPath.moveTo(centerX, centerY + tailGap) // Punta de la cola triangular
        downPath.lineTo(centerX + halfW, downTop)    // Base derecha del rectángulo
        downPath.lineTo(centerX + halfW, downBottom) // Esquina inferior derecha
        downPath.lineTo(centerX - halfW, downBottom) // Esquina inferior izquierda
        downPath.lineTo(centerX - halfW, downTop)    // Base izquierda del rectángulo
        downPath.close()

        // 3. Botón Izquierda (Rectángulo exterior + Cola Triangular al centro)
        val leftOuter = centerX - dpadRadius
        val leftInner = leftOuter + rectHeight
        leftPath.reset()
        leftPath.moveTo(centerX - tailGap, centerY) // Punta de la cola triangular
        leftPath.lineTo(leftInner, centerY + halfW)  // Base inferior del rectángulo
        leftPath.lineTo(leftOuter, centerY + halfW)  // Esquina exterior inferior
        leftPath.lineTo(leftOuter, centerY - halfW)  // Esquina exterior superior
        leftPath.lineTo(leftInner, centerY - halfW)  // Base superior del rectángulo
        leftPath.close()

        // 4. Botón Derecha (Rectángulo exterior + Cola Triangular al centro)
        val rightOuter = centerX + dpadRadius
        val rightInner = rightOuter - rectHeight
        rightPath.reset()
        rightPath.moveTo(centerX + tailGap, centerY) // Punta de la cola triangular
        rightPath.lineTo(rightInner, centerY - halfW) // Base superior del rectángulo
        rightPath.lineTo(rightOuter, centerY - halfW) // Esquina exterior superior
        rightPath.lineTo(rightOuter, centerY + halfW) // Esquina exterior inferior
        rightPath.lineTo(rightInner, centerY + halfW) // Base inferior del rectángulo
        rightPath.close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dpadRadius <= 0f) return

        // 1. Dibujar Botón Arriba
        val paintUp = if (activeDirection == SnakeDirection.UP) buttonPressedPaint else buttonPaint
        canvas.drawPath(upPath, paintUp)
        canvas.drawPath(upPath, strokePaint)

        // 2. Dibujar Botón Abajo
        val paintDown = if (activeDirection == SnakeDirection.DOWN) buttonPressedPaint else buttonPaint
        canvas.drawPath(downPath, paintDown)
        canvas.drawPath(downPath, strokePaint)

        // 3. Dibujar Botón Izquierda
        val paintLeft = if (activeDirection == SnakeDirection.LEFT) buttonPressedPaint else buttonPaint
        canvas.drawPath(leftPath, paintLeft)
        canvas.drawPath(leftPath, strokePaint)

        // 4. Dibujar Botón Derecha
        val paintRight = if (activeDirection == SnakeDirection.RIGHT) buttonPressedPaint else buttonPaint
        canvas.drawPath(rightPath, paintRight)
        canvas.drawPath(rightPath, strokePaint)

        // 5. Flechas Direccionales centradas en el cuerpo rectangular de cada botón
        val arrowSize = rectWidth * 0.22f
        val offset = dpadRadius - (rectHeight / 2f)

        val arrowUpPaint = if (activeDirection == SnakeDirection.UP) arrowPressedPaint else arrowPaint
        drawTriangle(canvas, centerX, centerY - offset, arrowSize, SnakeDirection.UP, arrowUpPaint)

        val arrowDownPaint = if (activeDirection == SnakeDirection.DOWN) arrowPressedPaint else arrowPaint
        drawTriangle(canvas, centerX, centerY + offset, arrowSize, SnakeDirection.DOWN, arrowDownPaint)

        val arrowLeftPaint = if (activeDirection == SnakeDirection.LEFT) arrowPressedPaint else arrowPaint
        drawTriangle(canvas, centerX - offset, centerY, arrowSize, SnakeDirection.LEFT, arrowLeftPaint)

        val arrowRightPaint = if (activeDirection == SnakeDirection.RIGHT) arrowPressedPaint else arrowPaint
        drawTriangle(canvas, centerX + offset, centerY, arrowSize, SnakeDirection.RIGHT, arrowRightPaint)
    }

    private fun drawTriangle(canvas: Canvas, cx: Float, cy: Float, size: Float, dir: SnakeDirection, paint: Paint) {
        arrowPath.reset()
        when (dir) {
            SnakeDirection.UP -> {
                arrowPath.moveTo(cx, cy - size)
                arrowPath.lineTo(cx - size * 0.85f, cy + size * 0.85f)
                arrowPath.lineTo(cx + size * 0.85f, cy + size * 0.85f)
            }
            SnakeDirection.DOWN -> {
                arrowPath.moveTo(cx, cy + size)
                arrowPath.lineTo(cx - size * 0.85f, cy - size * 0.85f)
                arrowPath.lineTo(cx + size * 0.85f, cy - size * 0.85f)
            }
            SnakeDirection.LEFT -> {
                arrowPath.moveTo(cx - size, cy)
                arrowPath.lineTo(cx + size * 0.85f, cy - size * 0.85f)
                arrowPath.lineTo(cx + size * 0.85f, cy + size * 0.85f)
            }
            SnakeDirection.RIGHT -> {
                arrowPath.moveTo(cx + size, cy)
                arrowPath.lineTo(cx - size * 0.85f, cy - size * 0.85f)
                arrowPath.lineTo(cx - size * 0.85f, cy + size * 0.85f)
            }
        }
        arrowPath.close()
        canvas.drawPath(arrowPath, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                // Detección angular inmediata a través de las colas triangulares
                if (distance > 5f) {
                    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

                    val newDir = when {
                        angle >= -45f && angle < 45f -> SnakeDirection.RIGHT
                        angle >= 45f && angle < 135f -> SnakeDirection.DOWN
                        angle >= -135f && angle < -45f -> SnakeDirection.UP
                        else -> SnakeDirection.LEFT
                    }

                    if (newDir != activeDirection) {
                        activeDirection = newDir
                        onDirectionChanged?.invoke(newDir)
                        invalidate()
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeDirection = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
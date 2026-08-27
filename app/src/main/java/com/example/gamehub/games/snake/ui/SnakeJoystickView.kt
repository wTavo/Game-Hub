package com.example.gamehub.games.snake.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import com.example.gamehub.R
import com.example.gamehub.games.snake.model.SnakeDirection
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Palanca Arcade de 4 Vías (4-Way Gate Joystick con camino de esquina continuo).
 * Permite que el pomo viaje en un camino perimetral suave y continuo entre ejes
 * (de izquierda a abajo, etc.) sin saltos ni teletransportaciones visuales.
 */
class SnakeJoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onDirectionChanged: ((SnakeDirection) -> Unit)? = null

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var thumbRadius = 0f
    private var trackWidth = 0f
    private var deadzoneRadius = 0f
    private var maxTravel = 0f
    private var triggerThreshold = 0f

    // Posición física y visual directa del pomo
    private var thumbX = 0f
    private var thumbY = 0f

    private var lastDirection: SnakeDirection? = null
    private var returnAnimator: ValueAnimator? = null

    // Pinceles
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val baseStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val trackStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    // Pincel para la marca de Zona Muerta (círculo central punteado)
    private val deadzoneStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Pomo sólido uniforme
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val thumbStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val horizontalTrackRect = RectF()
    private val verticalTrackRect = RectF()
    private val arrowPath = Path()

    init {
        updateColors()
    }

    /**
     * Sincroniza los colores con el tema activo (Modo Claro / Modo Oscuro).
     */
    fun updateColors() {
        basePaint.color = ContextCompat.getColor(context, R.color.snake_joystick_base)
        baseStrokePaint.color = ContextCompat.getColor(context, R.color.cell_stroke)
        trackPaint.color = ContextCompat.getColor(context, R.color.bg_primary)
        trackStrokePaint.color = ContextCompat.getColor(context, R.color.cell_stroke)
        deadzoneStrokePaint.color = ContextCompat.getColor(context, R.color.text_muted)
        arrowPaint.color = ContextCompat.getColor(context, R.color.text_muted)
        thumbPaint.color = ContextCompat.getColor(context, R.color.snake_joystick_thumb)
        thumbStrokePaint.color = ContextCompat.getColor(context, R.color.cell_stroke)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(w, h) / 2.15f

        // Bolita del centro en diseño más grande y ranura arcade
        thumbRadius = baseRadius * 0.68f
        trackWidth = baseRadius * 0.74f

        val maxTrackLen = baseRadius * 1.84f
        horizontalTrackRect.set(
            centerX - (maxTrackLen / 2f),
            centerY - (trackWidth / 2f),
            centerX + (maxTrackLen / 2f),
            centerY + (trackWidth / 2f)
        )

        verticalTrackRect.set(
            centerX - (trackWidth / 2f),
            centerY - (maxTrackLen / 2f),
            centerX + (trackWidth / 2f),
            centerY + (maxTrackLen / 2f)
        )

        // Recorrido máximo delimitado dentro de la base para que la bolita grande nunca se corte
        maxTravel = baseRadius - thumbRadius - 3f

        // Zona muerta ágil y sensible (28% del recorrido)
        deadzoneRadius = maxTravel * 0.28f
        triggerThreshold = deadzoneRadius

        thumbX = centerX
        thumbY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (baseRadius <= 0f) return

        // 1. Base circular exterior
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        canvas.drawCircle(centerX, centerY, baseRadius, baseStrokePaint)

        // 2. Ranura amplia en Cruz Arcade (4-Way Track)
        val trackCorner = trackWidth / 2f
        canvas.drawRoundRect(horizontalTrackRect, trackCorner, trackCorner, trackPaint)
        canvas.drawRoundRect(verticalTrackRect, trackCorner, trackCorner, trackPaint)
        canvas.drawRoundRect(horizontalTrackRect, trackCorner, trackCorner, trackStrokePaint)
        canvas.drawRoundRect(verticalTrackRect, trackCorner, trackCorner, trackStrokePaint)

        // 3. Indicadores direccionales en su posición original dentro de cada extremo
        drawDirectionalArrows(canvas)

        // 4. Pomo de la palanca arcade grande (100% visible)
        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbPaint)
        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbStrokePaint)
    }

    /**
     * Dibuja los indicadores direccionales en su posición y proporción original.
     */
    private fun drawDirectionalArrows(canvas: Canvas) {
        val arrowSize = baseRadius * 0.11f
        val offset = baseRadius * 0.68f

        // Arriba
        drawTriangle(canvas, centerX, centerY - offset, arrowSize, SnakeDirection.UP)
        // Abajo
        drawTriangle(canvas, centerX, centerY + offset, arrowSize, SnakeDirection.DOWN)
        // Izquierda
        drawTriangle(canvas, centerX - offset, centerY, arrowSize, SnakeDirection.LEFT)
        // Derecha
        drawTriangle(canvas, centerX + offset, centerY, arrowSize, SnakeDirection.RIGHT)
    }

    private fun drawTriangle(canvas: Canvas, cx: Float, cy: Float, size: Float, dir: SnakeDirection) {
        arrowPath.reset()
        when (dir) {
            SnakeDirection.UP -> {
                arrowPath.moveTo(cx, cy - size)
                arrowPath.lineTo(cx - size * 0.85f, cy + size)
                arrowPath.lineTo(cx + size * 0.85f, cy + size)
            }
            SnakeDirection.DOWN -> {
                arrowPath.moveTo(cx, cy + size)
                arrowPath.lineTo(cx - size * 0.85f, cy - size)
                arrowPath.lineTo(cx + size * 0.85f, cy - size)
            }
            SnakeDirection.LEFT -> {
                arrowPath.moveTo(cx - size, cy)
                arrowPath.lineTo(cx + size, cy - size * 0.85f)
                arrowPath.lineTo(cx + size, cy + size * 0.85f)
            }
            SnakeDirection.RIGHT -> {
                arrowPath.moveTo(cx + size, cy)
                arrowPath.lineTo(cx - size, cy - size * 0.85f)
                arrowPath.lineTo(cx - size, cy + size * 0.85f)
            }
        }
        arrowPath.close()
        canvas.drawPath(arrowPath, arrowPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                returnAnimator?.cancel()
                handleTouchPosition(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                handleTouchPosition(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                lastDirection = null
                animateThumbReturnToCenter()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Restringe el movimiento del pomo estrictamente a los 4 ejes ortogonales en Cruz (+)
     * sin permitir desplazamientos diagonales, manteniendo la bolita siempre 100% visible.
     */
    private fun handleTouchPosition(touchX: Float, touchY: Float) {
        val dx = touchX - centerX
        val dy = touchY - centerY
        val absX = abs(dx)
        val absY = abs(dy)

        if (absX == 0f && absY == 0f) {
            thumbX = centerX
            thumbY = centerY
            lastDirection = null
            invalidate()
            return
        }

        // Restricción ortogonal estricta en Cruz (+) de 4 vías (Cero diagonales)
        if (absX >= absY) {
            // Eje Horizontal Puro (Izquierda / Derecha)
            thumbY = centerY
            val clampedX = dx.coerceIn(-maxTravel, maxTravel)
            thumbX = centerX + clampedX

            if (abs(clampedX) >= deadzoneRadius) {
                val dir = if (clampedX > 0) SnakeDirection.RIGHT else SnakeDirection.LEFT
                if (dir != lastDirection) {
                    lastDirection = dir
                    onDirectionChanged?.invoke(dir)
                }
            } else {
                lastDirection = null
            }
        } else {
            // Eje Vertical Puro (Arriba / Abajo)
            thumbX = centerX
            val clampedY = dy.coerceIn(-maxTravel, maxTravel)
            thumbY = centerY + clampedY

            if (abs(clampedY) >= deadzoneRadius) {
                val dir = if (clampedY > 0) SnakeDirection.DOWN else SnakeDirection.UP
                if (dir != lastDirection) {
                    lastDirection = dir
                    onDirectionChanged?.invoke(dir)
                }
            } else {
                lastDirection = null
            }
        }

        invalidate()
    }

    /**
     * Retorno suave con resorte al centro neutro.
     */
    private fun animateThumbReturnToCenter() {
        returnAnimator?.cancel()
        val startX = thumbX
        val startY = thumbY

        returnAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 150L
            interpolator = OvershootInterpolator(1.3f)
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                thumbX = startX + (centerX - startX) * fraction
                thumbY = startY + (centerY - startY) * fraction
                invalidate()
            }
            start()
        }
    }
}
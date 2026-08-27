package com.example.gamehub.core.animations

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator

/**
 * Utilidad de animaciones fluidas para la interfaz de usuario de Tres en Raya.
 * Proporciona efectos elásticos de aparición (pop-in), transiciones suaves de entrada/salida
 * (slide & fade) para controles y botones, animaciones de victoria y limpieza de tablero.
 */
object GameAnimations {

    /**
     * Animación de aparición pop-in elástica (escala 0.0 -> 1.15 -> 1.0) con efecto de rebote.
     * Utilizada al colocar una ficha X u O en una celda del tablero.
     *
     * @param view Vista a animar (típicamente TextView de la celda).
     * @param duration Duración en milisegundos.
     * @param onEnd Callback opcional invocado al finalizar la animación.
     */
    fun animatePopIn(view: View, duration: Long = 300L, onEnd: (() -> Unit)? = null) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0f
        view.visibility = View.VISIBLE

        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f)

        ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha).apply {
            this.duration = duration
            interpolator = OvershootInterpolator(1.8f)
            if (onEnd != null) {
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        onEnd()
                    }
                })
            }
            start()
        }
    }

    /**
     * Animación unificada y armónica para el marcador al cambiar cualquier configuración (modo, ficha o dificultad).
     * Aplica un único pase fluido de escala elástica suave (0.92 -> 1.0) y opacidad (0.4 -> 1.0)
     * sobre las tarjetas del marcador, garantizando una transición sólida sin artefactos ni dobles parpadeos.
     *
     * @param views Lista de vistas (tarjetas del marcador) a animar.
     */
    fun animateScoreboardUpdate(views: List<View>) {
        val animators = views.map { view ->
            val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.92f, 1f).apply {
                duration = 240L
                interpolator = OvershootInterpolator(1.2f)
            }
            val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.92f, 1f).apply {
                duration = 240L
                interpolator = OvershootInterpolator(1.2f)
            }
            val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.4f, 1f).apply {
                duration = 200L
                interpolator = AccelerateDecelerateInterpolator()
            }
            AnimatorSet().apply { playTogether(scaleX, scaleY, alpha) }
        }

        AnimatorSet().apply {
            playTogether(animators)
            start()
        }
    }

    /**
     * Animación de transición suave para las etiquetas del marcador (Tú / CPU / Ficha)
     * al cambiar de modo, preferencia o al revelarse el sorteo aleatorio.
     *
     * @param views Vistas de las etiquetas a animar.
     */
    fun animateScoreLabelChange(views: List<View>) {
        val animators = views.map { view ->
            val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.85f, 1.08f, 1f).apply {
                duration = 260L
                interpolator = OvershootInterpolator(1.5f)
            }
            val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.85f, 1.08f, 1f).apply {
                duration = 260L
                interpolator = OvershootInterpolator(1.5f)
            }
            val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.3f, 1f).apply {
                duration = 220L
            }
            AnimatorSet().apply { playTogether(scaleX, scaleY, alpha) }
        }

        AnimatorSet().apply {
            playTogether(animators)
            start()
        }
    }

    /**
     * Animación elástica de incremento para un contador del marcador que suma una victoria o empate.
     *
     * @param view Vista del número que se incrementa.
     */
    fun animateScoreValueIncrement(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.35f, 1f).apply {
            duration = 380L
            interpolator = OvershootInterpolator(2.2f)
        }
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.35f, 1f).apply {
            duration = 380L
            interpolator = OvershootInterpolator(2.2f)
        }
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }
    }

    /**
     * Animación de pulso continuo sobre las 3 celdas que conforman la línea ganadora.
     *
     * @param views Lista de vistas de las celdas ganadoras a pulsar.
     */
    fun animateWinningCells(views: List<View>) {
        val animators = views.map { view ->
            val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.1f, 1f).apply {
                duration = 600L
                repeatCount = 3
                repeatMode = ObjectAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
            }
            val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.1f, 1f).apply {
                duration = 600L
                repeatCount = 3
                repeatMode = ObjectAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
            }
            AnimatorSet().apply { playTogether(scaleX, scaleY) }
        }

        AnimatorSet().apply {
            playTogether(animators)
            start()
        }
    }

    /**
     * Animación de aparición del overlay de fin de partida (fondo nublado + tarjeta central con rebote).
     *
     * @param overlayView Capa de fondo semitransparente.
     * @param cardView Tarjeta central con el resultado y botón de reinicio.
     */
    fun animateOverlayIn(overlayView: View, cardView: View) {
        overlayView.alpha = 0f
        overlayView.visibility = View.VISIBLE
        cardView.scaleX = 0.7f
        cardView.scaleY = 0.7f
        cardView.alpha = 0f

        val overlayFade = ObjectAnimator.ofFloat(overlayView, View.ALPHA, 0f, 1f).apply {
            duration = 250L
        }

        val cardScaleX = ObjectAnimator.ofFloat(cardView, View.SCALE_X, 0.7f, 1f).apply {
            duration = 350L
            interpolator = OvershootInterpolator(1.4f)
        }
        val cardScaleY = ObjectAnimator.ofFloat(cardView, View.SCALE_Y, 0.7f, 1f).apply {
            duration = 350L
            interpolator = OvershootInterpolator(1.4f)
        }
        val cardAlpha = ObjectAnimator.ofFloat(cardView, View.ALPHA, 0f, 1f).apply {
            duration = 250L
        }

        AnimatorSet().apply {
            play(overlayFade).with(cardScaleX).with(cardScaleY).with(cardAlpha)
            start()
        }
    }

    /**
     * Animación de desaparición suave del overlay de fin de partida o de configuraciones.
     *
     * @param overlayView Vista del overlay a desvanecer.
     * @param onEnd Callback invocado tras ocultarse el overlay.
     */
    fun animateOverlayOut(overlayView: View, onEnd: () -> Unit = {}) {
        ObjectAnimator.ofFloat(overlayView, View.ALPHA, 1f, 0f).apply {
            duration = 200L
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    overlayView.visibility = View.GONE
                    onEnd()
                }
            })
            start()
        }
    }

    /**
     * Transición suave de fundido cruzado (crossfade) entre dos vistas dentro de un contenedor fijo.
     * Oculta suavemente viewToHide y muestra viewToShow sin mover ningún elemento circundante.
     *
     * @param viewToHide Vista que se desvanece y pasa a GONE.
     * @param viewToShow Vista que aparece gradualmente.
     * @param duration Duración en milisegundos de la transición.
     */
    fun animateCrossfade(viewToHide: View, viewToShow: View, duration: Long = 220L) {
        viewToShow.alpha = 0f
        viewToShow.visibility = View.VISIBLE

        val fadeOut = ObjectAnimator.ofFloat(viewToHide, View.ALPHA, 1f, 0f).apply {
            this.duration = duration
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    viewToHide.visibility = View.GONE
                    viewToHide.alpha = 1f
                }
            })
        }

        val fadeIn = ObjectAnimator.ofFloat(viewToShow, View.ALPHA, 0f, 1f).apply {
            this.duration = duration
        }

        AnimatorSet().apply {
            playTogether(fadeOut, fadeIn)
            start()
        }
    }

    /**
     * Animación de desvanecimiento suave para resetear y limpiar las celdas del tablero.
     *
     * @param views Lista de vistas de celdas a desvanecer.
     * @param onCleared Callback invocado al terminar el desvanecimiento para limpiar textos.
     */
    fun animateBoardClear(views: List<View>, onCleared: () -> Unit) {
        val animators = views.map { view ->
            val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).apply {
                duration = 180L
            }
            val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 0.5f).apply {
                duration = 180L
            }
            val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 0.5f).apply {
                duration = 180L
            }
            AnimatorSet().apply { playTogether(alpha, scaleX, scaleY) }
        }

        AnimatorSet().apply {
            playTogether(animators)
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onCleared()
                    // Restaurar propiedades visuales
                    views.forEach { v ->
                        v.alpha = 1f
                        v.scaleX = 1f
                        v.scaleY = 1f
                    }
                }
            })
            start()
        }
    }

    /**
     * Desvanecimiento de entrada genérico para vistas hijas.
     *
     * @param view Vista a desvanecer hacia adentro.
     * @param duration Duración en milisegundos.
     */
    fun animateFadeIn(view: View, duration: Long = 200L) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
            this.duration = duration
            start()
        }
    }

    /**
     * Desvanecimiento de salida genérico para vistas hijas.
     *
     * @param view Vista a desvanecer hacia afuera.
     * @param duration Duración en milisegundos.
     * @param onEnd Callback invocado al finalizar el desvanecimiento.
     */
    fun animateFadeOut(view: View, duration: Long = 180L, onEnd: (() -> Unit)? = null) {
        ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).apply {
            this.duration = duration
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    view.visibility = View.GONE
                    view.alpha = 1f
                    onEnd?.invoke()
                }
            })
            start()
        }
    }
}

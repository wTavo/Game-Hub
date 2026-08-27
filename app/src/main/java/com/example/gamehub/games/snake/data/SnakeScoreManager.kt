package com.example.gamehub.games.snake.data

import android.content.Context
import android.content.SharedPreferences
import com.example.gamehub.games.snake.model.SnakeControlType
import com.example.gamehub.games.snake.model.SnakeDifficulty
import com.example.gamehub.games.snake.model.SnakeGameMode
import androidx.core.content.edit

/**
 * Gestor de persistencia para el récord histórico (High Score) y preferencias de Snake.
 */
class SnakeScoreManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "snake_score_prefs"
        private const val KEY_HIGH_SCORE_PREFIX = "high_score_"
        private const val KEY_SELECTED_CONTROL = "snake_selected_control_type"
    }

    /**
     * Construye una clave única según el modo y la dificultad seleccionada.
     */
    private fun getScoreKey(mode: SnakeGameMode, difficulty: SnakeDifficulty): String {
        return "$KEY_HIGH_SCORE_PREFIX${mode.name}_${difficulty.name}"
    }

    /**
     * Obtiene el récord máximo alcanzado para el modo y dificultad actuales.
     */
    fun getHighScore(mode: SnakeGameMode, difficulty: SnakeDifficulty): Int {
        return prefs.getInt(getScoreKey(mode, difficulty), 0)
    }

    /**
     * Comprueba si la puntuación actual es un nuevo récord y la guarda en persistencia.
     * @return true si se estableció una nueva mejor marca histórica.
     */
    fun submitScore(mode: SnakeGameMode, difficulty: SnakeDifficulty, score: Int): Boolean {
        val currentHigh = getHighScore(mode, difficulty)
        return if (score > currentHigh) {
            prefs.edit { putInt(getScoreKey(mode, difficulty), score) }
            true
        } else {
            false
        }
    }

    /**
     * Obtiene el tipo de control guardado por el usuario (por defecto JOYSTICK / Palanca).
     */
    fun getSavedControlType(): SnakeControlType {
        val savedName = prefs.getString(KEY_SELECTED_CONTROL, null) ?: return SnakeControlType.JOYSTICK
        return try {
            SnakeControlType.valueOf(savedName)
        } catch (_: Exception) {
            SnakeControlType.JOYSTICK
        }
    }

    /**
     * Guarda la elección del esquema de control en persistencia.
     */
    fun saveControlType(type: SnakeControlType) {
        prefs.edit { putString(KEY_SELECTED_CONTROL, type.name) }
    }
}
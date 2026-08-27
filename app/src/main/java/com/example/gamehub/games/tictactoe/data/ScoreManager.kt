package com.example.gamehub.games.tictactoe.data

import android.content.Context
import android.content.SharedPreferences
import com.example.gamehub.games.tictactoe.model.Difficulty
import com.example.gamehub.games.tictactoe.model.GameMode
import androidx.core.content.edit

/**
 * Modelo inmutable que representa los puntajes acumulados de una modalidad:
 * victorias del Jugador 1 (Tú en 1J, Jugador 1 en 2J),
 * victorias del Jugador 2 (CPU en 1J, Jugador 2 en 2J) y empates.
 */
data class Score(
    val player1Wins: Int = 0,
    val player2Wins: Int = 0,
    val draws: Int = 0
)

/**
 * Administrador de persistencia de puntajes para cada modo de juego y nivel de dificultad.
 * Utiliza [SharedPreferences] de Android para garantizar que los marcadores persistan
 * de forma independiente entre sesiones, vinculando el puntaje al jugador y a la CPU.
 */
class ScoreManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tres_en_raya_scores", Context.MODE_PRIVATE)

    /**
     * Construye el prefijo de clave único para una combinación específica de modo y dificultad.
     *
     * @param mode Modo de juego (1 Jugador vs AI o 2 Jugadores).
     * @param difficulty Nivel de dificultad (Fácil, Medio, Difícil).
     * @return Cadena identificadora para las claves de SharedPreferences.
     */
    private fun getPrefix(mode: GameMode, difficulty: Difficulty): String {
        return if (mode == GameMode.TWO_PLAYERS) {
            "two_players"
        } else {
            "vs_ai_${difficulty.name.lowercase()}"
        }
    }

    /**
     * Obtiene el puntaje acumulado actual para el modo y dificultad solicitados.
     *
     * @param mode Modo de juego.
     * @param difficulty Dificultad seleccionada.
     * @return [Score] con las victorias del Jugador 1, Jugador 2/CPU y empates guardados.
     */
    fun getScore(mode: GameMode, difficulty: Difficulty): Score {
        val prefix = getPrefix(mode, difficulty)
        val p1 = prefs.getInt("${prefix}_p1_wins", 0)
        val p2 = prefs.getInt("${prefix}_p2_wins", 0)
        val draws = prefs.getInt("${prefix}_draws", 0)
        return Score(p1, p2, draws)
    }

    /**
     * Incrementa en 1 las victorias del Jugador 1 (Tú en modo 1 Jugador, Jugador 1 en 2 Jugadores)
     * para el modo y dificultad especificados.
     *
     * @param mode Modo de juego.
     * @param difficulty Dificultad seleccionada.
     * @return [Score] actualizado tras el incremento.
     */
    fun recordPlayer1Win(mode: GameMode, difficulty: Difficulty): Score {
        val prefix = getPrefix(mode, difficulty)
        val newWins = prefs.getInt("${prefix}_p1_wins", 0) + 1
        prefs.edit { putInt("${prefix}_p1_wins", newWins) }
        return getScore(mode, difficulty)
    }

    /**
     * Incrementa en 1 las victorias del Jugador 2 (CPU en modo 1 Jugador, Jugador 2 en 2 Jugadores)
     * para el modo y dificultad especificados.
     *
     * @param mode Modo de juego.
     * @param difficulty Dificultad seleccionada.
     * @return [Score] actualizado tras el incremento.
     */
    fun recordPlayer2Win(mode: GameMode, difficulty: Difficulty): Score {
        val prefix = getPrefix(mode, difficulty)
        val newWins = prefs.getInt("${prefix}_p2_wins", 0) + 1
        prefs.edit { putInt("${prefix}_p2_wins", newWins) }
        return getScore(mode, difficulty)
    }

    /**
     * Incrementa en 1 el contador de empates para el modo y dificultad especificados.
     *
     * @param mode Modo de juego.
     * @param difficulty Dificultad seleccionada.
     * @return [Score] actualizado tras el incremento.
     */
    fun recordDraw(mode: GameMode, difficulty: Difficulty): Score {
        val prefix = getPrefix(mode, difficulty)
        val newDraws = prefs.getInt("${prefix}_draws", 0) + 1
        prefs.edit { putInt("${prefix}_draws", newDraws) }
        return getScore(mode, difficulty)
    }
}

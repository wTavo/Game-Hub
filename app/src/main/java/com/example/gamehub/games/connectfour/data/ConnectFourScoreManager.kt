package com.example.gamehub.games.connectfour.data

import android.content.Context
import android.content.SharedPreferences
import com.example.gamehub.games.connectfour.model.ConnectFourDifficulty
import com.example.gamehub.games.connectfour.model.ConnectFourGameMode
import androidx.core.content.edit

/**
 * Modelo inmutable que representa el registro de puntuaciones de Conecta 4.
 */
data class ConnectFourScore(
    val player1Wins: Int = 0,
    val player2Wins: Int = 0,
    val draws: Int = 0
)

/**
 * Gestor de persistencia de puntuaciones de Conecta 4 mediante SharedPreferences.
 * Almacena de forma desacoplada las victorias del Jugador 1 / Tú, CPU / Jugador 2 y empates
 * para cada combinación de modo y nivel de dificultad.
 */
class ConnectFourScoreManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("connect_four_score_prefs", Context.MODE_PRIVATE)

    /**
     * Construye una clave única para identificar el récord de puntuación según modo y dificultad.
     */
    private fun buildKey(mode: ConnectFourGameMode, difficulty: ConnectFourDifficulty, field: String): String {
        return "c4_${mode.name}_${difficulty.name}_$field"
    }

    /**
     * Obtiene la puntuación almacenada para el modo y dificultad indicados.
     */
    fun getScore(mode: ConnectFourGameMode, difficulty: ConnectFourDifficulty): ConnectFourScore {
        val p1 = prefs.getInt(buildKey(mode, difficulty, "p1"), 0)
        val p2 = prefs.getInt(buildKey(mode, difficulty, "p2"), 0)
        val draws = prefs.getInt(buildKey(mode, difficulty, "draws"), 0)
        return ConnectFourScore(p1, p2, draws)
    }

    /**
     * Registra una victoria para el Jugador 1 (o Tú contra la IA).
     */
    fun recordPlayer1Win(mode: ConnectFourGameMode, difficulty: ConnectFourDifficulty): ConnectFourScore {
        val current = getScore(mode, difficulty)
        val updated = current.copy(player1Wins = current.player1Wins + 1)
        prefs.edit { putInt(buildKey(mode, difficulty, "p1"), updated.player1Wins) }
        return updated
    }

    /**
     * Registra una victoria para el Jugador 2 (o la CPU).
     */
    fun recordPlayer2Win(mode: ConnectFourGameMode, difficulty: ConnectFourDifficulty): ConnectFourScore {
        val current = getScore(mode, difficulty)
        val updated = current.copy(player2Wins = current.player2Wins + 1)
        prefs.edit { putInt(buildKey(mode, difficulty, "p2"), updated.player2Wins) }
        return updated
    }

    /**
     * Registra un empate en el marcador.
     */
    fun recordDraw(mode: ConnectFourGameMode, difficulty: ConnectFourDifficulty): ConnectFourScore {
        val current = getScore(mode, difficulty)
        val updated = current.copy(draws = current.draws + 1)
        prefs.edit { putInt(buildKey(mode, difficulty, "draws"), updated.draws) }
        return updated
    }

    /**
     * Restablece las puntuaciones a cero para el modo y dificultad indicados.
     */
    fun resetScore(mode: ConnectFourGameMode, difficulty: ConnectFourDifficulty): ConnectFourScore {
        prefs.edit {
            putInt(buildKey(mode, difficulty, "p1"), 0)
                .putInt(buildKey(mode, difficulty, "p2"), 0)
                .putInt(buildKey(mode, difficulty, "draws"), 0)
        }
        return ConnectFourScore(0, 0, 0)
    }
}
package com.example.gamehub.games.tictactoe.domain.ai

import com.example.gamehub.games.tictactoe.domain.TicTacToeEngine
import com.example.gamehub.games.tictactoe.model.CellPosition
import com.example.gamehub.games.tictactoe.model.Difficulty
import com.example.gamehub.games.tictactoe.model.Player
import kotlin.random.Random

/**
 * Proveedor de Inteligencia Artificial para el juego Tres en Raya.
 * Ofrece estrategias de juego diferenciadas según el nivel de dificultad seleccionado:
 * - [Difficulty.EASY]: Movimientos aleatorios y relajados.
 * - [Difficulty.MEDIUM]: Heurística reactiva (gana de inmediato o bloquea la victoria del jugador).
 * - [Difficulty.HARD]: Minimax imbatible con poda Alfa-Beta.
 */
object AIPlayer {

    /**
     * Calcula la siguiente jugada de la IA en función del nivel de dificultad.
     *
     * @param engine Motor con el estado actual del tablero.
     * @param difficulty Dificultad deseada (EASY, MEDIUM, HARD).
     * @param aiPlayer Jugador que representa a la IA (Player.O por defecto).
     * @return [CellPosition] calculada para colocar la ficha, o null si no hay casillas libres.
     */
    fun getNextMove(
        engine: TicTacToeEngine,
        difficulty: Difficulty,
        aiPlayer: Player = Player.O
    ): CellPosition? {
        val available = engine.getAvailableCells()
        if (available.isEmpty()) return null

        return when (difficulty) {
            Difficulty.EASY -> getEasyMove(available)
            Difficulty.MEDIUM -> getMediumMove(engine, aiPlayer, available)
            Difficulty.HARD -> MinimaxAlgorithm.findBestMove(engine, aiPlayer)
        }
    }

    /**
     * Estrategia Fácil: Selecciona una celda aleatoria entre las disponibles sin análisis táctico.
     *
     * @param available Lista de celdas libres en el tablero.
     * @return [CellPosition] seleccionada al azar.
     */
    private fun getEasyMove(available: List<CellPosition>): CellPosition {
        return available.random()
    }

    /**
     * Estrategia Media (Heurística):
     * 1. Si la IA puede ganar en este movimiento, toma la celda ganadora.
     * 2. Si el jugador humano está por ganar en el siguiente turno, bloquea la casilla.
     * 3. Si el centro está libre, intenta ocuparlo (65% de probabilidad).
     * 4. Si hay esquinas libres, intenta tomar una (50% de probabilidad).
     * 5. De lo contrario, elige una casilla libre aleatoria.
     *
     * @param engine Motor de juego.
     * @param aiPlayer Jugador de la IA.
     * @param available Lista de celdas libres.
     * @return [CellPosition] determinada por la heurística.
     */
    private fun getMediumMove(
        engine: TicTacToeEngine,
        aiPlayer: Player,
        available: List<CellPosition>
    ): CellPosition {
        val humanPlayer = aiPlayer.opponent()

        // 1. ¿Puede la IA ganar en este turno?
        for (move in available) {
            engine.makeMove(move, aiPlayer)
            val won = engine.checkWinner()?.winner == aiPlayer
            engine.board[move.row][move.col] = null
            if (won) return move
        }

        // 2. ¿Puede el jugador humano ganar en su próximo turno? Si es así, bloquear
        for (move in available) {
            engine.makeMove(move, humanPlayer)
            val won = engine.checkWinner()?.winner == humanPlayer
            engine.board[move.row][move.col] = null
            if (won) return move
        }

        // 3. Tomar el centro con 65% de probabilidad si está disponible
        val center = CellPosition(1, 1)
        if (available.contains(center) && Random.nextFloat() < 0.65f) {
            return center
        }

        // 4. Tomar una esquina con 50% de probabilidad si están disponibles
        val corners = listOf(
            CellPosition(0, 0),
            CellPosition(0, 2),
            CellPosition(2, 0),
            CellPosition(2, 2)
        ).filter { available.contains(it) }

        if (corners.isNotEmpty() && Random.nextFloat() < 0.5f) {
            return corners.random()
        }

        // 5. Movimiento aleatorio como fallback
        return available.random()
    }
}

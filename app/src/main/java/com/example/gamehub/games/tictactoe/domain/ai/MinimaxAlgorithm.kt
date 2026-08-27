package com.example.gamehub.games.tictactoe.domain.ai

import com.example.gamehub.games.tictactoe.domain.TicTacToeEngine
import com.example.gamehub.games.tictactoe.model.CellPosition
import com.example.gamehub.games.tictactoe.model.Player
import kotlin.math.max
import kotlin.math.min

/**
 * Implementación del algoritmo Minimax con poda Alfa-Beta (Alpha-Beta Pruning)
 * para calcular la mejor jugada matemáticamente óptima en Tres en Raya.
 * Garantiza que la Inteligencia Artificial sea imbatible (solo gana o empata).
 */
object MinimaxAlgorithm {

    /**
     * Encuentra la mejor jugada para el jugador [aiPlayer] en el estado actual de [engine].
     *
     * @param engine Estado del motor con el tablero actual.
     * @param aiPlayer Jugador que representa a la IA (típicamente Player.O).
     * @return [CellPosition] óptima para jugar, o null si el tablero está lleno.
     */
    fun findBestMove(engine: TicTacToeEngine, aiPlayer: Player): CellPosition? {
        val availableMoves = engine.getAvailableCells()
        if (availableMoves.isEmpty()) return null

        // Heurística de apertura: si el tablero está vacío, tomar el centro o una esquina ahorra cómputo
        if (availableMoves.size == 9) {
            val openings = listOf(
                CellPosition(1, 1),
                CellPosition(0, 0),
                CellPosition(0, 2),
                CellPosition(2, 0),
                CellPosition(2, 2)
            )
            return openings.random()
        }

        var bestScore = Int.MIN_VALUE
        var bestMove: CellPosition? = null

        for (move in availableMoves) {
            engine.makeMove(move, aiPlayer)
            val score = minimax(
                engine = engine,
                depth = 0,
                isMaximizing = false,
                aiPlayer = aiPlayer,
                alpha = Int.MIN_VALUE,
                beta = Int.MAX_VALUE
            )
            // Deshacer movimiento para restaurar el tablero original
            engine.board[move.row][move.col] = null

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }

        return bestMove ?: availableMoves.firstOrNull()
    }

    /**
     * Función recursiva Minimax con poda alfa-beta.
     * Evalúa recursivamente las ramas de jugadas posibles asignando puntuaciones:
     * - Victoria IA: (+10 - profundidad) -> Favorece victorias rápidas.
     * - Victoria Humano: (profundidad - 10) -> Retrasa derrotas al máximo.
     * - Empate: 0
     *
     * @param engine Motor de juego con el tablero simulado.
     * @param depth Profundidad actual en el árbol de recursión.
     * @param isMaximizing true si es el turno de maximizar (IA), false si es minimizar (Humano).
     * @param aiPlayer Jugador de la IA.
     * @param alpha Mejor valor que el maximizador puede garantizar.
     * @param beta Mejor valor que el minimizador puede garantizar.
     * @return Puntuación heurística de la rama evaluada.
     */
    private fun minimax(
        engine: TicTacToeEngine,
        depth: Int,
        isMaximizing: Boolean,
        aiPlayer: Player,
        alpha: Int,
        beta: Int
    ): Int {
        val winnerResult = engine.checkWinner()
        if (winnerResult != null) {
            return if (winnerResult.winner == aiPlayer) {
                10 - depth
            } else {
                depth - 10
            }
        }

        if (engine.isBoardFull()) {
            return 0
        }

        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in engine.getAvailableCells()) {
                engine.makeMove(move, aiPlayer)
                val eval = minimax(engine, depth + 1, false, aiPlayer, currentAlpha, currentBeta)
                engine.board[move.row][move.col] = null
                maxEval = max(maxEval, eval)
                currentAlpha = max(currentAlpha, eval)
                if (currentBeta <= currentAlpha) break // Poda Alfa-Beta
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            val humanPlayer = aiPlayer.opponent()
            for (move in engine.getAvailableCells()) {
                engine.makeMove(move, humanPlayer)
                val eval = minimax(engine, depth + 1, true, aiPlayer, currentAlpha, currentBeta)
                engine.board[move.row][move.col] = null
                minEval = min(minEval, eval)
                currentBeta = min(currentBeta, eval)
                if (currentBeta <= currentAlpha) break // Poda Alfa-Beta
            }
            return minEval
        }
    }
}

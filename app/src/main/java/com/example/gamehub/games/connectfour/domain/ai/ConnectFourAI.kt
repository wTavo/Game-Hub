package com.example.gamehub.games.connectfour.domain.ai

import com.example.gamehub.games.connectfour.domain.ConnectFourEngine
import com.example.gamehub.games.connectfour.model.ConnectFourDifficulty
import com.example.gamehub.games.connectfour.model.ConnectFourPiece
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Motor de Inteligencia Artificial para Conecta 4.
 * Emplea evaluación heurística de control central, puntuación de ventanas deslizantes
 * y el algoritmo Minimax con Poda Alfa-Beta para anticipar jugadas futuras.
 */
object ConnectFourAI {

    // Orden táctico preferente de columnas (priorizando el centro estratégico)
    private val COLUMN_SEARCH_ORDER = listOf(3, 2, 4, 1, 5, 0, 6)

    /**
     * Calcula la columna óptima en la que la IA debe soltar su ficha según el nivel de dificultad.
     * @return Índice de columna (0..6) o null si el tablero está lleno.
     */
    fun getNextMove(
        engine: ConnectFourEngine,
        difficulty: ConnectFourDifficulty,
        aiPiece: ConnectFourPiece
    ): Int? {
        val validCols = engine.getValidColumns()
        if (validCols.isEmpty()) return null

        return when (difficulty) {
            ConnectFourDifficulty.EASY -> getEasyMove(engine, validCols, aiPiece)
            ConnectFourDifficulty.MEDIUM -> getMediumMove(engine, validCols, aiPiece)
            ConnectFourDifficulty.HARD -> getHardMove(engine, aiPiece)
        }
    }

    /**
     * Nivel Fácil: Movimientos primordialmente casuales con baja anticipación.
     */
    private fun getEasyMove(
        engine: ConnectFourEngine,
        validCols: List<Int>,
        aiPiece: ConnectFourPiece
    ): Int {
        // 40% de probabilidad de tomar una victoria directa si existe
        if (Random.nextInt(100) < 40) {
            for (col in validCols) {
                val row = engine.getLowestAvailableRow(col) ?: continue
                engine.dropPiece(col, aiPiece)
                val isWin = engine.checkWinner()?.winner == aiPiece
                engine.undoMove(row, col)
                if (isWin) return col
            }
        }
        return validCols.random()
    }

    /**
     * Nivel Medio: Bloquea amenazas de victoria inmediata del rival y toma victorias propias;
     * de lo contrario, evalúa mediante heurística de corto alcance (profundidad 2).
     */
    private fun getMediumMove(
        engine: ConnectFourEngine,
        validCols: List<Int>,
        aiPiece: ConnectFourPiece
    ): Int {
        val opponent = aiPiece.opponent()

        // 1. Ganar si hay jugada ganadora inmediata
        for (col in validCols) {
            val row = engine.getLowestAvailableRow(col) ?: continue
            engine.dropPiece(col, aiPiece)
            val isWin = engine.checkWinner()?.winner == aiPiece
            engine.undoMove(row, col)
            if (isWin) return col
        }

        // 2. Bloquear si el oponente gana en la siguiente jugada
        for (col in validCols) {
            val row = engine.getLowestAvailableRow(col) ?: continue
            engine.dropPiece(col, opponent)
            val oppWin = engine.checkWinner()?.winner == opponent
            engine.undoMove(row, col)
            if (oppWin) return col
        }

        // 3. Minimax a profundidad 2
        return runMinimax(engine, depth = 2, aiPiece)
    }

    /**
     * Nivel Difícil: Minimax con Poda Alfa-Beta a profundidad 4 para juego táctico profundo.
     */
    private fun getHardMove(engine: ConnectFourEngine, aiPiece: ConnectFourPiece): Int {
        return runMinimax(engine, depth = 4, aiPiece)
    }

    /**
     * Ejecuta el árbol Minimax con Poda Alfa-Beta para seleccionar la mejor columna.
     */
    private fun runMinimax(
        engine: ConnectFourEngine,
        depth: Int,
        aiPiece: ConnectFourPiece
    ): Int {
        var bestScore = Int.MIN_VALUE
        var bestCol = engine.getValidColumns().first()

        for (col in COLUMN_SEARCH_ORDER) {
            val row = engine.getLowestAvailableRow(col) ?: continue
            engine.dropPiece(col, aiPiece)

            val score = minimax(
                engine = engine,
                depth = depth - 1,
                alpha = Int.MIN_VALUE,
                beta = Int.MAX_VALUE,
                isMaximizing = false,
                aiPiece = aiPiece
            )

            engine.undoMove(row, col)

            if (score > bestScore) {
                bestScore = score
                bestCol = col
            }
        }

        return bestCol
    }

    /**
     * Función recursiva Minimax con Poda Alfa-Beta.
     */
    private fun minimax(
        engine: ConnectFourEngine,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        aiPiece: ConnectFourPiece
    ): Int {
        var currentAlpha = alpha
        var currentBeta = beta
        val opponent = aiPiece.opponent()

        val winner = engine.checkWinner()?.winner
        if (winner == aiPiece) return 100_000 + depth
        if (winner == opponent) return -100_000 - depth
        if (engine.isBoardFull() || depth == 0) {
            return evaluateBoard(engine, aiPiece)
        }

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (col in COLUMN_SEARCH_ORDER) {
                val row = engine.getLowestAvailableRow(col) ?: continue
                engine.dropPiece(col, aiPiece)
                val evaluation = minimax(engine, depth - 1, currentAlpha, currentBeta, false, aiPiece)
                engine.undoMove(row, col)

                maxEval = max(maxEval, evaluation)
                currentAlpha = max(currentAlpha, evaluation)
                if (currentBeta <= currentAlpha) break // Poda beta
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (col in COLUMN_SEARCH_ORDER) {
                val row = engine.getLowestAvailableRow(col) ?: continue
                engine.dropPiece(col, opponent)
                val evaluation = minimax(engine, depth - 1, currentAlpha, currentBeta, true, aiPiece)
                engine.undoMove(row, col)

                minEval = min(minEval, evaluation)
                currentBeta = min(currentBeta, evaluation)
                if (currentBeta <= currentAlpha) break // Poda alfa
            }
            return minEval
        }
    }

    /**
     * Función de evaluación estática del tablero basada en control central y ventanas de 4 celdas.
     */
    private fun evaluateBoard(engine: ConnectFourEngine, aiPiece: ConnectFourPiece): Int {
        var score = 0
        val opponent = aiPiece.opponent()

        // 1. Control de la columna central (columna 3)
        var centerCount = 0
        for (r in 0 until ConnectFourEngine.ROWS) {
            if (engine.getCell(r, 3) == aiPiece) {
                centerCount++
            }
        }
        score += centerCount * 6

        // 2. Evaluar ventanas horizontales (6 filas)
        for (r in 0 until ConnectFourEngine.ROWS) {
            for (c in 0..ConnectFourEngine.COLS - ConnectFourEngine.WIN_STREAK) {
                val window = listOf(
                    engine.getCell(r, c),
                    engine.getCell(r, c + 1),
                    engine.getCell(r, c + 2),
                    engine.getCell(r, c + 3)
                )
                score += evaluateWindow(window, aiPiece, opponent)
            }
        }

        // 3. Evaluar ventanas verticales (7 columnas)
        for (c in 0 until ConnectFourEngine.COLS) {
            for (r in 0..ConnectFourEngine.ROWS - ConnectFourEngine.WIN_STREAK) {
                val window = listOf(
                    engine.getCell(r, c),
                    engine.getCell(r + 1, c),
                    engine.getCell(r + 2, c),
                    engine.getCell(r + 3, c)
                )
                score += evaluateWindow(window, aiPiece, opponent)
            }
        }

        // 4. Evaluar ventanas diagonales principales (\)
        for (r in 0..ConnectFourEngine.ROWS - ConnectFourEngine.WIN_STREAK) {
            for (c in 0..ConnectFourEngine.COLS - ConnectFourEngine.WIN_STREAK) {
                val window = listOf(
                    engine.getCell(r, c),
                    engine.getCell(r + 1, c + 1),
                    engine.getCell(r + 2, c + 2),
                    engine.getCell(r + 3, c + 3)
                )
                score += evaluateWindow(window, aiPiece, opponent)
            }
        }

        // 5. Evaluar ventanas diagonales secundarias (/)
        for (r in ConnectFourEngine.WIN_STREAK - 1 until ConnectFourEngine.ROWS) {
            for (c in 0..ConnectFourEngine.COLS - ConnectFourEngine.WIN_STREAK) {
                val window = listOf(
                    engine.getCell(r, c),
                    engine.getCell(r - 1, c + 1),
                    engine.getCell(r - 2, c + 2),
                    engine.getCell(r - 3, c + 3)
                )
                score += evaluateWindow(window, aiPiece, opponent)
            }
        }

        return score
    }

    /**
     * Pondera el valor estratégico de una ventana de 4 casillas adyacentes.
     */
    private fun evaluateWindow(
        window: List<ConnectFourPiece?>,
        aiPiece: ConnectFourPiece,
        opponent: ConnectFourPiece
    ): Int {
        var score = 0
        val aiCount = window.count { it == aiPiece }
        val oppCount = window.count { it == opponent }
        val emptyCount = window.count { it == null }

        if (aiCount == 4) {
            score += 10_000
        } else if (aiCount == 3 && emptyCount == 1) {
            score += 80
        } else if (aiCount == 2 && emptyCount == 2) {
            score += 8
        }

        // Penalizar fuertemente ventanas donde el oponente está a punto de ganar
        if (oppCount == 3 && emptyCount == 1) {
            score -= 120
        }

        return score
    }
}
package com.example.gamehub.games.tictactoe

import com.example.gamehub.games.tictactoe.domain.TicTacToeEngine
import com.example.gamehub.games.tictactoe.domain.ai.AIPlayer
import com.example.gamehub.games.tictactoe.domain.ai.MinimaxAlgorithm
import com.example.gamehub.games.tictactoe.model.CellPosition
import com.example.gamehub.games.tictactoe.model.Difficulty
import com.example.gamehub.games.tictactoe.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Suite de pruebas unitarias para la Inteligencia Artificial: [AIPlayer] y [MinimaxAlgorithm].
 * Valida la capacidad de la IA de aprovechar victorias inmediatas, bloquear amenazas
 * inminentes del rival y comportarse de forma matemáticamente imbatible.
 */
class AIPlayerTest {

    /**
     * Verifica que Minimax elija la jugada ganadora disponible en lugar de movimientos subóptimos.
     */
    @Test
    fun `minimax takes winning move when available`() {
        val engine = TicTacToeEngine()
        // O O _
        // X X _
        // _ _ _
        engine.makeMove(0, 0, Player.O)
        engine.makeMove(1, 0, Player.X)
        engine.makeMove(0, 1, Player.O)
        engine.makeMove(1, 1, Player.X)

        val bestMove = MinimaxAlgorithm.findBestMove(engine, Player.O)
        // La IA debe jugar en (0, 2) para ganar antes de que X gane en (1, 2)
        assertEquals(CellPosition(0, 2), bestMove)
    }

    /**
     * Verifica que Minimax bloquee la victoria del oponente si éste tiene 2 fichas alineadas.
     */
    @Test
    fun `minimax blocks opponent winning move`() {
        val engine = TicTacToeEngine()
        // X X _
        // O _ _
        // _ _ _
        engine.makeMove(0, 0, Player.X)
        engine.makeMove(1, 0, Player.O)
        engine.makeMove(0, 1, Player.X)

        val bestMove = MinimaxAlgorithm.findBestMove(engine, Player.O)
        // La IA debe bloquear en (0, 2)
        assertEquals(CellPosition(0, 2), bestMove)
    }

    /**
     * Simula 50 partidas consecutivas de Minimax contra un jugador que realiza jugadas aleatorias.
     * Minimax nunca debe perder ninguna partida (solo victorias o empates).
     */
    @Test
    fun `minimax never loses against random moves`() {
        for (game in 1..50) {
            val engine = TicTacToeEngine()
            var currentTurn = if (game % 2 == 0) Player.X else Player.O
            val aiPlayer = Player.O
            val randomOpponent = Player.X

            while (!engine.isGameOver()) {
                if (currentTurn == aiPlayer) {
                    val move = MinimaxAlgorithm.findBestMove(engine, aiPlayer)
                    assertNotNull(move)
                    engine.makeMove(move!!, aiPlayer)
                } else {
                    val randomMove = engine.getAvailableCells().random()
                    engine.makeMove(randomMove, randomOpponent)
                }
                currentTurn = currentTurn.opponent()
            }

            val winner = engine.checkWinner()?.winner
            // La IA nunca debe perder
            assertNotEquals("AI should never lose against random player", randomOpponent, winner)
        }
    }

    /**
     * Verifica que la dificultad Media (heurística) detecte y bloquee amenazas inmediatas.
     */
    @Test
    fun `medium AI blocks immediate threat`() {
        val engine = TicTacToeEngine()
        // X X _
        // _ _ _
        // _ _ _
        engine.makeMove(0, 0, Player.X)
        engine.makeMove(0, 1, Player.X)

        val move = AIPlayer.getNextMove(engine, Difficulty.MEDIUM, Player.O)
        assertEquals(CellPosition(0, 2), move)
    }
}

package com.example.gamehub.games.tictactoe

import com.example.gamehub.games.tictactoe.domain.TicTacToeEngine
import com.example.gamehub.games.tictactoe.model.CellPosition
import com.example.gamehub.games.tictactoe.model.Player
import com.example.gamehub.games.tictactoe.model.WinningLineType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de pruebas unitarias para el motor de reglas [TicTacToeEngine].
 * Valida la correcta inicialización del tablero, la detección de victorias en filas,
 * columnas y diagonales, el manejo de empates y la prevención de jugadas en celdas ocupadas.
 */
class TicTacToeEngineTest {

    private lateinit var engine: TicTacToeEngine

    /**
     * Inicializa una instancia limpia del motor antes de cada caso de prueba.
     */
    @Before
    fun setUp() {
        engine = TicTacToeEngine()
    }

    /**
     * Verifica que el tablero inicie con 9 celdas disponibles, sin ganador y sin estar lleno.
     */
    @Test
    fun `test initial board is empty`() {
        assertEquals(9, engine.getAvailableCells().size)
        assertFalse(engine.isBoardFull())
        assertNull(engine.checkWinner())
    }

    /**
     * Verifica la detección de victoria horizontal en la fila 0.
     */
    @Test
    fun `test horizontal win row 0`() {
        engine.makeMove(0, 0, Player.X)
        engine.makeMove(1, 0, Player.O)
        engine.makeMove(0, 1, Player.X)
        engine.makeMove(1, 1, Player.O)
        engine.makeMove(0, 2, Player.X)

        val result = engine.checkWinner()
        assertNotNull(result)
        assertEquals(Player.X, result?.winner)
        assertEquals(WinningLineType.ROW_0, result?.lineType)
    }

    /**
     * Verifica la detección de victoria vertical en la columna 1.
     */
    @Test
    fun `test vertical win column 1`() {
        engine.makeMove(0, 1, Player.O)
        engine.makeMove(0, 0, Player.X)
        engine.makeMove(1, 1, Player.O)
        engine.makeMove(0, 2, Player.X)
        engine.makeMove(2, 1, Player.O)

        val result = engine.checkWinner()
        assertNotNull(result)
        assertEquals(Player.O, result?.winner)
        assertEquals(WinningLineType.COL_1, result?.lineType)
    }

    /**
     * Verifica la detección de victoria en la diagonal principal (0,0)-(1,1)-(2,2).
     */
    @Test
    fun `test diagonal main win`() {
        engine.makeMove(0, 0, Player.X)
        engine.makeMove(0, 1, Player.O)
        engine.makeMove(1, 1, Player.X)
        engine.makeMove(0, 2, Player.O)
        engine.makeMove(2, 2, Player.X)

        val result = engine.checkWinner()
        assertNotNull(result)
        assertEquals(Player.X, result?.winner)
        assertEquals(WinningLineType.DIAGONAL_MAIN, result?.lineType)
    }

    /**
     * Verifica la detección de victoria en la diagonal secundaria (0,2)-(1,1)-(2,0).
     */
    @Test
    fun `test diagonal anti win`() {
        engine.makeMove(0, 2, Player.O)
        engine.makeMove(0, 0, Player.X)
        engine.makeMove(1, 1, Player.O)
        engine.makeMove(0, 1, Player.X)
        engine.makeMove(2, 0, Player.O)

        val result = engine.checkWinner()
        assertNotNull(result)
        assertEquals(Player.O, result?.winner)
        assertEquals(WinningLineType.DIAGONAL_ANTI, result?.lineType)
    }

    /**
     * Verifica la detección correcta de empate cuando las 9 casillas se llenan sin 3 en raya.
     */
    @Test
    fun `test draw game`() {
        // Disposición de empate:
        // X O X
        // X O O
        // O X X
        val moves = listOf(
            Triple(0, 0, Player.X), Triple(0, 1, Player.O), Triple(0, 2, Player.X),
            Triple(1, 0, Player.X), Triple(1, 1, Player.O), Triple(1, 2, Player.O),
            Triple(2, 0, Player.O), Triple(2, 1, Player.X), Triple(2, 2, Player.X)
        )
        for ((r, c, p) in moves) {
            engine.makeMove(r, c, p)
        }

        assertTrue(engine.isBoardFull())
        assertNull(engine.checkWinner())
        assertTrue(engine.isGameOver())
    }

    /**
     * Verifica que intentar mover sobre una celda ya ocupada retorne false y no sobreescriba.
     */
    @Test
    fun `test move on already occupied cell returns false`() {
        assertTrue(engine.makeMove(0, 0, Player.X))
        assertFalse(engine.makeMove(0, 0, Player.O))
        assertEquals(Player.X, engine.getCell(0, 0))
    }
}

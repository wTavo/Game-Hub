package com.example.gamehub.games.connectfour

import com.example.gamehub.games.connectfour.domain.ConnectFourEngine
import com.example.gamehub.games.connectfour.model.ConnectFourCell
import com.example.gamehub.games.connectfour.model.ConnectFourLineType
import com.example.gamehub.games.connectfour.model.ConnectFourPiece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de pruebas unitarias para el motor de reglas [ConnectFourEngine].
 * Valida la física de gravedad en columnas, la detección de 4 en raya en todas las direcciones,
 * el manejo de columnas saturadas y la detección de empates.
 */
class ConnectFourEngineTest {

    private lateinit var engine: ConnectFourEngine

    @Before
    fun setUp() {
        engine = ConnectFourEngine()
    }

    @Test
    fun `test initial board is empty and all columns valid`() {
        assertEquals(7, engine.getValidColumns().size)
        assertFalse(engine.isBoardFull())
        assertNull(engine.checkWinner())
        for (c in 0 until ConnectFourEngine.COLS) {
            assertEquals(5, engine.getLowestAvailableRow(c))
        }
    }

    @Test
    fun `test pieces fall by gravity and stack from bottom to top`() {
        val cell1 = engine.dropPiece(3, ConnectFourPiece.YELLOW)
        assertEquals(ConnectFourCell(5, 3), cell1)
        assertEquals(ConnectFourPiece.YELLOW, engine.getCell(5, 3))

        val cell2 = engine.dropPiece(3, ConnectFourPiece.RED)
        assertEquals(ConnectFourCell(4, 3), cell2)
        assertEquals(ConnectFourPiece.RED, engine.getCell(4, 3))

        val cell3 = engine.dropPiece(3, ConnectFourPiece.YELLOW)
        assertEquals(ConnectFourCell(3, 3), cell3)
        assertEquals(ConnectFourPiece.YELLOW, engine.getCell(3, 3))
    }

    @Test
    fun `test full column rejects further moves`() {
        // Llenar columna 0 con 6 fichas
        for (i in 0 until ConnectFourEngine.ROWS) {
            val cell = engine.dropPiece(0, ConnectFourPiece.YELLOW)
            assertNotNull(cell)
        }
        assertNull(engine.getLowestAvailableRow(0))
        assertNull(engine.dropPiece(0, ConnectFourPiece.RED))
        assertFalse(engine.getValidColumns().contains(0))
    }

    @Test
    fun `test horizontal win on bottom row`() {
        engine.dropPiece(0, ConnectFourPiece.YELLOW) // (5, 0)
        engine.dropPiece(0, ConnectFourPiece.RED)    // (4, 0)
        engine.dropPiece(1, ConnectFourPiece.YELLOW) // (5, 1)
        engine.dropPiece(1, ConnectFourPiece.RED)    // (4, 1)
        engine.dropPiece(2, ConnectFourPiece.YELLOW) // (5, 2)
        engine.dropPiece(2, ConnectFourPiece.RED)    // (4, 2)
        engine.dropPiece(3, ConnectFourPiece.YELLOW) // (5, 3) 4 en raya!

        val win = engine.checkWinner()
        assertNotNull(win)
        assertEquals(ConnectFourPiece.YELLOW, win?.winner)
        assertEquals(ConnectFourLineType.HORIZONTAL, win?.lineType)
        assertEquals(4, win?.winningCells?.size)
    }

    @Test
    fun `test vertical win on column 4`() {
        engine.dropPiece(4, ConnectFourPiece.RED) // (5, 4)
        engine.dropPiece(0, ConnectFourPiece.YELLOW)
        engine.dropPiece(4, ConnectFourPiece.RED) // (4, 4)
        engine.dropPiece(0, ConnectFourPiece.YELLOW)
        engine.dropPiece(4, ConnectFourPiece.RED) // (3, 4)
        engine.dropPiece(0, ConnectFourPiece.YELLOW)
        engine.dropPiece(4, ConnectFourPiece.RED) // (2, 4) 4 en raya vertical!

        val win = engine.checkWinner()
        assertNotNull(win)
        assertEquals(ConnectFourPiece.RED, win?.winner)
        assertEquals(ConnectFourLineType.VERTICAL, win?.lineType)
    }

    @Test
    fun `test diagonal main win`() {
        // Formar diagonal descendente (\) de (2, 0) a (5, 3)
        // Col 0: 4 fichas (5, 4, 3, 2=Y)
        engine.dropPiece(0, ConnectFourPiece.RED)
        engine.dropPiece(0, ConnectFourPiece.RED)
        engine.dropPiece(0, ConnectFourPiece.RED)
        engine.dropPiece(0, ConnectFourPiece.YELLOW) // (2, 0)

        // Col 1: 3 fichas (5, 4, 3=Y)
        engine.dropPiece(1, ConnectFourPiece.RED)
        engine.dropPiece(1, ConnectFourPiece.RED)
        engine.dropPiece(1, ConnectFourPiece.YELLOW) // (3, 1)

        // Col 2: 2 fichas (5, 4=Y)
        engine.dropPiece(2, ConnectFourPiece.RED)
        engine.dropPiece(2, ConnectFourPiece.YELLOW) // (4, 2)

        // Col 3: 1 ficha (5=Y)
        engine.dropPiece(3, ConnectFourPiece.YELLOW) // (5, 3)

        val win = engine.checkWinner()
        assertNotNull(win)
        assertEquals(ConnectFourPiece.YELLOW, win?.winner)
        assertEquals(ConnectFourLineType.DIAGONAL_MAIN, win?.lineType)
    }

    @Test
    fun `test diagonal anti win`() {
        // Formar diagonal ascendente (/) de (5, 0) a (2, 3)
        // Col 0: (5=R)
        engine.dropPiece(0, ConnectFourPiece.RED) // (5, 0)

        // Col 1: (5=Y, 4=R)
        engine.dropPiece(1, ConnectFourPiece.YELLOW)
        engine.dropPiece(1, ConnectFourPiece.RED) // (4, 1)

        // Col 2: (5=Y, 4=Y, 3=R)
        engine.dropPiece(2, ConnectFourPiece.YELLOW)
        engine.dropPiece(2, ConnectFourPiece.YELLOW)
        engine.dropPiece(2, ConnectFourPiece.RED) // (3, 2)

        // Col 3: (5=Y, 4=Y, 3=Y, 2=R)
        engine.dropPiece(3, ConnectFourPiece.YELLOW)
        engine.dropPiece(3, ConnectFourPiece.YELLOW)
        engine.dropPiece(3, ConnectFourPiece.YELLOW)
        engine.dropPiece(3, ConnectFourPiece.RED) // (2, 3)

        val win = engine.checkWinner()
        assertNotNull(win)
        assertEquals(ConnectFourPiece.RED, win?.winner)
        assertEquals(ConnectFourLineType.DIAGONAL_ANTI, win?.lineType)
    }
}
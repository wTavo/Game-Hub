package com.example.gamehub.games.connectfour

import com.example.gamehub.games.connectfour.domain.ConnectFourEngine
import com.example.gamehub.games.connectfour.domain.ai.ConnectFourAI
import com.example.gamehub.games.connectfour.model.ConnectFourDifficulty
import com.example.gamehub.games.connectfour.model.ConnectFourPiece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Suite de pruebas unitarias para la IA de Conecta 4 [ConnectFourAI].
 */
class ConnectFourAITest {

    @Test
    fun `test AI takes winning move immediately`() {
        val engine = ConnectFourEngine()
        // Crear 3 fichas consecutivas de la IA en la fila inferior: col 0, 1, 2
        engine.dropPiece(0, ConnectFourPiece.RED)
        engine.dropPiece(1, ConnectFourPiece.RED)
        engine.dropPiece(2, ConnectFourPiece.RED)

        // La IA debe elegir col 3 para completar 4 en raya
        val move = ConnectFourAI.getNextMove(engine, ConnectFourDifficulty.MEDIUM, ConnectFourPiece.RED)
        assertEquals(3, move)
    }

    @Test
    fun `test AI blocks opponent winning move`() {
        val engine = ConnectFourEngine()
        // El oponente (YELLOW) tiene 3 en columna 0, 1, 2
        engine.dropPiece(0, ConnectFourPiece.YELLOW)
        engine.dropPiece(1, ConnectFourPiece.YELLOW)
        engine.dropPiece(2, ConnectFourPiece.YELLOW)

        // La IA (RED) debe bloquear en col 3
        val move = ConnectFourAI.getNextMove(engine, ConnectFourDifficulty.MEDIUM, ConnectFourPiece.RED)
        assertEquals(3, move)
    }
}
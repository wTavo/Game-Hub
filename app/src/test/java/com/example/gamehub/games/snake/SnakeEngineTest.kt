package com.example.gamehub.games.snake

import com.example.gamehub.games.snake.domain.SnakeEngine
import com.example.gamehub.games.snake.model.SnakeDirection
import com.example.gamehub.games.snake.model.SnakeFood
import com.example.gamehub.games.snake.model.SnakeGameMode
import com.example.gamehub.games.snake.model.SnakePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SnakeEngineTest {

    private lateinit var engine: SnakeEngine

    @Before
    fun setUp() {
        engine = SnakeEngine(gridWidth = 10, gridHeight = 10)
        engine.food = SnakeFood(SnakePosition(0, 0), isGolden = false)
    }

    @Test
    fun testInitialization() {
        assertEquals(3, engine.snake.size)
        assertEquals(SnakePosition(5, 5), engine.snake[0]) // Cabeza
        assertEquals(SnakePosition(4, 5), engine.snake[1])
        assertEquals(SnakePosition(3, 5), engine.snake[2]) // Cola
        assertFalse(engine.isGameOver)
        assertEquals(0, engine.score)
    }

    @Test
    fun testStepMoveForward() {
        engine.food = SnakeFood(SnakePosition(0, 0), isGolden = false)
        val result = engine.step()
        assertEquals(SnakeEngine.StepResult.MOVED, result)
        assertEquals(SnakePosition(6, 5), engine.snake[0])
        assertEquals(3, engine.snake.size)
    }

    @Test
    fun testDirectionChangeAndRejectionOfOpposites() {
        // Inicialmente va hacia RIGHT. Girar a LEFT debe ser rechazado.
        val oppositeRejected = engine.changeDirection(SnakeDirection.LEFT)
        assertFalse(oppositeRejected)

        // Girar a UP debe ser aceptado
        val turnUpAccepted = engine.changeDirection(SnakeDirection.UP)
        assertTrue(turnUpAccepted)

        engine.step()
        assertEquals(SnakePosition(5, 4), engine.snake[0])
    }

    @Test
    fun testClassicWallCollisionCausesGameOver() {
        engine.reset(SnakeGameMode.CLASSIC_WALLS)
        // Mover a la derecha hasta chocar con la pared (ancho 10, empieza en x=5)
        engine.step() // x=6
        engine.step() // x=7
        engine.step() // x=8
        engine.step() // x=9
        val crashResult = engine.step() // x=10 -> Colisión

        assertEquals(SnakeEngine.StepResult.CRASHED, crashResult)
        assertTrue(engine.isGameOver)
    }

    @Test
    fun testFreeWrapAroundWalls() {
        engine.reset(SnakeGameMode.FREE_WRAP)
        engine.step() // x=6
        engine.step() // x=7
        engine.step() // x=8
        engine.step() // x=9
        val wrapResult = engine.step() // Sale por x=9 y entra por x=0

        assertEquals(SnakeEngine.StepResult.MOVED, wrapResult)
        assertEquals(SnakePosition(0, 5), engine.snake[0])
        assertFalse(engine.isGameOver)
    }

    @Test
    fun testSelfCollision() {
        engine.reset(SnakeGameMode.CLASSIC_WALLS)
        // Crecer la serpiente agregando segmentos
        val snakeList = engine.snake as MutableList<SnakePosition>
        snakeList.add(SnakePosition(2, 5))
        snakeList.add(SnakePosition(1, 5))
        snakeList.add(SnakePosition(0, 5))

        // Ahora la serpiente tiene longitud 6: (5,5), (4,5), (3,5), (2,5), (1,5), (0,5)
        // UP -> (5,4)
        engine.changeDirection(SnakeDirection.UP)
        engine.step()
        // LEFT -> (4,4)
        engine.changeDirection(SnakeDirection.LEFT)
        engine.step()
        // DOWN -> (4,5) colisiona con el segmento que está en (4,5)
        engine.changeDirection(SnakeDirection.DOWN)
        val crash = engine.step()

        assertEquals(SnakeEngine.StepResult.CRASHED, crash)
        assertTrue(engine.isGameOver)
    }

    @Test
    fun testCircularInputBufferQueue() {
        engine.reset(SnakeGameMode.CLASSIC_WALLS)
        // Inicialmente va hacia RIGHT en (5,5)
        // Enviar secuencia rápida de círculo: UP -> LEFT -> DOWN -> RIGHT antes de los steps
        engine.changeDirection(SnakeDirection.UP)
        engine.changeDirection(SnakeDirection.LEFT)
        engine.changeDirection(SnakeDirection.DOWN)

        // Paso 1: aplica UP -> (5,4)
        engine.step()
        assertEquals(SnakePosition(5, 4), engine.snake[0])

        // Paso 2: aplica LEFT -> (4,4)
        engine.step()
        assertEquals(SnakePosition(4, 4), engine.snake[0])

        // Paso 3: aplica DOWN -> (4,5)
        engine.step()
        assertEquals(SnakePosition(4, 5), engine.snake[0])
    }
}
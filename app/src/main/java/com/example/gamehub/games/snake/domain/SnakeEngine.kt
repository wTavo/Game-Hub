package com.example.gamehub.games.snake.domain

import com.example.gamehub.games.snake.model.SnakeDirection
import com.example.gamehub.games.snake.model.SnakeFood
import com.example.gamehub.games.snake.model.SnakeGameMode
import com.example.gamehub.games.snake.model.SnakePosition
import kotlin.random.Random

/**
 * Motor de reglas y física discreta para el juego Snake (Gusanito).
 * Administra el movimiento sobre la cuadrícula con cola de entrada bufferizada (Input Buffer FIFO),
 * permitiendo giros rápidos fluidos y movimientos circulares completos sin pérdida de comandos.
 */
class SnakeEngine(
    val gridWidth: Int = DEFAULT_GRID_WIDTH,
    val gridHeight: Int = DEFAULT_GRID_HEIGHT
) {

    companion object {
        const val DEFAULT_GRID_WIDTH = 20
        const val DEFAULT_GRID_HEIGHT = 20
        private const val INITIAL_SNAKE_LENGTH = 3
        private const val MAX_INPUT_BUFFER_SIZE = 3
    }

    /**
     * Resultado de cada iteración del bucle de avance (Tick).
     */
    enum class StepResult {
        MOVED,
        ATE_FOOD,
        ATE_GOLDEN_FOOD,
        CRASHED
    }

    // Estructuras de datos del juego
    private val _snake: MutableList<SnakePosition> = mutableListOf()
    val snake: List<SnakePosition> get() = _snake

    var currentDirection: SnakeDirection = SnakeDirection.RIGHT
        private set

    // Cola FIFO de direcciones para retención de comandos rápidos (giros en círculo)
    private val inputQueue: ArrayDeque<SnakeDirection> = ArrayDeque()

    var food: SnakeFood? = null
        internal set

    var score: Int = 0
        private set
    var applesEaten: Int = 0
        private set

    var mode: SnakeGameMode = SnakeGameMode.CLASSIC_WALLS
    var isGameOver: Boolean = false
        private set

    init {
        reset(SnakeGameMode.CLASSIC_WALLS)
    }

    /**
     * Reinicia el tablero, centrando al gusanito con longitud inicial hacia la derecha.
     */
    fun reset(gameMode: SnakeGameMode = this.mode) {
        mode = gameMode
        isGameOver = false
        score = 0
        applesEaten = 0
        currentDirection = SnakeDirection.RIGHT
        inputQueue.clear()

        _snake.clear()
        val startX = gridWidth / 2
        val startY = gridHeight / 2

        for (i in 0 until INITIAL_SNAKE_LENGTH) {
            _snake.add(SnakePosition(startX - i, startY))
        }

        spawnFood()
    }

    /**
     * Encola un nuevo cambio de dirección en el búfer de entrada.
     * Evalúa la coherencia respecto al último comando en cola para permitir secuencias rápidas (como círculos).
     */
    fun changeDirection(newDir: SnakeDirection): Boolean {
        if (isGameOver) return false

        // Comparar contra el último giro en cola (o contra la dirección actual si la cola está vacía)
        val referenceDir = inputQueue.lastOrNull() ?: currentDirection

        if (newDir == referenceDir || newDir.isOppositeTo(referenceDir)) {
            return false
        }

        if (inputQueue.size < MAX_INPUT_BUFFER_SIZE) {
            inputQueue.add(newDir)
        } else {
            // Reemplazar el último para no perder el comando más reciente
            inputQueue.removeLast()
            inputQueue.add(newDir)
        }
        return true
    }

    /**
     * Ejecuta un avance de 1 casilla (Tick) en la simulación del gusanito.
     */
    fun step(): StepResult {
        if (isGameOver) return StepResult.CRASHED

        // Extraer el siguiente giro pendiente en el búfer
        if (inputQueue.isNotEmpty()) {
            currentDirection = inputQueue.removeFirst()
        }

        val head = _snake.first()

        var nextX = when (currentDirection) {
            SnakeDirection.UP -> head.x
            SnakeDirection.DOWN -> head.x
            SnakeDirection.LEFT -> head.x - 1
            SnakeDirection.RIGHT -> head.x + 1
        }

        var nextY = when (currentDirection) {
            SnakeDirection.UP -> head.y - 1
            SnakeDirection.DOWN -> head.y + 1
            SnakeDirection.LEFT -> head.y
            SnakeDirection.RIGHT -> head.y
        }

        // 1. Verificación de colisiones con los bordes
        if (mode == SnakeGameMode.CLASSIC_WALLS) {
            if (nextX < 0 || nextX >= gridWidth || nextY < 0 || nextY >= gridHeight) {
                isGameOver = true
                return StepResult.CRASHED
            }
        } else {
            // Modo Envolvente (Wrap around)
            nextX = (nextX + gridWidth) % gridWidth
            nextY = (nextY + gridHeight) % gridHeight
        }

        val newHead = SnakePosition(nextX, nextY)

        // 2. Verificación de colisiones contra el propio cuerpo
        val willEat = food != null && food?.position == newHead
        val bodyToCheck = if (willEat) _snake else _snake.dropLast(1)
        if (bodyToCheck.contains(newHead)) {
            isGameOver = true
            return StepResult.CRASHED
        }

        // 3. Inserción de la nueva cabeza
        _snake.add(0, newHead)

        // 4. Ingesta de alimentos
        return if (willEat) {
            val eatenFood = food!!
            score += eatenFood.points
            applesEaten++
            spawnFood()
            if (eatenFood.isGolden) StepResult.ATE_GOLDEN_FOOD else StepResult.ATE_FOOD
        } else {
            _snake.removeAt(_snake.lastIndex)
            StepResult.MOVED
        }
    }

    /**
     * Genera un alimento en una posición libre aleatoria de la cuadrícula.
     */
    fun spawnFood() {
        val occupied = _snake.toSet()
        val freeCells = mutableListOf<SnakePosition>()

        for (x in 0 until gridWidth) {
            for (y in 0 until gridHeight) {
                val pos = SnakePosition(x, y)
                if (pos !in occupied) {
                    freeCells.add(pos)
                }
            }
        }

        if (freeCells.isNotEmpty()) {
            val randomPos = freeCells[Random.nextInt(freeCells.size)]
            val isGolden = Random.nextInt(100) < 18 // 18% probabilidad de manzana dorada bonus
            food = SnakeFood(position = randomPos, isGolden = isGolden)
        } else {
            food = null // Victoria total: tablero completamente lleno
        }
    }

    /**
     * Restaura el estado interno completo del motor tras una rotación de pantalla.
     */
    fun restoreState(
        snakeSegments: List<SnakePosition>,
        direction: SnakeDirection,
        currentFood: SnakeFood?,
        currentScore: Int,
        apples: Int,
        gameMode: SnakeGameMode,
        gameOver: Boolean
    ) {
        _snake.clear()
        _snake.addAll(snakeSegments)
        currentDirection = direction
        inputQueue.clear()
        food = currentFood
        score = currentScore
        applesEaten = apples
        mode = gameMode
        isGameOver = gameOver
    }
}
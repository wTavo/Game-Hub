package com.example.gamehub.games.snake.model

/**
 * Representa una coordenada discreta (x, y) en la cuadrícula del tablero de Snake.
 * @param x Columna horizontal (0 .. COLS - 1)
 * @param y Fila vertical (0 .. ROWS - 1)
 */
data class SnakePosition(
    val x: Int,
    val y: Int
)
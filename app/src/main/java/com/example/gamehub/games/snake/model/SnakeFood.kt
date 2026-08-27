package com.example.gamehub.games.snake.model

/**
 * Representa un alimento generado en el tablero.
 * @param position Coordenada de la cuadrícula.
 * @param isGolden true si es una manzana dorada bonus de alta puntuación.
 * @param points Puntos que otorga al ser comida (10 regular, 30 dorada).
 */
data class SnakeFood(
    val position: SnakePosition,
    val isGolden: Boolean = false,
    val points: Int = if (isGolden) 30 else 10
)
package com.example.gamehub.games.snake.model

/**
 * Modos de juego de Snake respecto a los límites del mapa.
 */
enum class SnakeGameMode {
    CLASSIC_WALLS, // Chocar con las paredes causa Game Over
    FREE_WRAP      // Atravesar un borde te hace reaparecer en el opuesto
}
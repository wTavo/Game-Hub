package com.example.gamehub.games.snake.model

/**
 * Estados del ciclo de vida del juego Snake.
 */
sealed class SnakeGameState {
    object Idle : SnakeGameState()
    object Running : SnakeGameState()
    object Paused : SnakeGameState()
    data class GameOver(val finalScore: Int, val isNewRecord: Boolean) : SnakeGameState()
}
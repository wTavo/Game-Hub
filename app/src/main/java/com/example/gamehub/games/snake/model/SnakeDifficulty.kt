package com.example.gamehub.games.snake.model

/**
 * Niveles de dificultad y velocidad de avance del gusanito.
 * @param tickDelayMs Intervalo en milisegundos entre cada avance de casilla.
 */
enum class SnakeDifficulty(val tickDelayMs: Long) {
    EASY(tickDelayMs = 210L),
    MEDIUM(tickDelayMs = 145L),
    HARD(tickDelayMs = 90L)
}
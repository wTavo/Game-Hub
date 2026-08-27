package com.example.gamehub.games.tictactoe.model

/**
 * Niveles de dificultad para las partidas contra la Inteligencia Artificial:
 * - [EASY]: Movimientos aleatorios y relajados.
 * - [MEDIUM]: Heurística reactiva con capacidad de ataque y bloqueo.
 * - [HARD]: Algoritmo Minimax matemáticamente imbatible.
 */
enum class Difficulty {
    EASY,
    MEDIUM,
    HARD
}

package com.example.gamehub.games.tictactoe.model

/**
 * Jerarquía de estados posibles para el ciclo de vida de una partida:
 * - [Idle]: En espera de iniciar antes del primer movimiento.
 * - [Playing]: Partida activa con el turno de [currentTurn].
 * - [Won]: Partida finalizada con un ganador detallado en [winningResult].
 * - [Draw]: Partida finalizada en empate sin movimientos restantes.
 */
sealed class GameState {
    /**
     * Estado inicial o en reposo, listo para configurar modo/dificultad.
     */
    object Idle : GameState()

    /**
     * Estado de juego activo.
     *
     * @property currentTurn Jugador a quien le corresponde jugar en este momento.
     */
    data class Playing(val currentTurn: Player) : GameState()

    /**
     * Estado de victoria.
     *
     * @property winningResult Información de la línea ganadora y celdas correspondientes.
     */
    data class Won(val winningResult: WinningResult) : GameState()

    /**
     * Estado de empate por tablero completo sin combinación ganadora.
     */
    object Draw : GameState()
}

package com.example.gamehub.games.tictactoe.model

/**
 * Representa la preferencia del jugador para elegir su ficha de juego o sortearla al azar.
 */
enum class PlayerSymbolChoice {
    /**
     * El jugador elige jugar con la ficha X (inicia la partida).
     */
    X,

    /**
     * El jugador elige jugar con la ficha O (la CPU/Jugador 2 inicia con X).
     */
    O,

    /**
     * La ficha inicial y el turno se sortean de manera aleatoria al inicio de cada partida.
     */
    RANDOM
}

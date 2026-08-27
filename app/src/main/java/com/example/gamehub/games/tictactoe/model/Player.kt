package com.example.gamehub.games.tictactoe.model

/**
 * Representa a los dos jugadores posibles en una partida de Tres en Raya:
 * [X] (primer turno, color Cyan) y [O] (segundo turno / CPU, color Coral).
 *
 * @property symbol Carácter o símbolo visual de la ficha ("X" u "O").
 */
enum class Player(val symbol: String) {
    X("X"),
    O("O");

    /**
     * Retorna el jugador oponente al actual.
     * Si es X retorna O; si es O retorna X.
     */
    fun opponent(): Player = if (this == X) O else X
}

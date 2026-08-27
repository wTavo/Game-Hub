package com.example.gamehub.games.connectfour.model

/**
 * Representa el color de la ficha en el juego Conecta 4.
 */
enum class ConnectFourPiece(val symbol: String) {
    RED("R"),
    YELLOW("Y");

    /**
     * Retorna la ficha del oponente.
     */
    fun opponent(): ConnectFourPiece = when (this) {
        RED -> YELLOW
        YELLOW -> RED
    }
}
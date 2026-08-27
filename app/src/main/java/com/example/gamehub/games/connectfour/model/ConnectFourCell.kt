package com.example.gamehub.games.connectfour.model

/**
 * Coordenadas de una celda en el tablero de Conecta 4.
 * @param row Fila de 0 (superior) a 5 (inferior).
 * @param col Columna de 0 (izquierda) a 6 (derecha).
 */
data class ConnectFourCell(val row: Int, val col: Int)
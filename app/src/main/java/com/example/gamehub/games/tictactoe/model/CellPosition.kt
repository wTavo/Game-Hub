package com.example.gamehub.games.tictactoe.model

/**
 * Coordenadas bidimensionales de una casilla dentro del tablero de 3x3.
 *
 * @property row Índice de fila (0, 1 o 2).
 * @property col Índice de columna (0, 1 o 2).
 */
data class CellPosition(
    val row: Int,
    val col: Int
) {
    /**
     * Índice lineal (0 a 8) correspondiente a la posición en el grid.
     */
    val index: Int get() = row * 3 + col

    companion object {
        /**
         * Crea una [CellPosition] a partir de un índice lineal de 0 a 8.
         *
         * @param index Índice entre 0 y 8.
         */
        fun fromIndex(index: Int): CellPosition {
            return CellPosition(index / 3, index % 3)
        }
    }
}

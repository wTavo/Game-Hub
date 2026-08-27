package com.example.gamehub.games.tictactoe.model

/**
 * Identifica el patrón o línea en el tablero que formó el tres en raya:
 * - Filas: [ROW_0], [ROW_1], [ROW_2]
 * - Columnas: [COL_0], [COL_1], [COL_2]
 * - Diagonales: [DIAGONAL_MAIN] (0,0)-(1,1)-(2,2) y [DIAGONAL_ANTI] (0,2)-(1,1)-(2,0)
 */
enum class WinningLineType {
    ROW_0, ROW_1, ROW_2,
    COL_0, COL_1, COL_2,
    DIAGONAL_MAIN,
    DIAGONAL_ANTI
}

/**
 * Representa el resultado completo de una victoria en Tres en Raya.
 *
 * @property winner Jugador ganador ([Player.X] o [Player.O]).
 * @property lineType Patrón de la línea ganadora ([WinningLineType]).
 * @property winningCells Lista de las 3 posiciones ([CellPosition]) que formaron el tres en raya.
 */
data class WinningResult(
    val winner: Player,
    val lineType: WinningLineType,
    val winningCells: List<CellPosition>
)

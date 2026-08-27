package com.example.gamehub.games.connectfour.model

/**
 * Tipo de alineación que produjo la victoria en Conecta 4.
 */
enum class ConnectFourLineType {
    HORIZONTAL,
    VERTICAL,
    DIAGONAL_MAIN,  // Diagonal descendente (\)
    DIAGONAL_ANTI   // Diagonal ascendente (/)
}

/**
 * Resultado de fin de juego por victoria.
 * Contiene la ficha ganadora, las 4 celdas alineadas y el tipo de línea.
 */
data class ConnectFourWinningResult(
    val winner: ConnectFourPiece,
    val winningCells: List<ConnectFourCell>,
    val lineType: ConnectFourLineType
)
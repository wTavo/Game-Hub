package com.example.gamehub.games.tictactoe.domain

import com.example.gamehub.games.tictactoe.model.CellPosition
import com.example.gamehub.games.tictactoe.model.Player
import com.example.gamehub.games.tictactoe.model.WinningLineType
import com.example.gamehub.games.tictactoe.model.WinningResult

/**
 * Motor de lógica pura para el juego Tres en Raya.
 * Mantiene la representación del tablero 3x3 en memoria y ofrece métodos para
 * ejecutar movimientos, comprobar condiciones de victoria o empate y clonar estados.
 */
class TicTacToeEngine {

    /**
     * Matriz de 3x3 que representa las 9 celdas del tablero.
     * null indica una celda vacía; Player.X o Player.O indica ocupación.
     */
    val board: Array<Array<Player?>> = Array(3) { Array(3) { null } }

    /**
     * Intenta colocar la ficha de un jugador en la posición (row, col) dada.
     *
     * @param row Índice de fila (0 a 2).
     * @param col Índice de columna (0 a 2).
     * @param player Jugador que realiza el movimiento (X u O).
     * @return true si el movimiento fue válido y se ejecutó, false si la celda estaba ocupada o fuera de límites.
     */
    fun makeMove(row: Int, col: Int, player: Player): Boolean {
        if (row !in 0..2 || col !in 0..2) return false
        if (board[row][col] != null) return false
        board[row][col] = player
        return true
    }

    /**
     * Sobrecarga de makeMove que recibe una posición empaquetada como [CellPosition].
     *
     * @param position Coordenadas de la celda.
     * @param player Jugador que realiza el movimiento.
     * @return true si el movimiento fue exitoso, false en caso contrario.
     */
    fun makeMove(position: CellPosition, player: Player): Boolean {
        return makeMove(position.row, position.col, player)
    }

    /**
     * Obtiene el ocupante de la casilla en las coordenadas dadas.
     *
     * @param row Índice de fila (0 a 2).
     * @param col Índice de columna (0 a 2).
     * @return [Player] si la casilla está ocupada, o null si está vacía o fuera de rango.
     */
    fun getCell(row: Int, col: Int): Player? {
        if (row !in 0..2 || col !in 0..2) return null
        return board[row][col]
    }

    /**
     * Retorna la lista de todas las posiciones desocupadas actualmente en el tablero.
     *
     * @return Lista de [CellPosition] disponibles para jugar.
     */
    fun getAvailableCells(): List<CellPosition> {
        val list = mutableListOf<CellPosition>()
        for (r in 0..2) {
            for (c in 0..2) {
                if (board[r][c] == null) {
                    list.add(CellPosition(r, c))
                }
            }
        }
        return list
    }

    /**
     * Verifica si el tablero no tiene celdas disponibles para jugar.
     *
     * @return true si las 9 celdas están ocupadas, false en caso contrario.
     */
    fun isBoardFull(): Boolean {
        return getAvailableCells().isEmpty()
    }

    /**
     * Evalúa las 8 líneas posibles (3 filas, 3 columnas y 2 diagonales) para determinar
     * si existe un ganador.
     *
     * @return [WinningResult] con el jugador ganador, el tipo de línea y las 3 celdas ganadoras;
     *         o null si aún no hay ganador.
     */
    fun checkWinner(): WinningResult? {
        // 1. Verificar las 3 Filas
        for (r in 0..2) {
            val p = board[r][0]
            if (p != null && p == board[r][1] && p == board[r][2]) {
                val lineType = when (r) {
                    0 -> WinningLineType.ROW_0
                    1 -> WinningLineType.ROW_1
                    else -> WinningLineType.ROW_2
                }
                return WinningResult(
                    winner = p,
                    lineType = lineType,
                    winningCells = listOf(
                        CellPosition(r, 0),
                        CellPosition(r, 1),
                        CellPosition(r, 2)
                    )
                )
            }
        }

        // 2. Verificar las 3 Columnas
        for (c in 0..2) {
            val p = board[0][c]
            if (p != null && p == board[1][c] && p == board[2][c]) {
                val lineType = when (c) {
                    0 -> WinningLineType.COL_0
                    1 -> WinningLineType.COL_1
                    else -> WinningLineType.COL_2
                }
                return WinningResult(
                    winner = p,
                    lineType = lineType,
                    winningCells = listOf(
                        CellPosition(0, c),
                        CellPosition(1, c),
                        CellPosition(2, c)
                    )
                )
            }
        }

        // 3. Diagonal Principal (0,0), (1,1), (2,2)
        val diagMain = board[0][0]
        if (diagMain != null && diagMain == board[1][1] && diagMain == board[2][2]) {
            return WinningResult(
                winner = diagMain,
                lineType = WinningLineType.DIAGONAL_MAIN,
                winningCells = listOf(
                    CellPosition(0, 0),
                    CellPosition(1, 1),
                    CellPosition(2, 2)
                )
            )
        }

        // 4. Diagonal Secundaria (0,2), (1,1), (2,0)
        val diagAnti = board[0][2]
        if (diagAnti != null && diagAnti == board[1][1] && diagAnti == board[2][0]) {
            return WinningResult(
                winner = diagAnti,
                lineType = WinningLineType.DIAGONAL_ANTI,
                winningCells = listOf(
                    CellPosition(0, 2),
                    CellPosition(1, 1),
                    CellPosition(2, 0)
                )
            )
        }

        return null
    }

    /**
     * Comprueba si la partida actual ha finalizado por victoria o por empate (tablero lleno).
     *
     * @return true si el juego terminó, false si aún se pueden hacer movimientos.
     */
    fun isGameOver(): Boolean {
        return checkWinner() != null || isBoardFull()
    }

    /**
     * Limpia el tablero restableciendo todas las celdas a null (vacías).
     */
    fun reset() {
        for (r in 0..2) {
            for (c in 0..2) {
                board[r][c] = null
            }
        }
    }

    /**
     * Crea y retorna una copia profunda independiente de este motor y su estado de tablero.
     *
     * @return Nueva instancia de [TicTacToeEngine] con el mismo estado de celdas.
     */
    fun copy(): TicTacToeEngine {
        val copy = TicTacToeEngine()
        for (r in 0..2) {
            for (c in 0..2) {
                copy.board[r][c] = this.board[r][c]
            }
        }
        return copy
    }
}

package com.example.gamehub.games.connectfour.domain

import com.example.gamehub.games.connectfour.model.ConnectFourCell
import com.example.gamehub.games.connectfour.model.ConnectFourLineType
import com.example.gamehub.games.connectfour.model.ConnectFourPiece
import com.example.gamehub.games.connectfour.model.ConnectFourWinningResult

/**
 * Motor de reglas puramente matemático para el juego Conecta 4.
 * Gestiona la matriz bidimensional de 6 filas x 7 columnas, la física de gravedad
 * en caída por columna, y la detección algorítmica de 4 en raya en tiempo lineal.
 */
class ConnectFourEngine {

    companion object {
        const val ROWS = 6
        const val COLS = 7
        const val WIN_STREAK = 4
    }

    // Matriz 6x7 interna (indexada: row 0 es arriba, row 5 es abajo en el fondo)
    private val board: Array<Array<ConnectFourPiece?>> = Array(ROWS) { arrayOfNulls(COLS) }

    /**
     * Retorna el contenido actual de la celda especificada.
     */
    fun getCell(row: Int, col: Int): ConnectFourPiece? {
        if (row !in 0 until ROWS || col !in 0 until COLS) return null
        return board[row][col]
    }

    /**
     * Retorna la fila más baja desocupada en la columna indicada (física de gravedad).
     * Si la columna está llena, retorna null.
     */
    fun getLowestAvailableRow(col: Int): Int? {
        if (col !in 0 until COLS) return null
        for (r in ROWS - 1 downTo 0) {
            if (board[r][col] == null) {
                return r
            }
        }
        return null
    }

    /**
     * Inserta una ficha en la columna especificada cayendo por gravedad al fondo.
     * @return La coordenada [ConnectFourCell] donde aterrizó la ficha, o null si la columna estaba llena.
     */
    fun dropPiece(col: Int, piece: ConnectFourPiece): ConnectFourCell? {
        val targetRow = getLowestAvailableRow(col) ?: return null
        board[targetRow][col] = piece
        return ConnectFourCell(targetRow, col)
    }

    /**
     * Establece directamente el ocupante de una celda (usado para restaurar el tablero tras rotaciones).
     */
    fun setCell(row: Int, col: Int, piece: ConnectFourPiece?) {
        if (row in 0 until ROWS && col in 0 until COLS) {
            board[row][col] = piece
        }
    }

    /**
     * Deshace una jugada en la posición indicada (usado en simulaciones de IA).
     */
    fun undoMove(row: Int, col: Int) {
        if (row in 0 until ROWS && col !in 0 until COLS) return
        board[row][col] = null
    }

    /**
     * Retorna la lista de columnas que aún tienen al menos un espacio libre.
     */
    fun getValidColumns(): List<Int> {
        val valid = ArrayList<Int>()
        for (c in 0 until COLS) {
            if (board[0][c] == null) {
                valid.add(c)
            }
        }
        return valid
    }

    /**
     * Verifica si el tablero está completamente lleno (empate).
     */
    fun isBoardFull(): Boolean {
        return getValidColumns().isEmpty()
    }

    /**
     * Evalúa el tablero y retorna el resultado de victoria si existen 4 fichas consecutivas alineadas.
     */
    fun checkWinner(): ConnectFourWinningResult? {
        // 1. Comprobar Horizontales (6 filas x 4 inicios posibles)
        for (r in 0 until ROWS) {
            for (c in 0..COLS - WIN_STREAK) {
                val p = board[r][c] ?: continue
                if (p == board[r][c + 1] && p == board[r][c + 2] && p == board[r][c + 3]) {
                    val cells = listOf(
                        ConnectFourCell(r, c),
                        ConnectFourCell(r, c + 1),
                        ConnectFourCell(r, c + 2),
                        ConnectFourCell(r, c + 3)
                    )
                    return ConnectFourWinningResult(p, cells, ConnectFourLineType.HORIZONTAL)
                }
            }
        }

        // 2. Comprobar Verticales (3 inicios x 7 columnas)
        for (r in 0..ROWS - WIN_STREAK) {
            for (c in 0 until COLS) {
                val p = board[r][c] ?: continue
                if (p == board[r + 1][c] && p == board[r + 2][c] && p == board[r + 3][c]) {
                    val cells = listOf(
                        ConnectFourCell(r, c),
                        ConnectFourCell(r + 1, c),
                        ConnectFourCell(r + 2, c),
                        ConnectFourCell(r + 3, c)
                    )
                    return ConnectFourWinningResult(p, cells, ConnectFourLineType.VERTICAL)
                }
            }
        }

        // 3. Comprobar Diagonal Principal (\ descendente: r+1, c+1)
        for (r in 0..ROWS - WIN_STREAK) {
            for (c in 0..COLS - WIN_STREAK) {
                val p = board[r][c] ?: continue
                if (p == board[r + 1][c + 1] && p == board[r + 2][c + 2] && p == board[r + 3][c + 3]) {
                    val cells = listOf(
                        ConnectFourCell(r, c),
                        ConnectFourCell(r + 1, c + 1),
                        ConnectFourCell(r + 2, c + 2),
                        ConnectFourCell(r + 3, c + 3)
                    )
                    return ConnectFourWinningResult(p, cells, ConnectFourLineType.DIAGONAL_MAIN)
                }
            }
        }

        // 4. Comprobar Diagonal Anti (/ ascendente: r-1, c+1)
        for (r in WIN_STREAK - 1 until ROWS) {
            for (c in 0..COLS - WIN_STREAK) {
                val p = board[r][c] ?: continue
                if (p == board[r - 1][c + 1] && p == board[r - 2][c + 2] && p == board[r - 3][c + 3]) {
                    val cells = listOf(
                        ConnectFourCell(r, c),
                        ConnectFourCell(r - 1, c + 1),
                        ConnectFourCell(r - 2, c + 2),
                        ConnectFourCell(r - 3, c + 3)
                    )
                    return ConnectFourWinningResult(p, cells, ConnectFourLineType.DIAGONAL_ANTI)
                }
            }
        }

        return null
    }

    /**
     * Limpia el tablero por completo para una nueva partida.
     */
    fun reset() {
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                board[r][c] = null
            }
        }
    }
}
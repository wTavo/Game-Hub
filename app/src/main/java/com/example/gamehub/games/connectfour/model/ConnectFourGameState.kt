package com.example.gamehub.games.connectfour.model

/**
 * Estado reactivo del flujo de una partida de Conecta 4.
 */
sealed class ConnectFourGameState {
    object Idle : ConnectFourGameState()
    data class Playing(val currentTurn: ConnectFourPiece) : ConnectFourGameState()
    data class Won(val winningResult: ConnectFourWinningResult) : ConnectFourGameState()
    object Draw : ConnectFourGameState()
}
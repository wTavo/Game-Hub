package com.example.gamehub.games.tictactoe.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.gamehub.R
import com.example.gamehub.databinding.FragmentTictactoeBinding
import com.example.gamehub.games.tictactoe.data.ScoreManager
import com.example.gamehub.games.tictactoe.domain.TicTacToeEngine
import com.example.gamehub.games.tictactoe.domain.ai.AIPlayer
import com.example.gamehub.games.tictactoe.model.CellPosition
import com.example.gamehub.games.tictactoe.model.Difficulty
import com.example.gamehub.games.tictactoe.model.GameMode
import com.example.gamehub.games.tictactoe.model.GameState
import com.example.gamehub.games.tictactoe.model.Player
import com.example.gamehub.games.tictactoe.model.PlayerSymbolChoice
import com.example.gamehub.games.tictactoe.model.WinningLineType
import com.example.gamehub.games.tictactoe.model.WinningResult
import com.example.gamehub.core.animations.GameAnimations
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Fragmento que encapsula el juego Tres en Raya.
 * Gestiona el tablero 3x3, los modos 1J (vs IA) y 2 Jugadores, los niveles de dificultad,
 * las estadísticas y las animaciones de la experiencia de juego.
 */
class TicTacToeFragment : Fragment() {

    private var _binding: FragmentTictactoeBinding? = null
    private val binding get() = _binding!!

    private lateinit var scoreManager: ScoreManager

    // Motor de lógica de juego en memoria
    private val engine = TicTacToeEngine()

    // Estados actuales de configuración y partida
    private var gameMode: GameMode = GameMode.VS_AI
    private var difficulty: Difficulty = Difficulty.MEDIUM
    private var playerSymbolChoice: PlayerSymbolChoice = PlayerSymbolChoice.X
    private var humanPlayer: Player = Player.X
    private var cpuPlayer: Player = Player.O
    private var player1Symbol: Player = Player.X
    private var player2Symbol: Player = Player.O
    private var currentTurn: Player = Player.X
    private var gameState: GameState = GameState.Idle
    private var isGameActive: Boolean = false
    private var isCpuThinking: Boolean = false
    private var cpuJob: Job? = null

    // Matriz de referencias de vistas para las 9 celdas del tablero
    private lateinit var cellCards: Array<Array<MaterialCardView>>
    private lateinit var cellTextViews: Array<Array<TextView>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTictactoeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scoreManager = ScoreManager(requireContext())

        initViews()
        setupListeners()

        if (savedInstanceState != null) {
            restoreSavedGameState(savedInstanceState)
        } else {
            updateScoreboardUI()
            prepareFreshGame(showControls = true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Guardar el estado de las 9 celdas del tablero
        val boardArray = Array(9) { "" }
        for (r in 0..2) {
            for (c in 0..2) {
                boardArray[r * 3 + c] = engine.getCell(r, c)?.name ?: ""
            }
        }
        outState.putStringArray(KEY_BOARD, boardArray)
        outState.putString(KEY_GAME_MODE, gameMode.name)
        outState.putString(KEY_DIFFICULTY, difficulty.name)
        outState.putString(KEY_PLAYER_SYMBOL_CHOICE, playerSymbolChoice.name)
        outState.putString(KEY_HUMAN_PLAYER, humanPlayer.name)
        outState.putString(KEY_CPU_PLAYER, cpuPlayer.name)
        outState.putString(KEY_PLAYER1_SYMBOL, player1Symbol.name)
        outState.putString(KEY_PLAYER2_SYMBOL, player2Symbol.name)
        outState.putString(KEY_CURRENT_TURN, currentTurn.name)
        outState.putBoolean(KEY_IS_GAME_ACTIVE, isGameActive)
        outState.putBoolean(KEY_IS_CPU_THINKING, isCpuThinking)

        when (val state = gameState) {
            is GameState.Idle -> outState.putString(KEY_GAME_STATE_TYPE, "IDLE")
            is GameState.Playing -> {
                outState.putString(KEY_GAME_STATE_TYPE, "PLAYING")
                outState.putString(KEY_STATE_TURN, state.currentTurn.name)
            }
            is GameState.Won -> {
                outState.putString(KEY_GAME_STATE_TYPE, "WON")
                outState.putString(KEY_WINNER, state.winningResult.winner.name)
                val winningRows = state.winningResult.winningCells.map { it.row }.toIntArray()
                val winningCols = state.winningResult.winningCells.map { it.col }.toIntArray()
                outState.putIntArray(KEY_WINNING_ROWS, winningRows)
                outState.putIntArray(KEY_WINNING_COLS, winningCols)
            }
            is GameState.Draw -> outState.putString(KEY_GAME_STATE_TYPE, "DRAW")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cpuJob?.cancel()
        cpuJob = null
        _binding = null
    }

    /**
     * Inicializa las matrices de referencias a las vistas de las celdas (Cards y TextViews).
     */
    private fun initViews() {
        cellCards = arrayOf(
            arrayOf(binding.card00, binding.card01, binding.card02),
            arrayOf(binding.card10, binding.card11, binding.card12),
            arrayOf(binding.card20, binding.card21, binding.card22)
        )

        cellTextViews = arrayOf(
            arrayOf(binding.tvCell00, binding.tvCell01, binding.tvCell02),
            arrayOf(binding.tvCell10, binding.tvCell11, binding.tvCell12),
            arrayOf(binding.tvCell20, binding.tvCell21, binding.tvCell22)
        )
    }

    /**
     * Configura los listeners de eventos para todas las vistas interactivas.
     */
    private fun setupListeners() {
        // Clics en las celdas del tablero
        for (r in 0..2) {
            for (c in 0..2) {
                cellCards[r][c].setOnClickListener {
                    onCellClicked(CellPosition(r, c))
                }
            }
        }

        // Selector de Modo de Juego (1 Jugador vs 2 Jugadores)
        binding.toggleGameMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnModeVsAi -> {
                        gameMode = GameMode.VS_AI
                        GameAnimations.animateFadeIn(binding.togglePlayerSymbol)
                        GameAnimations.animateFadeIn(binding.toggleDifficulty)
                    }
                    R.id.btnModeTwoPlayers -> {
                        gameMode = GameMode.TWO_PLAYERS
                        GameAnimations.animateFadeOut(binding.togglePlayerSymbol)
                        GameAnimations.animateFadeOut(binding.toggleDifficulty)
                    }
                }
                updateScoreboardUI(animate = true)
                GameAnimations.animateScoreLabelChange(listOf(binding.tvScoreLabelX, binding.tvScoreLabelO))
                prepareFreshGame(showControls = true)
            }
        }

        // Selector de Ficha / Turno Inicial (Ficha X, Ficha O, Aleatorio)
        binding.togglePlayerSymbol.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                playerSymbolChoice = when (checkedId) {
                    R.id.btnSymbolX -> PlayerSymbolChoice.X
                    R.id.btnSymbolO -> PlayerSymbolChoice.O
                    else -> PlayerSymbolChoice.RANDOM
                }
                updateScoreboardColors()
                GameAnimations.animateScoreLabelChange(listOf(binding.tvScoreLabelX, binding.tvScoreLabelO))
                prepareFreshGame(showControls = true)
            }
        }

        // Selector de Dificultad (Fácil, Medio, Difícil)
        binding.toggleDifficulty.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                difficulty = when (checkedId) {
                    R.id.btnDiffEasy -> Difficulty.EASY
                    R.id.btnDiffHard -> Difficulty.HARD
                    else -> Difficulty.MEDIUM
                }
                updateScoreboardUI(animate = true)
                prepareFreshGame(showControls = true)
            }
        }

        // Botón Jugar de Nuevo (ubicado en el overlay central de fin de partida)
        binding.btnPlayAgain.setOnClickListener {
            GameAnimations.animateOverlayOut(binding.layoutBoardOverlay) {
                prepareFreshGame(showControls = false)
            }
        }

        // Botón Terminar Juego (ubicado en el overlay central de fin de partida)
        binding.btnFinishGame.setOnClickListener {
            GameAnimations.animateOverlayOut(binding.layoutBoardOverlay) {
                prepareFreshGame(showControls = true)
            }
        }

        // Botón Abandonar Partida
        binding.btnAbandonGame.setOnClickListener {
            prepareFreshGame(showControls = true)
        }
    }

    /**
     * Consulta las puntuaciones almacenadas en ScoreManager y actualiza los números en el marcador.
     *
     * @param animate true si se debe aplicar animación a los números que cambian.
     */
    private fun updateScoreboardUI(animate: Boolean = false) {
        val score = scoreManager.getScore(gameMode, difficulty)
        binding.tvScoreX.text = score.player1Wins.toString()
        binding.tvScoreO.text = score.player2Wins.toString()
        binding.tvScoreDraws.text = score.draws.toString()

        updateScoreboardColors()

        if (animate) {
            GameAnimations.animateScoreLabelChange(
                listOf(binding.tvScoreX, binding.tvScoreO, binding.tvScoreDraws)
            )
        }
    }

    /**
     * Actualiza los colores de las etiquetas del marcador superior según el modo y la ficha asignada a cada uno.
     * En reposo muestra colores neutros para no revelar la ficha antes de tiempo.
     * Al iniciar la partida, aplica inmediatamente el color de la ficha (Azul para X, Rojo para O).
     */
    private fun updateScoreboardColors() {
        val context = context ?: return
        val colorX = ContextCompat.getColor(context, R.color.player_x_color)
        val colorO = ContextCompat.getColor(context, R.color.player_o_color)
        val colorNeutralLabel = ContextCompat.getColor(context, R.color.text_secondary)

        if (gameMode == GameMode.VS_AI) {
            if (!isGameActive && playerSymbolChoice == PlayerSymbolChoice.RANDOM) {
                binding.tvScoreLabelX.setTextColor(colorNeutralLabel)
                binding.tvScoreLabelO.setTextColor(colorNeutralLabel)
            } else {
                val humanColor = if (humanPlayer == Player.X) colorX else colorO
                val cpuColor = if (cpuPlayer == Player.X) colorX else colorO

                binding.tvScoreLabelX.setTextColor(humanColor)
                binding.tvScoreLabelO.setTextColor(cpuColor)
            }
        } else {
            if (!isGameActive) {
                binding.tvScoreLabelX.setTextColor(colorNeutralLabel)
                binding.tvScoreLabelO.setTextColor(colorNeutralLabel)
            } else {
                val p1Color = if (player1Symbol == Player.X) colorX else colorO
                val p2Color = if (player2Symbol == Player.X) colorX else colorO

                binding.tvScoreLabelX.setTextColor(p1Color)
                binding.tvScoreLabelO.setTextColor(p2Color)
            }
        }
    }

    /**
     * Gestiona la pulsación de una casilla del tablero en la posición indicada.
     *
     * @param position Coordenadas (fila, columna) de la casilla pulsada.
     */
    private fun onCellClicked(position: CellPosition) {
        if (gameState is GameState.Won || gameState is GameState.Draw) return
        if (isCpuThinking) return

        // Si la partida aún no inicia, procesar sorteos de apertura y activación
        if (!isGameActive) {
            if (gameMode == GameMode.VS_AI) {
                if (playerSymbolChoice == PlayerSymbolChoice.RANDOM) {
                    if (Random.nextBoolean()) {
                        humanPlayer = Player.X
                        cpuPlayer = Player.O
                    } else {
                        humanPlayer = Player.O
                        cpuPlayer = Player.X
                    }
                    binding.tvScoreLabelX.text = getString(R.string.score_label_you_with_symbol, humanPlayer.symbol)
                    binding.tvScoreLabelO.text = getString(R.string.score_label_cpu_with_symbol, cpuPlayer.symbol)
                    GameAnimations.animateScoreLabelChange(listOf(binding.tvScoreLabelX, binding.tvScoreLabelO))
                }

                if (cpuPlayer == Player.X) {
                    setGameActiveState(true)
                    updateScoreboardColors()
                    gameState = GameState.Playing(Player.X)
                    updateTurnPresentation()
                    performCpuTurn()
                    return
                }
            } else {
                if (Random.nextBoolean()) {
                    player1Symbol = Player.X
                    player2Symbol = Player.O
                } else {
                    player1Symbol = Player.O
                    player2Symbol = Player.X
                }
                binding.tvScoreLabelX.text = getString(R.string.score_label_player_1_with_symbol, player1Symbol.symbol)
                binding.tvScoreLabelO.text = getString(R.string.score_label_player_2_with_symbol, player2Symbol.symbol)
                GameAnimations.animateScoreLabelChange(listOf(binding.tvScoreLabelX, binding.tvScoreLabelO))
            }

            setGameActiveState(true)
            updateScoreboardColors()
        }

        if (gameMode == GameMode.VS_AI && currentTurn == cpuPlayer) return
        if (engine.getCell(position.row, position.col) != null) return

        makePlayerMove(position)
    }

    /**
     * Realiza el movimiento en el tablero y actualiza el estado.
     */
    private fun makePlayerMove(position: CellPosition) {
        val player = currentTurn
        val success = engine.makeMove(position, player)
        if (!success) return

        triggerHapticFeedback()
        renderCell(position, player)

        val winner = engine.checkWinner()
        if (winner != null) {
            handleGameOver(winner)
            return
        }

        if (engine.isBoardFull()) {
            handleGameDraw()
            return
        }

        currentTurn = currentTurn.opponent()
        gameState = GameState.Playing(currentTurn)
        updateTurnPresentation()

        if (gameMode == GameMode.VS_AI && currentTurn == cpuPlayer) {
            performCpuTurn()
        }
    }

    /**
     * Ejecuta el turno de la IA con retardo realista (850ms).
     */
    private fun performCpuTurn() {
        cpuJob?.cancel()
        isCpuThinking = true
        updateTurnPresentation()

        cpuJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(850L)

            if (!isActive || !isGameActive || gameState !is GameState.Playing || currentTurn != cpuPlayer) {
                isCpuThinking = false
                return@launch
            }

            val cpuMove = AIPlayer.getNextMove(engine, difficulty, cpuPlayer)
            isCpuThinking = false

            if (cpuMove != null && isGameActive && gameState is GameState.Playing) {
                val success = engine.makeMove(cpuMove, cpuPlayer)
                if (success) {
                    triggerHapticFeedback()
                    renderCell(cpuMove, cpuPlayer)

                    val winner = engine.checkWinner()
                    if (winner != null) {
                        handleGameOver(winner)
                        return@launch
                    }

                    if (engine.isBoardFull()) {
                        handleGameDraw()
                        return@launch
                    }

                    currentTurn = humanPlayer
                    gameState = GameState.Playing(currentTurn)
                    updateTurnPresentation()
                }
            }
        }
    }

    /**
     * Renderiza la ficha en el TextView de la casilla especificada.
     */
    private fun renderCell(position: CellPosition, player: Player) {
        val context = context ?: return
        val tv = cellTextViews[position.row][position.col]
        tv.text = player.symbol
        val colorRes = if (player == Player.X) R.color.player_x_color else R.color.player_o_color
        tv.setTextColor(ContextCompat.getColor(context, colorRes))

        GameAnimations.animatePopIn(tv)
    }

    /**
     * Gestiona el fin de partida por victoria.
     */
    private fun handleGameOver(result: WinningResult) {
        val context = context ?: return
        gameState = GameState.Won(result)
        isGameActive = false
        triggerWinVibration()

        if (binding.btnAbandonGame.visibility == View.VISIBLE) {
            GameAnimations.animateFadeOut(binding.btnAbandonGame)
        }

        if (gameMode == GameMode.VS_AI) {
            if (result.winner == humanPlayer) {
                val updatedScore = scoreManager.recordPlayer1Win(gameMode, difficulty)
                binding.tvScoreX.text = updatedScore.player1Wins.toString()
                GameAnimations.animateScoreValueIncrement(binding.tvScoreX)

                binding.tvOverlayTitle.text = getString(R.string.overlay_victory)
                binding.tvOverlaySubtitle.text = getString(R.string.overlay_desc_winner_you)
            } else {
                val updatedScore = scoreManager.recordPlayer2Win(gameMode, difficulty)
                binding.tvScoreO.text = updatedScore.player2Wins.toString()
                GameAnimations.animateScoreValueIncrement(binding.tvScoreO)

                binding.tvOverlayTitle.text = getString(R.string.overlay_defeat)
                binding.tvOverlaySubtitle.text = getString(R.string.overlay_desc_winner_cpu)
            }
        } else {
            if (result.winner == player1Symbol) {
                val updatedScore = scoreManager.recordPlayer1Win(gameMode, difficulty)
                binding.tvScoreX.text = updatedScore.player1Wins.toString()
                GameAnimations.animateScoreValueIncrement(binding.tvScoreX)

                binding.tvOverlayTitle.text = getString(R.string.overlay_victory)
                binding.tvOverlaySubtitle.text = getString(R.string.overlay_desc_winner_player_1)
            } else {
                val updatedScore = scoreManager.recordPlayer2Win(gameMode, difficulty)
                binding.tvScoreO.text = updatedScore.player2Wins.toString()
                GameAnimations.animateScoreValueIncrement(binding.tvScoreO)

                binding.tvOverlayTitle.text = getString(R.string.overlay_victory)
                binding.tvOverlaySubtitle.text = getString(R.string.overlay_desc_winner_player_2)
            }
        }

        updateTurnPresentation()

        val winningViews = result.winningCells.map { cellCards[it.row][it.col] }
        result.winningCells.forEach { pos ->
            cellCards[pos.row][pos.col].strokeColor = ContextCompat.getColor(context, R.color.win_glow)
            cellCards[pos.row][pos.col].strokeWidth = (2.5f * resources.displayMetrics.density).toInt()
        }
        GameAnimations.animateWinningCells(winningViews)

        lifecycleScope.launch {
            delay(400L)
            if (gameState is GameState.Won && _binding != null) {
                GameAnimations.animateOverlayIn(binding.layoutBoardOverlay, binding.cardOverlayContent)
            }
        }
    }

    /**
     * Gestiona el fin de partida por empate.
     */
    private fun handleGameDraw() {
        gameState = GameState.Draw
        isGameActive = false
        val updatedScore = scoreManager.recordDraw(gameMode, difficulty)
        binding.tvScoreDraws.text = updatedScore.draws.toString()
        GameAnimations.animateScoreValueIncrement(binding.tvScoreDraws)

        if (binding.btnAbandonGame.visibility == View.VISIBLE) {
            GameAnimations.animateFadeOut(binding.btnAbandonGame)
        }

        binding.tvOverlayTitle.text = getString(R.string.overlay_draw)
        binding.tvOverlaySubtitle.text = getString(R.string.overlay_desc_draw)

        updateTurnPresentation()

        lifecycleScope.launch {
            delay(350L)
            if (gameState is GameState.Draw && _binding != null) {
                GameAnimations.animateOverlayIn(binding.layoutBoardOverlay, binding.cardOverlayContent)
            }
        }
    }

    /**
     * Actualiza la presentación visual de turno y resultado.
     */
    private fun updateTurnPresentation() {
        val context = context ?: return
        val density = resources.displayMetrics.density
        val colorX = ContextCompat.getColor(context, R.color.player_x_color)
        val colorO = ContextCompat.getColor(context, R.color.player_o_color)
        val strokeDefault = ContextCompat.getColor(context, R.color.cell_stroke)
        val colorTextPrimary = ContextCompat.getColor(context, R.color.text_primary)
        val colorMuted = ContextCompat.getColor(context, R.color.text_muted)

        when (val state = gameState) {
            is GameState.Idle -> {
                binding.cardTurnBadge.strokeColor = strokeDefault
                binding.tvTurnLabel.text = getString(R.string.label_status)
                binding.tvTurnLabel.setTextColor(colorMuted)
                binding.tvStatus.text = getString(R.string.status_ready_to_play)
                binding.tvStatus.setTextColor(colorTextPrimary)

                binding.cardScoreX.strokeColor = strokeDefault
                binding.cardScoreX.strokeWidth = (1f * density).toInt()
                binding.cardScoreO.strokeColor = strokeDefault
                binding.cardScoreO.strokeWidth = (1f * density).toInt()
            }
            is GameState.Playing -> {
                binding.tvTurnLabel.text = getString(R.string.label_turn_of)
                binding.tvTurnLabel.setTextColor(colorMuted)

                val activeColor = if (state.currentTurn == Player.X) colorX else colorO
                binding.cardTurnBadge.strokeColor = activeColor

                val statusText = if (gameMode == GameMode.VS_AI) {
                    if (state.currentTurn == humanPlayer) getString(R.string.score_label_you) else getString(R.string.turn_cpu)
                } else {
                    if (state.currentTurn == player1Symbol) getString(R.string.turn_player_1) else getString(R.string.turn_player_2)
                }
                binding.tvStatus.text = statusText
                binding.tvStatus.setTextColor(activeColor)

                val isPlayer1Turn = if (gameMode == GameMode.VS_AI) state.currentTurn == humanPlayer else state.currentTurn == player1Symbol
                if (isPlayer1Turn) {
                    binding.cardScoreX.strokeColor = activeColor
                    binding.cardScoreX.strokeWidth = (2f * density).toInt()
                    binding.cardScoreO.strokeColor = strokeDefault
                    binding.cardScoreO.strokeWidth = (1f * density).toInt()
                } else {
                    binding.cardScoreO.strokeColor = activeColor
                    binding.cardScoreO.strokeWidth = (2f * density).toInt()
                    binding.cardScoreX.strokeColor = strokeDefault
                    binding.cardScoreX.strokeWidth = (1f * density).toInt()
                }
            }
            is GameState.Won -> {
                val winColor = if (state.winningResult.winner == Player.X) colorX else colorO
                binding.cardTurnBadge.strokeColor = ContextCompat.getColor(context, R.color.win_glow)
                binding.tvTurnLabel.text = getString(R.string.label_result)
                binding.tvTurnLabel.setTextColor(ContextCompat.getColor(context, R.color.win_glow))
                val winText = if (gameMode == GameMode.VS_AI) {
                    if (state.winningResult.winner == humanPlayer) getString(R.string.status_winner_you) else getString(R.string.status_winner_cpu)
                } else {
                    if (state.winningResult.winner == player1Symbol) getString(R.string.status_winner_player_1) else getString(R.string.status_winner_player_2)
                }
                binding.tvStatus.text = winText
                binding.tvStatus.setTextColor(winColor)
            }
            is GameState.Draw -> {
                val drawColor = ContextCompat.getColor(context, R.color.draw_color)
                binding.cardTurnBadge.strokeColor = drawColor
                binding.tvTurnLabel.text = getString(R.string.label_result)
                binding.tvTurnLabel.setTextColor(drawColor)
                binding.tvStatus.text = getString(R.string.status_draw)
                binding.tvStatus.setTextColor(drawColor)
            }
        }
    }

    /**
     * Alterna la visibilidad entre los controles y el botón de abandonar, y oculta/muestra la barra de menú.
     */
    private fun setGameActiveState(active: Boolean, animated: Boolean = true) {
        isGameActive = active
        updateScoreboardColors()

        // Ocultar o mostrar la barra de navegación para evitar abandonar por accidente
        (activity as? com.example.gamehub.MainActivity)?.setBottomNavigationVisible(!active, animated = animated)

        if (active) {
            if (binding.btnAbandonGame.visibility != View.VISIBLE) {
                if (animated) {
                    binding.btnAbandonGame.alpha = 0f
                    binding.btnAbandonGame.visibility = View.VISIBLE
                    GameAnimations.animateFadeIn(binding.btnAbandonGame, duration = 220L)

                    if (binding.layoutControls.visibility == View.VISIBLE) {
                        GameAnimations.animateFadeOut(binding.layoutControls, duration = 200L)
                    }
                } else {
                    binding.layoutControls.visibility = View.GONE
                    binding.layoutControls.alpha = 1f
                    binding.btnAbandonGame.visibility = View.VISIBLE
                    binding.btnAbandonGame.alpha = 1f
                }
            }
        } else {
            if (binding.layoutControls.visibility != View.VISIBLE) {
                if (animated) {
                    binding.layoutControls.alpha = 0f
                    binding.layoutControls.visibility = View.VISIBLE
                    GameAnimations.animateFadeIn(binding.layoutControls, duration = 220L)

                    if (binding.btnAbandonGame.visibility == View.VISIBLE) {
                        GameAnimations.animateFadeOut(binding.btnAbandonGame, duration = 200L)
                    }
                } else {
                    binding.layoutControls.visibility = View.VISIBLE
                    binding.layoutControls.alpha = 1f
                    binding.btnAbandonGame.visibility = View.GONE
                }
            } else {
                binding.layoutControls.visibility = View.VISIBLE
                binding.layoutControls.alpha = 1f
                binding.btnAbandonGame.visibility = View.GONE
            }
        }
    }

    /**
     * Prepara una nueva partida reiniciando el motor de juego y marcadores de forma inmediata.
     */
    private fun prepareFreshGame(showControls: Boolean) {
        val context = context ?: return
        cpuJob?.cancel()
        cpuJob = null
        isCpuThinking = false
        engine.reset()
        currentTurn = Player.X

        if (gameMode == GameMode.VS_AI) {
            when (playerSymbolChoice) {
                PlayerSymbolChoice.X -> {
                    humanPlayer = Player.X
                    cpuPlayer = Player.O
                    binding.tvScoreLabelX.text = getString(R.string.score_label_you_with_symbol, "X")
                    binding.tvScoreLabelO.text = getString(R.string.score_label_cpu_with_symbol, "O")
                }
                PlayerSymbolChoice.O -> {
                    humanPlayer = Player.O
                    cpuPlayer = Player.X
                    binding.tvScoreLabelX.text = getString(R.string.score_label_you_with_symbol, "O")
                    binding.tvScoreLabelO.text = getString(R.string.score_label_cpu_with_symbol, "X")
                }
                PlayerSymbolChoice.RANDOM -> {
                    if (showControls) {
                        binding.tvScoreLabelX.text = getString(R.string.score_label_you)
                        binding.tvScoreLabelO.text = getString(R.string.score_label_cpu)
                    } else {
                        if (Random.nextBoolean()) {
                            humanPlayer = Player.X
                            cpuPlayer = Player.O
                        } else {
                            humanPlayer = Player.O
                            cpuPlayer = Player.X
                        }
                        binding.tvScoreLabelX.text = getString(R.string.score_label_you_with_symbol, humanPlayer.symbol)
                        binding.tvScoreLabelO.text = getString(R.string.score_label_cpu_with_symbol, cpuPlayer.symbol)
                    }
                }
            }
        } else {
            if (showControls) {
                binding.tvScoreLabelX.text = getString(R.string.score_label_player_1)
                binding.tvScoreLabelO.text = getString(R.string.score_label_player_2)
            } else {
                if (Random.nextBoolean()) {
                    player1Symbol = Player.X
                    player2Symbol = Player.O
                } else {
                    player1Symbol = Player.O
                    player2Symbol = Player.X
                }
                binding.tvScoreLabelX.text = getString(R.string.score_label_player_1_with_symbol, player1Symbol.symbol)
                binding.tvScoreLabelO.text = getString(R.string.score_label_player_2_with_symbol, player2Symbol.symbol)
            }
        }

        gameState = if (showControls) GameState.Idle else GameState.Playing(currentTurn)

        setGameActiveState(active = !showControls, animated = true)
        updateScoreboardColors()
        updateTurnPresentation()

        binding.layoutBoardOverlay.visibility = View.GONE

        val resetViews = {
            for (r in 0..2) {
                for (c in 0..2) {
                    cellTextViews[r][c].text = ""
                    cellCards[r][c].strokeColor = ContextCompat.getColor(context, R.color.cell_stroke)
                    cellCards[r][c].strokeWidth = (1f * resources.displayMetrics.density).toInt()
                }
            }

            if (!showControls && gameMode == GameMode.VS_AI && cpuPlayer == Player.X) {
                performCpuTurn()
            }
        }

        val allCellCards = cellCards.flatten()
        val hasContent = cellTextViews.flatten().any { it.text.isNotEmpty() }
        if (hasContent && !showControls) {
            GameAnimations.animateBoardClear(allCellCards) {
                if (_binding != null) resetViews()
            }
        } else {
            resetViews()
        }
    }

    /**
     * Dispara una vibración táctil suave (25ms) al pulsar una casilla válida.
     */
    private fun triggerHapticFeedback() {
        val context = context ?: return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(25)
                }
            }
        } catch (_: Exception) {
            // Silencioso
        }
    }

    /**
     * Dispara un patrón de vibración festivo para celebrar la victoria.
     */
    private fun triggerWinVibration() {
        val context = context ?: return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 70, 70, 140), -1))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(longArrayOf(0, 70, 70, 140), -1)
                }
            }
        } catch (_: Exception) {
            // Silencioso
        }
    }

    /**
     * Restaura el estado de la partida en curso tras un cambio de configuración (ej. rotación de pantalla).
     */
    private fun restoreSavedGameState(savedInstanceState: Bundle) {
        val context = context ?: return

        // 1. Restaurar configuraciones
        val modeStr = savedInstanceState.getString(KEY_GAME_MODE, GameMode.VS_AI.name)
        gameMode = try { GameMode.valueOf(modeStr) } catch (_: Exception) { GameMode.VS_AI }

        val diffStr = savedInstanceState.getString(KEY_DIFFICULTY, Difficulty.MEDIUM.name)
        difficulty = try { Difficulty.valueOf(diffStr) } catch (_: Exception) { Difficulty.MEDIUM }

        val choiceStr = savedInstanceState.getString(KEY_PLAYER_SYMBOL_CHOICE, PlayerSymbolChoice.X.name)
        playerSymbolChoice = try { PlayerSymbolChoice.valueOf(choiceStr) } catch (_: Exception) { PlayerSymbolChoice.X }

        val humanStr = savedInstanceState.getString(KEY_HUMAN_PLAYER, Player.X.name)
        humanPlayer = try { Player.valueOf(humanStr) } catch (_: Exception) { Player.X }

        val cpuStr = savedInstanceState.getString(KEY_CPU_PLAYER, Player.O.name)
        cpuPlayer = try { Player.valueOf(cpuStr) } catch (_: Exception) { Player.O }

        val p1Str = savedInstanceState.getString(KEY_PLAYER1_SYMBOL, Player.X.name)
        player1Symbol = try { Player.valueOf(p1Str) } catch (_: Exception) { Player.X }

        val p2Str = savedInstanceState.getString(KEY_PLAYER2_SYMBOL, Player.O.name)
        player2Symbol = try { Player.valueOf(p2Str) } catch (_: Exception) { Player.O }

        val turnStr = savedInstanceState.getString(KEY_CURRENT_TURN, Player.X.name)
        currentTurn = try { Player.valueOf(turnStr) } catch (_: Exception) { Player.X }

        isGameActive = savedInstanceState.getBoolean(KEY_IS_GAME_ACTIVE, false)
        isCpuThinking = savedInstanceState.getBoolean(KEY_IS_CPU_THINKING, false)

        // 2. Sincronizar botones de control en la UI
        binding.toggleGameMode.check(if (gameMode == GameMode.VS_AI) R.id.btnModeVsAi else R.id.btnModeTwoPlayers)
        binding.toggleDifficulty.check(when (difficulty) {
            Difficulty.EASY -> R.id.btnDiffEasy
            Difficulty.MEDIUM -> R.id.btnDiffMedium
            Difficulty.HARD -> R.id.btnDiffHard
        })
        binding.togglePlayerSymbol.check(when (playerSymbolChoice) {
            PlayerSymbolChoice.X -> R.id.btnSymbolX
            PlayerSymbolChoice.O -> R.id.btnSymbolO
            PlayerSymbolChoice.RANDOM -> R.id.btnSymbolRandom
        })

        if (gameMode == GameMode.TWO_PLAYERS) {
            binding.togglePlayerSymbol.visibility = View.GONE
            binding.toggleDifficulty.visibility = View.GONE
        } else {
            binding.togglePlayerSymbol.visibility = View.VISIBLE
            binding.toggleDifficulty.visibility = View.VISIBLE
        }

        // 3. Restaurar tablero
        val boardArray = savedInstanceState.getStringArray(KEY_BOARD)
        if (boardArray != null) {
            for (r in 0..2) {
                for (c in 0..2) {
                    val pName = boardArray[r * 3 + c]
                    if (pName.isNotEmpty()) {
                        val player = try { Player.valueOf(pName) } catch (_: Exception) { null }
                        engine.board[r][c] = player
                        val tv = cellTextViews[r][c]
                        if (player != null) {
                            tv.text = player.symbol
                            val colorRes = if (player == Player.X) R.color.player_x_color else R.color.player_o_color
                            tv.setTextColor(ContextCompat.getColor(context, colorRes))
                        } else {
                            tv.text = ""
                        }
                    } else {
                        engine.board[r][c] = null
                        cellTextViews[r][c].text = ""
                    }
                    cellCards[r][c].strokeColor = ContextCompat.getColor(context, R.color.cell_stroke)
                    cellCards[r][c].strokeWidth = (1f * resources.displayMetrics.density).toInt()
                }
            }
        }

        // 4. Restaurar estado de partida y marcador
        updateScoreboardUI()

        if (gameMode == GameMode.VS_AI) {
            if (isGameActive && playerSymbolChoice == PlayerSymbolChoice.RANDOM) {
                val youLabel = if (humanPlayer == Player.X) getString(R.string.score_label_you_with_symbol, "X") else getString(R.string.score_label_you_with_symbol, "O")
                val cpuLabel = if (cpuPlayer == Player.X) getString(R.string.score_label_cpu_with_symbol, "X") else getString(R.string.score_label_cpu_with_symbol, "O")
                binding.tvScoreLabelX.text = youLabel
                binding.tvScoreLabelO.text = cpuLabel
            }
        } else {
            if (isGameActive) {
                binding.tvScoreLabelX.text = getString(R.string.score_label_player_1_with_symbol, player1Symbol.symbol)
                binding.tvScoreLabelO.text = getString(R.string.score_label_player_2_with_symbol, player2Symbol.symbol)
            }
        }

        val stateType = savedInstanceState.getString(KEY_GAME_STATE_TYPE, "IDLE")
        when (stateType) {
            "IDLE" -> {
                gameState = GameState.Idle
                setGameActiveState(false, animated = false)
            }
            "PLAYING" -> {
                val turn = try {
                    Player.valueOf(savedInstanceState.getString(KEY_STATE_TURN, currentTurn.name))
                } catch (_: Exception) { currentTurn }
                gameState = GameState.Playing(turn)
                setGameActiveState(isGameActive, animated = false)
            }
            "WON" -> {
                val winnerName = savedInstanceState.getString(KEY_WINNER, Player.X.name)
                val winner = try { Player.valueOf(winnerName) } catch (_: Exception) { Player.X }
                val winRows = savedInstanceState.getIntArray(KEY_WINNING_ROWS) ?: intArrayOf()
                val winCols = savedInstanceState.getIntArray(KEY_WINNING_COLS) ?: intArrayOf()
                val winningCells = winRows.indices.map { CellPosition(winRows[it], winCols[it]) }
                val winningResult = WinningResult(winner, WinningLineType.ROW_0, winningCells)
                gameState = GameState.Won(winningResult)
                setGameActiveState(false, animated = false)

                // Resaltar celdas ganadoras
                winningCells.forEach { pos ->
                    cellCards[pos.row][pos.col].strokeColor = ContextCompat.getColor(context, R.color.win_glow)
                    cellCards[pos.row][pos.col].strokeWidth = (2.5f * resources.displayMetrics.density).toInt()
                }
                binding.layoutBoardOverlay.visibility = View.VISIBLE
            }
            "DRAW" -> {
                gameState = GameState.Draw
                setGameActiveState(false, animated = false)
                binding.layoutBoardOverlay.visibility = View.VISIBLE
            }
        }

        updateTurnPresentation()

        // Reanudar turno IA si la rotación ocurrió mientras la IA calculaba
        if (isGameActive && gameMode == GameMode.VS_AI && currentTurn == cpuPlayer && gameState is GameState.Playing) {
            performCpuTurn()
        }
    }

    companion object {
        private const val KEY_BOARD = "tictactoe_board"
        private const val KEY_GAME_MODE = "tictactoe_game_mode"
        private const val KEY_DIFFICULTY = "tictactoe_difficulty"
        private const val KEY_PLAYER_SYMBOL_CHOICE = "tictactoe_player_symbol_choice"
        private const val KEY_HUMAN_PLAYER = "tictactoe_human_player"
        private const val KEY_CPU_PLAYER = "tictactoe_cpu_player"
        private const val KEY_PLAYER1_SYMBOL = "tictactoe_player1_symbol"
        private const val KEY_PLAYER2_SYMBOL = "tictactoe_player2_symbol"
        private const val KEY_CURRENT_TURN = "tictactoe_current_turn"
        private const val KEY_IS_GAME_ACTIVE = "tictactoe_is_game_active"
        private const val KEY_IS_CPU_THINKING = "tictactoe_is_cpu_thinking"
        private const val KEY_GAME_STATE_TYPE = "tictactoe_game_state_type"
        private const val KEY_STATE_TURN = "tictactoe_state_turn"
        private const val KEY_WINNER = "tictactoe_winner"
        private const val KEY_WINNING_ROWS = "tictactoe_winning_rows"
        private const val KEY_WINNING_COLS = "tictactoe_winning_cols"
    }
}

package com.example.gamehub.games.connectfour.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.gamehub.R
import com.example.gamehub.core.animations.GameAnimations
import com.example.gamehub.databinding.FragmentConnectFourBinding
import com.example.gamehub.games.connectfour.data.ConnectFourScoreManager
import com.example.gamehub.games.connectfour.domain.ConnectFourEngine
import com.example.gamehub.games.connectfour.domain.ai.ConnectFourAI
import com.example.gamehub.games.connectfour.model.ConnectFourDifficulty
import com.example.gamehub.games.connectfour.model.ConnectFourGameMode
import com.example.gamehub.games.connectfour.model.ConnectFourGameState
import com.example.gamehub.games.connectfour.model.ConnectFourPiece
import com.example.gamehub.games.connectfour.model.ConnectFourWinningResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.core.view.isVisible
import kotlin.time.Duration.Companion.milliseconds

/**
 * Representa la selección de ficha en el menú de configuración de Conecta 4.
 */
enum class ConnectFourSymbolChoice {
    YELLOW,
    RED,
    RANDOM
}

/**
 * Controlador de interfaz, físicas gravitacionales y flujo de Conecta 4.
 * Mantiene la misma consistencia visual y sincronización cromática que el Tres en Raya.
 */
class ConnectFourFragment : Fragment() {

    private var _binding: FragmentConnectFourBinding? = null
    private val binding get() = _binding!!

    private lateinit var scoreManager: ConnectFourScoreManager
    private val engine = ConnectFourEngine()

    // Configuración y estados
    private var gameMode: ConnectFourGameMode = ConnectFourGameMode.VS_AI
    private var difficulty: ConnectFourDifficulty = ConnectFourDifficulty.MEDIUM
    private var playerChoice: ConnectFourSymbolChoice = ConnectFourSymbolChoice.YELLOW
    private var humanPiece: ConnectFourPiece = ConnectFourPiece.YELLOW
    private var cpuPiece: ConnectFourPiece = ConnectFourPiece.RED
    private var player1Piece: ConnectFourPiece = ConnectFourPiece.YELLOW
    private var player2Piece: ConnectFourPiece = ConnectFourPiece.RED
    private var currentTurn: ConnectFourPiece = ConnectFourPiece.YELLOW
    private var gameState: ConnectFourGameState = ConnectFourGameState.Idle
    private var isGameActive: Boolean = false
    private var isCpuThinking: Boolean = false
    private var isDroppingPiece: Boolean = false
    private var cpuJob: Job? = null

    // Matriz de vistas de ranuras [row][col] y columnas
    private lateinit var slotViews: Array<Array<ImageView>>
    private lateinit var columnLayouts: Array<LinearLayout>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectFourBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scoreManager = ConnectFourScoreManager(requireContext())

        initViews()
        setupListeners()
        updateScoreboardUI()

        if (savedInstanceState != null) {
            restoreSavedGameState(savedInstanceState)
        } else {
            prepareFreshGame(showControls = true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cpuJob?.cancel()
        cpuJob = null
        _binding = null
    }

    /**
     * Indexa las referencias de las 42 ranuras (6 filas x 7 columnas) y las 7 columnas interactivas.
     */
    private fun initViews() {
        columnLayouts = arrayOf(
            binding.col0, binding.col1, binding.col2,
            binding.col3, binding.col4, binding.col5, binding.col6
        )

        slotViews = arrayOf(
            arrayOf(binding.slot00, binding.slot01, binding.slot02, binding.slot03, binding.slot04, binding.slot05, binding.slot06),
            arrayOf(binding.slot10, binding.slot11, binding.slot12, binding.slot13, binding.slot14, binding.slot15, binding.slot16),
            arrayOf(binding.slot20, binding.slot21, binding.slot22, binding.slot23, binding.slot24, binding.slot25, binding.slot26),
            arrayOf(binding.slot30, binding.slot31, binding.slot32, binding.slot33, binding.slot34, binding.slot35, binding.slot36),
            arrayOf(binding.slot40, binding.slot41, binding.slot42, binding.slot43, binding.slot44, binding.slot45, binding.slot46),
            arrayOf(binding.slot50, binding.slot51, binding.slot52, binding.slot53, binding.slot54, binding.slot55, binding.slot56)
        )
    }

    /**
     * Configura los listeners de eventos para todas las vistas interactivas.
     */
    private fun setupListeners() {
        for (col in 0 until ConnectFourEngine.COLS) {
            columnLayouts[col].setOnClickListener {
                onColumnClicked(col)
            }
        }

        // Selector de Modo de Juego (1J / 2J)
        binding.toggleGameMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnModeVsAi -> {
                        gameMode = ConnectFourGameMode.VS_AI
                        GameAnimations.animateFadeIn(binding.togglePlayerSymbol)
                        GameAnimations.animateFadeIn(binding.toggleDifficulty)
                    }
                    R.id.btnModeTwoPlayers -> {
                        gameMode = ConnectFourGameMode.TWO_PLAYERS
                        GameAnimations.animateFadeOut(binding.togglePlayerSymbol)
                        GameAnimations.animateFadeOut(binding.toggleDifficulty)
                    }
                }
                updateScoreboardUI(animate = true)
                GameAnimations.animateScoreLabelChange(listOf(binding.tvScoreLabelX, binding.tvScoreLabelO))
                prepareFreshGame(showControls = true)
            }
        }

        // Selector de Ficha / Color Inicial (Amarillo, Rojo, Aleatorio)
        binding.togglePlayerSymbol.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                playerChoice = when (checkedId) {
                    R.id.btnSymbolX -> ConnectFourSymbolChoice.YELLOW
                    R.id.btnSymbolO -> ConnectFourSymbolChoice.RED
                    else -> ConnectFourSymbolChoice.RANDOM
                }
                when (playerChoice) {
                    ConnectFourSymbolChoice.YELLOW -> {
                        humanPiece = ConnectFourPiece.YELLOW
                        cpuPiece = ConnectFourPiece.RED
                    }
                    ConnectFourSymbolChoice.RED -> {
                        humanPiece = ConnectFourPiece.RED
                        cpuPiece = ConnectFourPiece.YELLOW
                    }
                    ConnectFourSymbolChoice.RANDOM -> {}
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
                    R.id.btnDiffEasy -> ConnectFourDifficulty.EASY
                    R.id.btnDiffHard -> ConnectFourDifficulty.HARD
                    else -> ConnectFourDifficulty.MEDIUM
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
     * Actualiza las puntuaciones numéricas en el marcador.
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
     * Actualiza los colores de las etiquetas según el color asignado a cada participante.
     */
    private fun updateScoreboardColors() {
        val context = context ?: return
        val colorYellow = ContextCompat.getColor(context, R.color.c4_yellow)
        val colorRed = ContextCompat.getColor(context, R.color.c4_red)
        val colorNeutral = ContextCompat.getColor(context, R.color.text_secondary)

        if (gameMode == ConnectFourGameMode.VS_AI) {
            if (!isGameActive && playerChoice == ConnectFourSymbolChoice.RANDOM) {
                binding.tvScoreLabelX.text = getString(R.string.score_label_you)
                binding.tvScoreLabelO.text = getString(R.string.score_label_cpu)
                binding.tvScoreLabelX.setTextColor(colorNeutral)
                binding.tvScoreLabelO.setTextColor(colorNeutral)
            } else {
                binding.tvScoreLabelX.text = getString(R.string.score_label_you)
                binding.tvScoreLabelO.text = getString(R.string.score_label_cpu)
                val hColor = if (humanPiece == ConnectFourPiece.YELLOW) colorYellow else colorRed
                val cColor = if (cpuPiece == ConnectFourPiece.YELLOW) colorYellow else colorRed
                binding.tvScoreLabelX.setTextColor(hColor)
                binding.tvScoreLabelO.setTextColor(cColor)
            }
        } else {
            binding.tvScoreLabelX.text = getString(R.string.score_label_player_1)
            binding.tvScoreLabelO.text = getString(R.string.score_label_player_2)
            if (!isGameActive) {
                binding.tvScoreLabelX.setTextColor(colorNeutral)
                binding.tvScoreLabelO.setTextColor(colorNeutral)
            } else {
                val p1Color = if (player1Piece == ConnectFourPiece.YELLOW) colorYellow else colorRed
                val p2Color = if (player2Piece == ConnectFourPiece.YELLOW) colorYellow else colorRed
                binding.tvScoreLabelX.setTextColor(p1Color)
                binding.tvScoreLabelO.setTextColor(p2Color)
            }
        }
    }

    /**
     * Gestiona el toque sobre una columna.
     */
    private fun onColumnClicked(col: Int) {
        if (gameState is ConnectFourGameState.Won || gameState is ConnectFourGameState.Draw) return
        if (isCpuThinking || isDroppingPiece) return

        val targetRow = engine.getLowestAvailableRow(col) ?: return

        if (!isGameActive) {
            if (gameMode == ConnectFourGameMode.VS_AI) {
                when (playerChoice) {
                    ConnectFourSymbolChoice.RANDOM -> {
                        if (Random.nextBoolean()) {
                            humanPiece = ConnectFourPiece.YELLOW
                            cpuPiece = ConnectFourPiece.RED
                        } else {
                            humanPiece = ConnectFourPiece.RED
                            cpuPiece = ConnectFourPiece.YELLOW
                        }
                        GameAnimations.animateScoreLabelChange(listOf(binding.tvScoreLabelX, binding.tvScoreLabelO))
                    }
                    ConnectFourSymbolChoice.YELLOW -> {
                        humanPiece = ConnectFourPiece.YELLOW
                        cpuPiece = ConnectFourPiece.RED
                    }
                    else -> {
                        humanPiece = ConnectFourPiece.RED
                        cpuPiece = ConnectFourPiece.YELLOW
                    }
                }

                if (cpuPiece == ConnectFourPiece.YELLOW) {
                    setGameActiveState(true)
                    updateScoreboardColors()
                    gameState = ConnectFourGameState.Playing(ConnectFourPiece.YELLOW)
                    updateTurnPresentation()
                    performCpuTurn()
                    return
                }
            } else {
                if (Random.nextBoolean()) {
                    player1Piece = ConnectFourPiece.YELLOW
                    player2Piece = ConnectFourPiece.RED
                } else {
                    player1Piece = ConnectFourPiece.RED
                    player2Piece = ConnectFourPiece.YELLOW
                }
                GameAnimations.animateScoreLabelChange(listOf(binding.tvScoreLabelX, binding.tvScoreLabelO))
            }

            setGameActiveState(true)
            updateScoreboardColors()
        }

        executeMove(col, targetRow, currentTurn)
    }

    /**
     * Ejecuta el movimiento en la columna y fila con animación de caída física.
     */
    private fun executeMove(col: Int, targetRow: Int, piece: ConnectFourPiece) {
        isDroppingPiece = true
        engine.dropPiece(col, piece)

        val slot = slotViews[targetRow][col]
        val pieceRes = if (piece == ConnectFourPiece.YELLOW) R.drawable.bg_c4_piece_yellow else R.drawable.bg_c4_piece_red
        slot.setImageResource(pieceRes)

        // Simulación física de caída gravitacional acelerada con rebote elástico
        val dropDistance = -((targetRow + 1) * slot.height.toFloat() + 80f)
        slot.translationY = dropDistance

        val duration = 240L + (targetRow * 38L)
        slot.animate()
            .translationY(0f)
            .setDuration(duration)
            .setInterpolator(BounceInterpolator())
            .withEndAction {
                triggerHapticFeedback()
                isDroppingPiece = false
                checkPostMoveStatus(piece)
            }
            .start()
    }

    /**
     * Comprueba victoria o empate tras finalizar la animación de caída de la ficha.
     */
    private fun checkPostMoveStatus(pieceJustMoved: ConnectFourPiece) {
        val win = engine.checkWinner()
        if (win != null) {
            handleGameOver(win)
            return
        }

        if (engine.isBoardFull()) {
            handleGameDraw()
            return
        }

        currentTurn = currentTurn.opponent()
        gameState = ConnectFourGameState.Playing(currentTurn)
        updateTurnPresentation()

        if (gameMode == ConnectFourGameMode.VS_AI && currentTurn == cpuPiece) {
            performCpuTurn()
        }
    }

    /**
     * Ejecuta el turno de la IA con retardo táctico realista (850ms).
     */
    private fun performCpuTurn() {
        cpuJob?.cancel()
        isCpuThinking = true
        updateTurnPresentation()

        cpuJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(850L.milliseconds)

            if (!isActive || !isGameActive || gameState !is ConnectFourGameState.Playing || currentTurn != cpuPiece) {
                isCpuThinking = false
                return@launch
            }

            val cpuCol = ConnectFourAI.getNextMove(engine, difficulty, cpuPiece)
            isCpuThinking = false

            if (cpuCol != null && isGameActive && gameState is ConnectFourGameState.Playing) {
                val targetRow = engine.getLowestAvailableRow(cpuCol)
                if (targetRow != null) {
                    executeMove(cpuCol, targetRow, cpuPiece)
                }
            }
        }
    }

    /**
     * Gestiona el fin de partida por victoria.
     */
    private fun handleGameOver(result: ConnectFourWinningResult) {
        gameState = ConnectFourGameState.Won(result)
        isGameActive = false
        triggerWinVibration()

        if (binding.btnAbandonGame.isVisible) {
            GameAnimations.animateFadeOut(binding.btnAbandonGame)
        }

        if (gameMode == ConnectFourGameMode.VS_AI) {
            if (result.winner == humanPiece) {
                val updated = scoreManager.recordPlayer1Win(gameMode, difficulty)
                binding.tvScoreX.text = updated.player1Wins.toString()
                GameAnimations.animateScoreValueIncrement(binding.tvScoreX)

                binding.tvOverlayTitle.text = getString(R.string.overlay_victory)
                binding.tvOverlaySubtitle.text = getString(R.string.c4_overlay_desc_winner_yellow)
            } else {
                val updated = scoreManager.recordPlayer2Win(gameMode, difficulty)
                binding.tvScoreO.text = updated.player2Wins.toString()
                GameAnimations.animateScoreValueIncrement(binding.tvScoreO)

                binding.tvOverlayTitle.text = getString(R.string.overlay_defeat)
                binding.tvOverlaySubtitle.text = getString(R.string.overlay_desc_winner_cpu)
            }
        } else {
            if (result.winner == player1Piece) {
                val updated = scoreManager.recordPlayer1Win(gameMode, difficulty)
                binding.tvScoreX.text = updated.player1Wins.toString()
                GameAnimations.animateScoreValueIncrement(binding.tvScoreX)

                binding.tvOverlayTitle.text = getString(R.string.overlay_victory)
                binding.tvOverlaySubtitle.text = getString(R.string.overlay_desc_winner_player_1)
            } else {
                val updated = scoreManager.recordPlayer2Win(gameMode, difficulty)
                binding.tvScoreO.text = updated.player2Wins.toString()
                GameAnimations.animateScoreValueIncrement(binding.tvScoreO)

                binding.tvOverlayTitle.text = getString(R.string.overlay_victory)
                binding.tvOverlaySubtitle.text = getString(R.string.overlay_desc_winner_player_2)
            }
        }

        updateTurnPresentation()

        // Resaltar con animación de pulso las 4 fichas ganadoras
        val winningViews = result.winningCells.map { slotViews[it.row][it.col] }
        animateWinningPieces(winningViews)

        lifecycleScope.launch {
            delay(500L.milliseconds)
            if (gameState is ConnectFourGameState.Won && _binding != null) {
                GameAnimations.animateOverlayIn(binding.layoutBoardOverlay, binding.cardOverlayContent)
            }
        }
    }

    /**
     * Anima las 4 fichas ganadoras con un efecto continuo de escala y resplandor.
     */
    private fun animateWinningPieces(views: List<ImageView>) {
        for (view in views) {
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.22f, 1f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.22f, 1f)
            val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.7f, 1f)
            ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha).apply {
                duration = 600L
                repeatCount = 3
                start()
            }
        }
    }

    /**
     * Gestiona el fin de partida por empate.
     */
    private fun handleGameDraw() {
        gameState = ConnectFourGameState.Draw
        isGameActive = false
        val updated = scoreManager.recordDraw(gameMode, difficulty)
        binding.tvScoreDraws.text = updated.draws.toString()
        GameAnimations.animateScoreValueIncrement(binding.tvScoreDraws)

        if (binding.btnAbandonGame.isVisible) {
            GameAnimations.animateFadeOut(binding.btnAbandonGame)
        }

        binding.tvOverlayTitle.text = getString(R.string.overlay_draw)
        binding.tvOverlaySubtitle.text = getString(R.string.overlay_desc_draw)

        updateTurnPresentation()

        lifecycleScope.launch {
            delay(400L.milliseconds)
            if (gameState is ConnectFourGameState.Draw && _binding != null) {
                GameAnimations.animateOverlayIn(binding.layoutBoardOverlay, binding.cardOverlayContent)
            }
        }
    }

    /**
     * Actualiza el badge vertical indicador de turno.
     */
    private fun updateTurnPresentation() {
        val context = context ?: return
        val density = resources.displayMetrics.density
        val colorYellow = ContextCompat.getColor(context, R.color.c4_yellow)
        val colorRed = ContextCompat.getColor(context, R.color.c4_red)
        val strokeDefault = ContextCompat.getColor(context, R.color.cell_stroke)
        val colorMuted = ContextCompat.getColor(context, R.color.text_muted)
        val colorTextPrimary = ContextCompat.getColor(context, R.color.text_primary)

        when (val state = gameState) {
            is ConnectFourGameState.Idle -> {
                binding.cardTurnBadge.strokeColor = strokeDefault
                binding.tvTurnLabel.text = getString(R.string.label_status)
                binding.tvTurnLabel.setTextColor(colorMuted)
                binding.tvStatus.text = getString(R.string.c4_status_ready)
                binding.tvStatus.setTextColor(colorTextPrimary)

                binding.cardScoreX.strokeColor = strokeDefault
                binding.cardScoreX.strokeWidth = (1f * density).toInt()
                binding.cardScoreO.strokeColor = strokeDefault
                binding.cardScoreO.strokeWidth = (1f * density).toInt()
            }
            is ConnectFourGameState.Playing -> {
                binding.tvTurnLabel.text = getString(R.string.label_turn_of)
                binding.tvTurnLabel.setTextColor(colorMuted)

                val activeColor = if (state.currentTurn == ConnectFourPiece.YELLOW) colorYellow else colorRed
                binding.cardTurnBadge.strokeColor = activeColor

                val statusText = if (gameMode == ConnectFourGameMode.VS_AI) {
                    if (state.currentTurn == humanPiece) getString(R.string.score_label_you) else getString(R.string.turn_cpu)
                } else {
                    if (state.currentTurn == player1Piece) getString(R.string.turn_player_1) else getString(R.string.turn_player_2)
                }
                binding.tvStatus.text = statusText
                binding.tvStatus.setTextColor(activeColor)

                val isPlayer1Turn = if (gameMode == ConnectFourGameMode.VS_AI) state.currentTurn == humanPiece else state.currentTurn == player1Piece
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
            is ConnectFourGameState.Won -> {
                val winColor = if (state.winningResult.winner == ConnectFourPiece.YELLOW) colorYellow else colorRed
                binding.cardTurnBadge.strokeColor = ContextCompat.getColor(context, R.color.win_glow)
                binding.tvTurnLabel.text = getString(R.string.label_result)
                binding.tvTurnLabel.setTextColor(ContextCompat.getColor(context, R.color.win_glow))
                val winText = if (gameMode == ConnectFourGameMode.VS_AI) {
                    if (state.winningResult.winner == humanPiece) getString(R.string.status_winner_you) else getString(R.string.status_winner_cpu)
                } else {
                    if (state.winningResult.winner == player1Piece) getString(R.string.status_winner_player_1) else getString(R.string.status_winner_player_2)
                }
                binding.tvStatus.text = winText
                binding.tvStatus.setTextColor(winColor)
            }
            is ConnectFourGameState.Draw -> {
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
     * Alterna la visibilidad entre los controles y el botón de abandonar partida, y oculta/muestra la barra de menú.
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

                    if (binding.layoutControls.isVisible) {
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

                    if (binding.btnAbandonGame.isVisible) {
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
     * Prepara una nueva partida reiniciando el motor de Conecta 4 y el tablero visual.
     */
    private fun prepareFreshGame(showControls: Boolean) {
        cpuJob?.cancel()
        cpuJob = null
        engine.reset()
        isCpuThinking = false
        isDroppingPiece = false
        currentTurn = ConnectFourPiece.YELLOW

        if (gameMode == ConnectFourGameMode.VS_AI) {
            when (playerChoice) {
                ConnectFourSymbolChoice.YELLOW -> {
                    humanPiece = ConnectFourPiece.YELLOW
                    cpuPiece = ConnectFourPiece.RED
                }
                ConnectFourSymbolChoice.RED -> {
                    humanPiece = ConnectFourPiece.RED
                    cpuPiece = ConnectFourPiece.YELLOW
                }
                ConnectFourSymbolChoice.RANDOM -> {
                    if (!showControls) {
                        if (Random.nextBoolean()) {
                            humanPiece = ConnectFourPiece.YELLOW
                            cpuPiece = ConnectFourPiece.RED
                        } else {
                            humanPiece = ConnectFourPiece.RED
                            cpuPiece = ConnectFourPiece.YELLOW
                        }
                    }
                }
            }
        } else {
            if (!showControls) {
                if (Random.nextBoolean()) {
                    player1Piece = ConnectFourPiece.YELLOW
                    player2Piece = ConnectFourPiece.RED
                } else {
                    player1Piece = ConnectFourPiece.RED
                    player2Piece = ConnectFourPiece.YELLOW
                }
            }
        }

        gameState = if (showControls) ConnectFourGameState.Idle else ConnectFourGameState.Playing(currentTurn)

        setGameActiveState(active = !showControls, animated = true)
        updateScoreboardColors()

        binding.layoutBoardOverlay.visibility = View.GONE

        // Limpiar todas las ranuras al estado vacío inicial
        for (r in 0 until ConnectFourEngine.ROWS) {
            for (c in 0 until ConnectFourEngine.COLS) {
                slotViews[r][c].setImageResource(R.drawable.bg_c4_slot)
                slotViews[r][c].scaleX = 1f
                slotViews[r][c].scaleY = 1f
                slotViews[r][c].alpha = 1f
                slotViews[r][c].translationY = 0f
            }
        }

        updateTurnPresentation()

        if (!showControls && gameMode == ConnectFourGameMode.VS_AI && cpuPiece == ConnectFourPiece.YELLOW) {
            performCpuTurn()
        }
    }

    /**
     * Dispara una vibración táctil suave (25ms).
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
     * Dispara una vibración festiva al ganar.
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // 1. Guardar estado de las 42 ranuras del tablero 6x7
        val boardArray = Array(ConnectFourEngine.ROWS * ConnectFourEngine.COLS) { index ->
            val r = index / ConnectFourEngine.COLS
            val c = index % ConnectFourEngine.COLS
            engine.getCell(r, c)?.name ?: ""
        }
        outState.putStringArray(KEY_BOARD, boardArray)

        // 2. Guardar opciones de configuración
        outState.putString(KEY_GAME_MODE, gameMode.name)
        outState.putString(KEY_DIFFICULTY, difficulty.name)
        outState.putString(KEY_PLAYER_CHOICE, playerChoice.name)
        outState.putString(KEY_HUMAN_PIECE, humanPiece.name)
        outState.putString(KEY_CPU_PIECE, cpuPiece.name)
        outState.putString(KEY_PLAYER1_PIECE, player1Piece.name)
        outState.putString(KEY_PLAYER2_PIECE, player2Piece.name)
        outState.putString(KEY_CURRENT_TURN, currentTurn.name)

        // 3. Guardar estado activo y de simulación
        outState.putBoolean(KEY_IS_GAME_ACTIVE, isGameActive)
        outState.putBoolean(KEY_IS_CPU_THINKING, isCpuThinking)

        // 4. Guardar estado de finalización si corresponde
        when (val state = gameState) {
            is ConnectFourGameState.Won -> {
                outState.putString(KEY_GAME_STATE_TYPE, "WIN")
                outState.putString(KEY_WINNER_PIECE, state.winningResult.winner.name)
                val rowIndices = IntArray(state.winningResult.winningCells.size) { i -> state.winningResult.winningCells[i].row }
                val colIndices = IntArray(state.winningResult.winningCells.size) { i -> state.winningResult.winningCells[i].col }
                outState.putIntArray(KEY_WINNING_CELLS_ROWS, rowIndices)
                outState.putIntArray(KEY_WINNING_CELLS_COLS, colIndices)
            }
            is ConnectFourGameState.Draw -> {
                outState.putString(KEY_GAME_STATE_TYPE, "DRAW")
            }
            else -> {
                outState.putString(KEY_GAME_STATE_TYPE, "ACTIVE")
            }
        }
    }

    /**
     * Reconstruye íntegramente el estado del juego tras una rotación de pantalla.
     */
    private fun restoreSavedGameState(savedInstanceState: Bundle) {
        val boardArray = savedInstanceState.getStringArray(KEY_BOARD) ?: return

        // 1. Restaurar configuraciones
        gameMode = savedInstanceState.getString(KEY_GAME_MODE)?.let {
            runCatching { ConnectFourGameMode.valueOf(it) }.getOrDefault(ConnectFourGameMode.VS_AI)
        } ?: ConnectFourGameMode.VS_AI

        difficulty = savedInstanceState.getString(KEY_DIFFICULTY)?.let {
            runCatching { ConnectFourDifficulty.valueOf(it) }.getOrDefault(ConnectFourDifficulty.MEDIUM)
        } ?: ConnectFourDifficulty.MEDIUM

        playerChoice = savedInstanceState.getString(KEY_PLAYER_CHOICE)?.let {
            runCatching { ConnectFourSymbolChoice.valueOf(it) }.getOrDefault(ConnectFourSymbolChoice.YELLOW)
        } ?: ConnectFourSymbolChoice.YELLOW

        humanPiece = savedInstanceState.getString(KEY_HUMAN_PIECE)?.let {
            runCatching { ConnectFourPiece.valueOf(it) }.getOrDefault(ConnectFourPiece.YELLOW)
        } ?: ConnectFourPiece.YELLOW

        cpuPiece = savedInstanceState.getString(KEY_CPU_PIECE)?.let {
            runCatching { ConnectFourPiece.valueOf(it) }.getOrDefault(ConnectFourPiece.RED)
        } ?: ConnectFourPiece.RED

        player1Piece = savedInstanceState.getString(KEY_PLAYER1_PIECE)?.let {
            runCatching { ConnectFourPiece.valueOf(it) }.getOrDefault(ConnectFourPiece.YELLOW)
        } ?: ConnectFourPiece.YELLOW

        player2Piece = savedInstanceState.getString(KEY_PLAYER2_PIECE)?.let {
            runCatching { ConnectFourPiece.valueOf(it) }.getOrDefault(ConnectFourPiece.RED)
        } ?: ConnectFourPiece.RED

        currentTurn = savedInstanceState.getString(KEY_CURRENT_TURN)?.let {
            runCatching { ConnectFourPiece.valueOf(it) }.getOrDefault(ConnectFourPiece.YELLOW)
        } ?: ConnectFourPiece.YELLOW

        isGameActive = savedInstanceState.getBoolean(KEY_IS_GAME_ACTIVE, false)
        isCpuThinking = savedInstanceState.getBoolean(KEY_IS_CPU_THINKING, false)

        // 2. Restaurar celdas en el motor y en las vistas de ranuras
        engine.reset()
        for (r in 0 until ConnectFourEngine.ROWS) {
            for (c in 0 until ConnectFourEngine.COLS) {
                val pieceName = boardArray[r * ConnectFourEngine.COLS + c]
                val piece = if (pieceName.isNotEmpty()) {
                    runCatching { ConnectFourPiece.valueOf(pieceName) }.getOrNull()
                } else null

                engine.setCell(r, c, piece)
                when (piece) {
                    ConnectFourPiece.YELLOW -> slotViews[r][c].setImageResource(R.drawable.bg_c4_piece_yellow)
                    ConnectFourPiece.RED -> slotViews[r][c].setImageResource(R.drawable.bg_c4_piece_red)
                    null -> slotViews[r][c].setImageResource(R.drawable.bg_c4_slot)
                }
                slotViews[r][c].translationY = 0f
            }
        }

        // 3. Sincronizar botones de los selectores
        when (gameMode) {
            ConnectFourGameMode.VS_AI -> {
                binding.toggleGameMode.check(R.id.btnModeVsAi)
                binding.togglePlayerSymbol.visibility = View.VISIBLE
                binding.toggleDifficulty.visibility = View.VISIBLE
            }
            ConnectFourGameMode.TWO_PLAYERS -> {
                binding.toggleGameMode.check(R.id.btnModeTwoPlayers)
                binding.togglePlayerSymbol.visibility = View.GONE
                binding.toggleDifficulty.visibility = View.GONE
            }
        }

        when (playerChoice) {
            ConnectFourSymbolChoice.YELLOW -> binding.togglePlayerSymbol.check(R.id.btnSymbolX)
            ConnectFourSymbolChoice.RED -> binding.togglePlayerSymbol.check(R.id.btnSymbolO)
            ConnectFourSymbolChoice.RANDOM -> binding.togglePlayerSymbol.check(R.id.btnSymbolRandom)
        }

        when (difficulty) {
            ConnectFourDifficulty.EASY -> binding.toggleDifficulty.check(R.id.btnDiffEasy)
            ConnectFourDifficulty.MEDIUM -> binding.toggleDifficulty.check(R.id.btnDiffMedium)
            ConnectFourDifficulty.HARD -> binding.toggleDifficulty.check(R.id.btnDiffHard)
        }

        updateScoreboardUI()

        // 4. Restaurar estado de fin de partida o partida en curso
        val stateType = savedInstanceState.getString(KEY_GAME_STATE_TYPE, "ACTIVE")
        when (stateType) {
            "WIN" -> {
                val winnerName = savedInstanceState.getString(KEY_WINNER_PIECE, ConnectFourPiece.YELLOW.name)
                val winnerPiece = runCatching { ConnectFourPiece.valueOf(winnerName) }.getOrDefault(ConnectFourPiece.YELLOW)
                val rows = savedInstanceState.getIntArray(KEY_WINNING_CELLS_ROWS) ?: intArrayOf()
                val cols = savedInstanceState.getIntArray(KEY_WINNING_CELLS_COLS) ?: intArrayOf()
                val cells = rows.indices.map { i -> com.example.gamehub.games.connectfour.model.ConnectFourCell(rows[i], cols[i]) }
                val winningResult = ConnectFourWinningResult(
                    winnerPiece,
                    cells,
                    com.example.gamehub.games.connectfour.model.ConnectFourLineType.HORIZONTAL
                )
                gameState = ConnectFourGameState.Won(winningResult)
                binding.layoutControls.visibility = View.GONE
                binding.btnAbandonGame.visibility = View.GONE
                binding.layoutBoardOverlay.visibility = View.VISIBLE
                binding.cardOverlayContent.scaleX = 1f
                binding.cardOverlayContent.scaleY = 1f
                binding.cardOverlayContent.alpha = 1f
                updateTurnPresentation()
            }
            "DRAW" -> {
                gameState = ConnectFourGameState.Draw
                binding.layoutControls.visibility = View.GONE
                binding.btnAbandonGame.visibility = View.GONE
                binding.layoutBoardOverlay.visibility = View.VISIBLE
                binding.cardOverlayContent.scaleX = 1f
                binding.cardOverlayContent.scaleY = 1f
                binding.cardOverlayContent.alpha = 1f
                updateTurnPresentation()
            }
            else -> {
                if (isGameActive) {
                    gameState = ConnectFourGameState.Playing(currentTurn)
                    binding.layoutControls.visibility = View.GONE
                    binding.btnAbandonGame.visibility = View.VISIBLE
                    binding.layoutBoardOverlay.visibility = View.GONE
                    updateTurnPresentation()

                    if (isCpuThinking && gameMode == ConnectFourGameMode.VS_AI && currentTurn == cpuPiece && gameState is ConnectFourGameState.Playing) {
                        performCpuTurn()
                    }
                } else {
                    prepareFreshGame(showControls = true)
                }
            }
        }
    }

    companion object {
        private const val KEY_BOARD = "c4_key_board"
        private const val KEY_GAME_MODE = "c4_key_game_mode"
        private const val KEY_DIFFICULTY = "c4_key_difficulty"
        private const val KEY_PLAYER_CHOICE = "c4_key_player_choice"
        private const val KEY_HUMAN_PIECE = "c4_key_human_piece"
        private const val KEY_CPU_PIECE = "c4_key_cpu_piece"
        private const val KEY_PLAYER1_PIECE = "c4_key_player1_piece"
        private const val KEY_PLAYER2_PIECE = "c4_key_player2_piece"
        private const val KEY_CURRENT_TURN = "c4_key_current_turn"
        private const val KEY_IS_GAME_ACTIVE = "c4_key_is_game_active"
        private const val KEY_IS_CPU_THINKING = "c4_key_is_cpu_thinking"
        private const val KEY_GAME_STATE_TYPE = "c4_key_game_state_type"
        private const val KEY_WINNER_PIECE = "c4_key_winner_piece"
        private const val KEY_WINNING_CELLS_ROWS = "c4_key_winning_cells_rows"
        private const val KEY_WINNING_CELLS_COLS = "c4_key_winning_cells_cols"
    }
}
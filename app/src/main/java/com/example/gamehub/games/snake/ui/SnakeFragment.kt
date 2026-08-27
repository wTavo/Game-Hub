package com.example.gamehub.games.snake.ui

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.gamehub.MainActivity
import com.example.gamehub.R
import com.example.gamehub.core.animations.GameAnimations
import com.example.gamehub.databinding.FragmentSnakeBinding
import com.example.gamehub.games.snake.data.SnakeScoreManager
import com.example.gamehub.games.snake.domain.SnakeEngine
import com.example.gamehub.games.snake.model.SnakeControlType
import com.example.gamehub.games.snake.model.SnakeDifficulty
import com.example.gamehub.games.snake.model.SnakeDirection
import com.example.gamehub.games.snake.model.SnakeFood
import com.example.gamehub.games.snake.model.SnakeGameMode
import com.example.gamehub.games.snake.model.SnakeGameState
import com.example.gamehub.games.snake.model.SnakePosition
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import kotlin.time.Duration.Companion.milliseconds

/**
 * Controlador de interfaz y flujo de ejecución para el juego Gusanito Arcade (Snake).
 * Gestiona el bucle de simulación con aceleración progresiva, esquemas de control seleccionables
 * (Palanca Arcade de 4 Vías, Botones D-Pad y Gestos Táctiles), soporte de tema claro/oscuro y bloqueo del menú inferior.
 */
class SnakeFragment : Fragment() {

    private var _binding: FragmentSnakeBinding? = null
    private val binding get() = _binding!!

    private lateinit var scoreManager: SnakeScoreManager
    private val engine = SnakeEngine()

    // Configuración y estados
    private var gameMode: SnakeGameMode = SnakeGameMode.CLASSIC_WALLS
    private var difficulty: SnakeDifficulty = SnakeDifficulty.MEDIUM
    private var controlType: SnakeControlType = SnakeControlType.JOYSTICK
    private var gameState: SnakeGameState = SnakeGameState.Idle
    private var gameLoopJob: Job? = null
    private var controlsFlyoutPopup: PopupWindow? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSnakeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scoreManager = SnakeScoreManager(requireContext())
        controlType = scoreManager.getSavedControlType()

        binding.snakeBoardView.updateThemeColors()
        binding.snakeJoystick.updateColors()
        binding.snakeDpadView.updateColors()

        setupGestureDetector()
        setupListeners()
        updateHighScoreUI()

        if (savedInstanceState != null) {
            restoreSavedGameState(savedInstanceState)
        } else {
            prepareFreshGame(showControls = true)
        }
    }

    override fun onPause() {
        super.onPause()
        if (gameState is SnakeGameState.Running) {
            pauseGame()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        controlsFlyoutPopup?.dismiss()
        controlsFlyoutPopup = null
        gameLoopJob?.cancel()
        _binding = null
    }

    /**
     * Configura el detector de gestos táctiles continuos (Drag-to-Steer en tiempo real).
     * Permite que el gusanito siga la dirección del dedo de forma continua mientras se mantenga presionado
     * y se deslice en cualquier rumbo sin necesidad de soltar la pantalla.
     */
    private fun setupGestureDetector() {
        val continuousTouchListener = createContinuousGestureListener()
        binding.snakeBoardView.setOnTouchListener(continuousTouchListener)
        binding.layoutSnakeGesturesHint.setOnTouchListener(continuousTouchListener)
    }

    /**
     * Crea un listener táctil de máxima respuesta y fiabilidad para el Touchpad y tablero.
     * Captura con 100% de eficacia tanto deslizamientos continuos manteniendo el dedo presionado
     * como swipes rápidos al soltar, garantizando que cada giro intencionado se registre al instante.
     */
    private fun createContinuousGestureListener(): View.OnTouchListener {
        var startX = 0f
        var startY = 0f
        var isDragging = false
        val density = resources.displayMetrics.density
        val dragThresholdPx = 28f * density // ~0.7 cm de desplazamiento ágil

        return View.OnTouchListener { _, event ->
            if (controlType != SnakeControlType.GESTURES) return@OnTouchListener false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    isDragging = true
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        val dx = event.x - startX
                        val dy = event.y - startY
                        val absDx = abs(dx)
                        val absDy = abs(dy)

                        if (absDx >= dragThresholdPx || absDy >= dragThresholdPx) {
                            val candidateDir = if (absDx >= absDy) {
                                if (dx > 0) SnakeDirection.RIGHT else SnakeDirection.LEFT
                            } else {
                                if (dy > 0) SnakeDirection.DOWN else SnakeDirection.UP
                            }

                            val accepted = if (gameState is SnakeGameState.Running) {
                                engine.changeDirection(candidateDir)
                            } else false

                            // Reanclamos el origen cada vez que se supera el umbral
                            startX = event.x
                            startY = event.y
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        val dx = event.x - startX
                        val dy = event.y - startY
                        val absDx = abs(dx)
                        val absDy = abs(dy)

                        // Si fue un swipe corto y rápido al soltar
                        if (absDx >= dragThresholdPx * 0.7f || absDy >= dragThresholdPx * 0.7f) {
                            val candidateDir = if (absDx >= absDy) {
                                if (dx > 0) SnakeDirection.RIGHT else SnakeDirection.LEFT
                            } else {
                                if (dy > 0) SnakeDirection.DOWN else SnakeDirection.UP
                            }
                            handleDirectionInput(candidateDir)
                        }
                    }
                    isDragging = false
                    true
                }

                else -> false
            }
        }
    }

    /**
     * Asigna los escuchadores de evento para los selectores de modo, controles, palanca, D-Pad y botones.
     */
    private fun setupListeners() {
        // Palanca Arcade de 4 Vías (Restricción física ortogonal)
        binding.snakeJoystick.onDirectionChanged = { dir ->
            handleDirectionInput(dir)
        }

        // Cruceta Arcade D-Pad con botones pegados y arrastre continuo sin soltar el dedo
        binding.snakeDpadView.onDirectionChanged = { dir ->
            handleDirectionInput(dir)
        }

        // Botón para Cambiar los Controles: Despliega la pestañita flotante nativa anclada al botón
        binding.btnOpenControlSettings.setOnClickListener {
            showControlsFlyout()
        }

        // Selector de Modo de Paredes (Con Bordes / Sin Bordes)
        binding.toggleSnakeMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                gameMode = when (checkedId) {
                    R.id.btnModeFree -> SnakeGameMode.FREE_WRAP
                    else -> SnakeGameMode.CLASSIC_WALLS
                }
                updateHighScoreUI()
                prepareFreshGame(showControls = true)
            }
        }

        // Selector de Dificultad (Fácil / Medio / Difícil)
        binding.toggleDifficulty.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                difficulty = when (checkedId) {
                    R.id.btnDiffEasy -> SnakeDifficulty.EASY
                    R.id.btnDiffHard -> SnakeDifficulty.HARD
                    else -> SnakeDifficulty.MEDIUM
                }
                updateHighScoreUI()
                prepareFreshGame(showControls = true)
            }
        }

        // Botón Iniciar Partida (único punto de inicio en reposo)
        binding.btnStartSnakeGame.setOnClickListener {
            startGame()
        }

        // Botón Pausa / Reanudar
        binding.btnPauseGame.setOnClickListener {
            if (gameState is SnakeGameState.Running) {
                pauseGame()
            } else if (gameState is SnakeGameState.Paused) {
                resumeGame()
            }
        }

        // Botón Abandonar Partida
        binding.btnAbandonGame.setOnClickListener {
            stopGameLoop()
            prepareFreshGame(showControls = true)
        }

        // Botón Jugar de Nuevo (en el overlay central de fin de partida)
        binding.btnPlayAgain.setOnClickListener {
            GameAnimations.animateOverlayOut(binding.layoutBoardOverlay) {
                prepareFreshGame(showControls = false)
                startGame()
            }
        }

        // Botón Terminar Juego (en el overlay central de fin de partida)
        binding.btnFinishGame.setOnClickListener {
            GameAnimations.animateOverlayOut(binding.layoutBoardOverlay) {
                prepareFreshGame(showControls = true)
            }
        }
    }

    /**
     * Procesa la entrada direccional de la palanca de 4 vías o de los gestos táctiles.
     * Solo tiene efecto cuando la partida está en curso (Running).
     */
    private fun handleDirectionInput(newDirection: SnakeDirection) {
        if (gameState is SnakeGameState.Running) {
            engine.changeDirection(newDirection)
        }
    }

    /**
     * Inicia la partida activa, ocultando la barra de navegación y habilitando los controles.
     */
    private fun startGame() {
        if (gameState is SnakeGameState.Running) return

        gameState = SnakeGameState.Running
        setInGameControlsVisible(true)
        restoreInGameButtonParams()
        binding.btnPauseGame.visibility = View.VISIBLE
        binding.btnPauseGame.text = getString(R.string.snake_btn_pause)
        binding.btnPauseGame.setIconResource(R.drawable.ic_pause)
        binding.btnAbandonGame.visibility = View.VISIBLE
        binding.btnAbandonGame.text = getString(R.string.action_abandon)
        updateTurnPresentation()

        (activity as? MainActivity)?.setBottomNavigationVisible(false, animated = true)

        startGameLoop()
    }

    /**
     * Restaura los parámetros de diseño y márgenes simétricos para la fila de botones en juego.
     */
    private fun restoreInGameButtonParams() {
        val margin = (5 * resources.displayMetrics.density).toInt()

        val pauseParams = binding.btnPauseGame.layoutParams as LinearLayout.LayoutParams
        pauseParams.marginStart = 0
        pauseParams.marginEnd = margin
        pauseParams.weight = 1f
        pauseParams.width = 0
        binding.btnPauseGame.layoutParams = pauseParams

        val abandonParams = binding.btnAbandonGame.layoutParams as LinearLayout.LayoutParams
        abandonParams.marginStart = margin
        abandonParams.marginEnd = 0
        abandonParams.weight = 1f
        abandonParams.width = 0
        binding.btnAbandonGame.layoutParams = abandonParams
    }

    /**
     * Pausa la ejecución de la partida manteniendo el menú inferior oculto.
     */
    private fun pauseGame() {
        gameState = SnakeGameState.Paused
        stopGameLoop()
        updateTurnPresentation()

        binding.btnPauseGame.text = getString(R.string.snake_btn_resume)
        binding.btnPauseGame.setIconResource(R.drawable.ic_play)
        binding.btnAbandonGame.text = getString(R.string.action_abandon)
    }

    /**
     * Reanuda la partida desde el estado de pausa.
     */
    private fun resumeGame() {
        gameState = SnakeGameState.Running
        updateTurnPresentation()

        binding.btnPauseGame.text = getString(R.string.snake_btn_pause)
        binding.btnPauseGame.setIconResource(R.drawable.ic_pause)
        binding.btnAbandonGame.text = getString(R.string.action_abandon)

        startGameLoop()
    }

    /**
     * Calcula la velocidad dinámica con aceleración progresiva según las manzanas comidas.
     */
    private fun calculateCurrentTickDelay(applesEaten: Int, difficulty: SnakeDifficulty): Long {
        return when (difficulty) {
            SnakeDifficulty.EASY -> max(110L, 210L - (applesEaten * 3L))
            SnakeDifficulty.MEDIUM -> max(75L, 145L - (applesEaten * 2.5).toLong())
            SnakeDifficulty.HARD -> max(48L, 90L - (applesEaten * 1.8).toLong())
        }
    }

    /**
     * Bucle continuo de avance (Game Loop) con aceleración progresiva y renderizado en Canvas.
     */
    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive && gameState is SnakeGameState.Running) {
                val currentDelay = calculateCurrentTickDelay(engine.applesEaten, difficulty)
                delay(currentDelay.milliseconds)

                val result = engine.step()

                // Renderizado en Canvas a 60/120 FPS
                binding.snakeBoardView.renderState(
                    engine.snake,
                    engine.currentDirection,
                    engine.food,
                    engine.gridWidth,
                    engine.gridHeight
                )

                when (result) {
                    SnakeEngine.StepResult.MOVED -> {
                        // Avance regular
                    }
                    SnakeEngine.StepResult.ATE_FOOD -> {
                        binding.tvScoreCurrent.text = engine.score.toString()
                        binding.tvScoreApples.text = engine.applesEaten.toString()
                        GameAnimations.animateScoreValueIncrement(binding.tvScoreCurrent)
                        triggerHapticFeedback(isBonus = false)
                    }
                    SnakeEngine.StepResult.ATE_GOLDEN_FOOD -> {
                        binding.tvScoreCurrent.text = engine.score.toString()
                        binding.tvScoreApples.text = engine.applesEaten.toString()
                        GameAnimations.animateScoreValueIncrement(binding.tvScoreCurrent)
                        triggerHapticFeedback(isBonus = true)
                    }
                    SnakeEngine.StepResult.CRASHED -> {
                        handleGameOver()
                        break
                    }
                }
            }
        }
    }

    /**
     * Detiene el bucle de simulación.
     */
    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    /**
     * Gestiona el fin de partida por colisión.
     */
    private fun handleGameOver() {
        stopGameLoop()
        val isNewHigh = scoreManager.submitScore(gameMode, difficulty, engine.score)
        gameState = SnakeGameState.GameOver(engine.score, isNewHigh)

        // Ocultar fila de botones de juego y mostrar overlay con Jugar de nuevo y Terminar juego
        binding.inGameButtonsRow.visibility = View.GONE

        triggerCrashVibration()
        updateHighScoreUI()
        updateTurnPresentation()

        if (isNewHigh) {
            binding.tvOverlayTitle.text = getString(R.string.snake_overlay_new_record)
            binding.tvOverlayTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.win_glow))
        } else {
            binding.tvOverlayTitle.text = getString(R.string.snake_overlay_game_over)
            binding.tvOverlayTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.player_o_color))
        }

        binding.tvOverlaySubtitle.text = getString(R.string.snake_overlay_desc_score, engine.score)

        viewLifecycleOwner.lifecycleScope.launch {
            delay(400L.milliseconds)
            if (_binding != null && gameState is SnakeGameState.GameOver) {
                GameAnimations.animateOverlayIn(binding.layoutBoardOverlay, binding.cardOverlayContent)
            }
        }
    }

    /**
     * Actualiza el marcador de récord máximo (*High Score*).
     */
    private fun updateHighScoreUI() {
        val high = scoreManager.getHighScore(gameMode, difficulty)
        binding.tvScoreHigh.text = high.toString()
    }

    /**
     * Actualiza el badge indicador de estado.
     */
    private fun updateTurnPresentation() {
        val context = context ?: return
        val strokeDefault = ContextCompat.getColor(context, R.color.cell_stroke)
        val colorPrimary = ContextCompat.getColor(context, R.color.text_primary)
        val colorMuted = ContextCompat.getColor(context, R.color.text_muted)
        val colorGreen = ContextCompat.getColor(context, R.color.win_glow)
        val colorAmber = ContextCompat.getColor(context, R.color.draw_color)
        val colorRed = ContextCompat.getColor(context, R.color.player_o_color)

        when (gameState) {
            is SnakeGameState.Idle -> {
                binding.cardTurnBadge.strokeColor = strokeDefault
                binding.tvTurnLabel.text = getString(R.string.label_status)
                binding.tvTurnLabel.setTextColor(colorMuted)
                binding.tvStatus.text = getString(R.string.snake_status_ready)
                binding.tvStatus.setTextColor(colorPrimary)
            }
            is SnakeGameState.Running -> {
                binding.cardTurnBadge.strokeColor = colorGreen
                binding.tvTurnLabel.text = getString(R.string.label_status)
                binding.tvTurnLabel.setTextColor(colorMuted)
                binding.tvStatus.text = getString(R.string.snake_status_playing)
                binding.tvStatus.setTextColor(colorGreen)
            }
            is SnakeGameState.Paused -> {
                binding.cardTurnBadge.strokeColor = colorAmber
                binding.tvTurnLabel.text = getString(R.string.label_status)
                binding.tvTurnLabel.setTextColor(colorAmber)
                binding.tvStatus.text = getString(R.string.snake_status_paused)
                binding.tvStatus.setTextColor(colorAmber)
            }
            is SnakeGameState.GameOver -> {
                binding.cardTurnBadge.strokeColor = colorRed
                binding.tvTurnLabel.text = getString(R.string.label_result)
                binding.tvTurnLabel.setTextColor(colorRed)
                binding.tvStatus.text = getString(R.string.snake_status_game_over)
                binding.tvStatus.setTextColor(colorRed)
            }
        }
    }

    /**
     * Alterna la visibilidad entre el panel de configuración en reposo y los controles de juego.
     */
    private fun setInGameControlsVisible(inGame: Boolean) {
        if (inGame) {
            updateInGameControlSchemeView()
            binding.btnOpenControlSettings.visibility = View.GONE
            if (binding.layoutInGameControls.visibility != View.VISIBLE) {
                binding.layoutInGameControls.alpha = 0f
                binding.layoutInGameControls.visibility = View.VISIBLE
                GameAnimations.animateFadeIn(binding.layoutInGameControls, duration = 200L)

                binding.inGameButtonsRow.alpha = 0f
                binding.inGameButtonsRow.visibility = View.VISIBLE
                GameAnimations.animateFadeIn(binding.inGameButtonsRow, duration = 200L)

                if (binding.layoutControls.isVisible) {
                    GameAnimations.animateFadeOut(binding.layoutControls, duration = 180L)
                }
            }
        } else {
            binding.btnOpenControlSettings.visibility = View.VISIBLE
            if (binding.layoutControls.visibility != View.VISIBLE) {
                binding.layoutControls.alpha = 0f
                binding.layoutControls.visibility = View.VISIBLE
                GameAnimations.animateFadeIn(binding.layoutControls, duration = 200L)

                if (binding.layoutInGameControls.isVisible) {
                    GameAnimations.animateFadeOut(binding.layoutInGameControls, duration = 180L)
                }
                if (binding.inGameButtonsRow.isVisible) {
                    GameAnimations.animateFadeOut(binding.inGameButtonsRow, duration = 180L)
                }
            }
        }
    }

    /**
     * Alterna la visibilidad interna del tipo de control seleccionado (Palanca, Cruceta D-Pad o Gestos).
     */
    private fun updateInGameControlSchemeView() {
        when (controlType) {
            SnakeControlType.JOYSTICK -> {
                binding.snakeJoystick.visibility = View.VISIBLE
                binding.snakeDpadView.visibility = View.GONE
                binding.layoutSnakeGesturesHint.visibility = View.GONE
            }
            SnakeControlType.BUTTONS -> {
                binding.snakeJoystick.visibility = View.GONE
                binding.snakeDpadView.visibility = View.VISIBLE
                binding.layoutSnakeGesturesHint.visibility = View.GONE
            }
            SnakeControlType.GESTURES -> {
                binding.snakeJoystick.visibility = View.GONE
                binding.snakeDpadView.visibility = View.GONE
                binding.layoutSnakeGesturesHint.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Despliega la pestañita flotante con vistas previas reales mediante PopupWindow nativo de Android.
     */
    private fun showControlsFlyout() {
        if (controlsFlyoutPopup?.isShowing == true) {
            controlsFlyoutPopup?.dismiss()
            return
        }

        val popupBinding = com.example.gamehub.databinding.LayoutSnakeControlsFlyoutBinding.inflate(layoutInflater)

        // Configurar vistas previas del popup
        popupBinding.popupJoystickPreview.updateColors()
        popupBinding.popupDpadPreview.updateColors()
        popupBinding.popupJoystickPreview.isEnabled = false
        popupBinding.popupDpadPreview.isEnabled = false

        // Función interna para refrescar bordes
        fun updatePopupSelection(type: SnakeControlType) {
            val activeColor = ContextCompat.getColor(requireContext(), R.color.color_primary)
            val defaultColor = ContextCompat.getColor(requireContext(), R.color.cell_stroke)
            val activeBg = ContextCompat.getColor(requireContext(), R.color.bg_card)
            val defaultBg = ContextCompat.getColor(requireContext(), R.color.bg_primary)

            val activeWidth = (3 * resources.displayMetrics.density).toInt()
            val defaultWidth = (1 * resources.displayMetrics.density).toInt()

            popupBinding.cardPopupJoystick.strokeColor = if (type == SnakeControlType.JOYSTICK) activeColor else defaultColor
            popupBinding.cardPopupJoystick.strokeWidth = if (type == SnakeControlType.JOYSTICK) activeWidth else defaultWidth
            popupBinding.cardPopupJoystick.setCardBackgroundColor(if (type == SnakeControlType.JOYSTICK) activeBg else defaultBg)

            popupBinding.cardPopupButtons.strokeColor = if (type == SnakeControlType.BUTTONS) activeColor else defaultColor
            popupBinding.cardPopupButtons.strokeWidth = if (type == SnakeControlType.BUTTONS) activeWidth else defaultWidth
            popupBinding.cardPopupButtons.setCardBackgroundColor(if (type == SnakeControlType.BUTTONS) activeBg else defaultBg)

            popupBinding.cardPopupGestures.strokeColor = if (type == SnakeControlType.GESTURES) activeColor else defaultColor
            popupBinding.cardPopupGestures.strokeWidth = if (type == SnakeControlType.GESTURES) activeWidth else defaultWidth
            popupBinding.cardPopupGestures.setCardBackgroundColor(if (type == SnakeControlType.GESTURES) activeBg else defaultBg)

            val mutedColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
            popupBinding.tvPopupGesturesLabel.setTextColor(if (type == SnakeControlType.GESTURES) activeColor else mutedColor)
            popupBinding.ivPopupGesturesIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                if (type == SnakeControlType.GESTURES) activeColor else mutedColor
            )
        }

        updatePopupSelection(controlType)

        // Clics directos e infalibles en cada botón
        popupBinding.cardPopupJoystick.setOnClickListener {
            controlType = SnakeControlType.JOYSTICK
            scoreManager.saveControlType(SnakeControlType.JOYSTICK)
            updatePopupSelection(SnakeControlType.JOYSTICK)
            updateInGameControlSchemeView()
        }

        popupBinding.cardPopupButtons.setOnClickListener {
            controlType = SnakeControlType.BUTTONS
            scoreManager.saveControlType(SnakeControlType.BUTTONS)
            updatePopupSelection(SnakeControlType.BUTTONS)
            updateInGameControlSchemeView()
        }

        popupBinding.cardPopupGestures.setOnClickListener {
            controlType = SnakeControlType.GESTURES
            scoreManager.saveControlType(SnakeControlType.GESTURES)
            updatePopupSelection(SnakeControlType.GESTURES)
            updateInGameControlSchemeView()
        }

        val popup = PopupWindow(
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
            elevation = 30 * resources.displayMetrics.density
            setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
            animationStyle = android.R.style.Animation_Dialog
        }

        controlsFlyoutPopup = popup

        popupBinding.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupBinding.root.measuredWidth
        val popupHeight = popupBinding.root.measuredHeight

        val btn = binding.btnOpenControlSettings
        val xOffset = -(popupWidth - btn.width)
        val yOffset = -(popupHeight + btn.height + (8 * resources.displayMetrics.density).toInt())

        popup.showAsDropDown(btn, xOffset, yOffset)
    }

    /**
     * Prepara una partida limpia en reposo reiniciando el motor y la vista Canvas.
     */
    private fun prepareFreshGame(showControls: Boolean) {
        stopGameLoop()
        engine.reset(gameMode)

        gameState = SnakeGameState.Idle
        setInGameControlsVisible(!showControls)

        binding.tvScoreCurrent.text = "0"
        binding.tvScoreApples.text = "0"
        restoreInGameButtonParams()
        binding.btnPauseGame.text = getString(R.string.snake_btn_pause)
        binding.btnPauseGame.setIconResource(R.drawable.ic_pause)
        binding.btnAbandonGame.text = getString(R.string.action_abandon)

        if (showControls) {
            binding.inGameButtonsRow.visibility = View.GONE
            (activity as? MainActivity)?.setBottomNavigationVisible(true, animated = true)
        }

        binding.layoutBoardOverlay.visibility = View.GONE

        binding.snakeBoardView.renderState(
            engine.snake,
            engine.currentDirection,
            engine.food,
            engine.gridWidth,
            engine.gridHeight
        )

        updateHighScoreUI()
        updateTurnPresentation()
    }

    /**
     * Dispara una vibración táctil corta al comer una manzana.
     */
    private fun triggerHapticFeedback(isBonus: Boolean) {
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
                    val duration = if (isBonus) 45L else 20L
                    it.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(20)
                }
            }
        } catch (_: Exception) {
            // Silencioso
        }
    }

    /**
     * Dispara una vibración al colisionar.
     */
    private fun triggerCrashVibration() {
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
                    it.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(120)
                }
            }
        } catch (_: Exception) {
            // Silencioso
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // 1. Guardar segmentos de la serpiente
        val snakeSegments = engine.snake
        val xCoords = IntArray(snakeSegments.size) { i -> snakeSegments[i].x }
        val yCoords = IntArray(snakeSegments.size) { i -> snakeSegments[i].y }
        outState.putIntArray(KEY_SNAKE_X, xCoords)
        outState.putIntArray(KEY_SNAKE_Y, yCoords)

        // 2. Guardar comida
        val food = engine.food
        if (food != null) {
            outState.putBoolean(KEY_HAS_FOOD, true)
            outState.putInt(KEY_FOOD_X, food.position.x)
            outState.putInt(KEY_FOOD_Y, food.position.y)
            outState.putBoolean(KEY_FOOD_IS_GOLDEN, food.isGolden)
        } else {
            outState.putBoolean(KEY_HAS_FOOD, false)
        }

        // 3. Guardar estado del motor
        outState.putString(KEY_DIRECTION, engine.currentDirection.name)
        outState.putInt(KEY_SCORE, engine.score)
        outState.putInt(KEY_APPLES, engine.applesEaten)
        outState.putBoolean(KEY_IS_GAME_OVER, engine.isGameOver)

        // 4. Guardar opciones de configuración
        outState.putString(KEY_GAME_MODE, gameMode.name)
        outState.putString(KEY_DIFFICULTY, difficulty.name)
        outState.putString(KEY_CONTROL_SCHEME, controlType.name)

        // 5. Guardar estado de la máquina de estados
        when (val state = gameState) {
            is SnakeGameState.Running -> outState.putString(KEY_GAME_STATE_TYPE, "RUNNING")
            is SnakeGameState.Paused -> outState.putString(KEY_GAME_STATE_TYPE, "PAUSED")
            is SnakeGameState.GameOver -> {
                outState.putString(KEY_GAME_STATE_TYPE, "GAME_OVER")
                outState.putBoolean(KEY_IS_NEW_HIGH, state.isNewRecord)
            }
            is SnakeGameState.Idle -> outState.putString(KEY_GAME_STATE_TYPE, "IDLE")
        }
    }

    /**
     * Restaura íntegramente el estado del juego tras una rotación de pantalla.
     */
    private fun restoreSavedGameState(savedInstanceState: Bundle) {
        val xCoords = savedInstanceState.getIntArray(KEY_SNAKE_X) ?: return
        val yCoords = savedInstanceState.getIntArray(KEY_SNAKE_Y) ?: return

        // 1. Reconstruir segmentos de la serpiente
        val snakeSegments = ArrayList<SnakePosition>()
        for (i in xCoords.indices) {
            snakeSegments.add(SnakePosition(xCoords[i], yCoords[i]))
        }

        // 2. Reconstruir comida
        val currentFood = if (savedInstanceState.getBoolean(KEY_HAS_FOOD, false)) {
            val fx = savedInstanceState.getInt(KEY_FOOD_X)
            val fy = savedInstanceState.getInt(KEY_FOOD_Y)
            val isGolden = savedInstanceState.getBoolean(KEY_FOOD_IS_GOLDEN, false)
            SnakeFood(position = SnakePosition(fx, fy), isGolden = isGolden)
        } else null

        // 3. Restaurar configuraciones y variables del motor
        gameMode = savedInstanceState.getString(KEY_GAME_MODE)?.let {
            runCatching { SnakeGameMode.valueOf(it) }.getOrDefault(SnakeGameMode.CLASSIC_WALLS)
        } ?: SnakeGameMode.CLASSIC_WALLS

        difficulty = savedInstanceState.getString(KEY_DIFFICULTY)?.let {
            runCatching { SnakeDifficulty.valueOf(it) }.getOrDefault(SnakeDifficulty.MEDIUM)
        } ?: SnakeDifficulty.MEDIUM

        controlType = savedInstanceState.getString(KEY_CONTROL_SCHEME)?.let {
            runCatching { SnakeControlType.valueOf(it) }.getOrDefault(SnakeControlType.JOYSTICK)
        } ?: SnakeControlType.JOYSTICK

        val direction = savedInstanceState.getString(KEY_DIRECTION)?.let {
            runCatching { SnakeDirection.valueOf(it) }.getOrDefault(SnakeDirection.RIGHT)
        } ?: SnakeDirection.RIGHT

        val score = savedInstanceState.getInt(KEY_SCORE, 0)
        val apples = savedInstanceState.getInt(KEY_APPLES, 0)
        val isGameOver = savedInstanceState.getBoolean(KEY_IS_GAME_OVER, false)

        engine.restoreState(
            snakeSegments = snakeSegments,
            direction = direction,
            currentFood = currentFood,
            currentScore = score,
            apples = apples,
            gameMode = gameMode,
            gameOver = isGameOver
        )

        // 4. Sincronizar botones de los selectores UI
        when (gameMode) {
            SnakeGameMode.CLASSIC_WALLS -> binding.toggleSnakeMode.check(R.id.btnModeClassic)
            SnakeGameMode.FREE_WRAP -> binding.toggleSnakeMode.check(R.id.btnModeFree)
        }

        when (difficulty) {
            SnakeDifficulty.EASY -> binding.toggleDifficulty.check(R.id.btnDiffEasy)
            SnakeDifficulty.MEDIUM -> binding.toggleDifficulty.check(R.id.btnDiffMedium)
            SnakeDifficulty.HARD -> binding.toggleDifficulty.check(R.id.btnDiffHard)
        }

        binding.tvScoreCurrent.text = score.toString()
        binding.tvScoreApples.text = apples.toString()
        updateHighScoreUI()

        binding.snakeBoardView.renderState(
            engine.snake,
            engine.currentDirection,
            engine.food,
            engine.gridWidth,
            engine.gridHeight
        )

        // 5. Restaurar estado de la partida
        val stateType = savedInstanceState.getString(KEY_GAME_STATE_TYPE, "IDLE")
        when (stateType) {
            "RUNNING" -> {
                gameState = SnakeGameState.Running
                setInGameControlsVisible(true)
                binding.btnPauseGame.visibility = View.VISIBLE
                binding.btnPauseGame.text = getString(R.string.snake_btn_pause)
                binding.btnPauseGame.setIconResource(R.drawable.ic_pause)
                binding.layoutBoardOverlay.visibility = View.GONE
                updateTurnPresentation()
                startGameLoop()
            }
            "PAUSED" -> {
                gameState = SnakeGameState.Paused
                setInGameControlsVisible(true)
                binding.btnPauseGame.visibility = View.VISIBLE
                binding.btnPauseGame.text = getString(R.string.snake_btn_resume)
                binding.btnPauseGame.setIconResource(R.drawable.ic_play)
                binding.layoutBoardOverlay.visibility = View.GONE
                updateTurnPresentation()
            }
            "GAME_OVER" -> {
                val isNewHigh = savedInstanceState.getBoolean(KEY_IS_NEW_HIGH, false)
                gameState = SnakeGameState.GameOver(score, isNewHigh)
                setInGameControlsVisible(false)
                binding.inGameButtonsRow.visibility = View.GONE
                binding.layoutBoardOverlay.visibility = View.VISIBLE
                binding.cardOverlayContent.scaleX = 1f
                binding.cardOverlayContent.scaleY = 1f
                binding.cardOverlayContent.alpha = 1f

                if (isNewHigh) {
                    binding.tvOverlayTitle.text = getString(R.string.snake_overlay_new_record)
                    binding.tvOverlayTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.win_glow))
                } else {
                    binding.tvOverlayTitle.text = getString(R.string.snake_overlay_game_over)
                    binding.tvOverlayTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.player_o_color))
                }
                binding.tvOverlaySubtitle.text = getString(R.string.snake_overlay_desc_score, score)
                updateTurnPresentation()
            }
            else -> {
                prepareFreshGame(showControls = true)
            }
        }
    }

    companion object {
        private const val KEY_SNAKE_X = "snake_key_x"
        private const val KEY_SNAKE_Y = "snake_key_y"
        private const val KEY_HAS_FOOD = "snake_key_has_food"
        private const val KEY_FOOD_X = "snake_key_food_x"
        private const val KEY_FOOD_Y = "snake_key_food_y"
        private const val KEY_FOOD_IS_GOLDEN = "snake_key_food_is_golden"
        private const val KEY_DIRECTION = "snake_key_direction"
        private const val KEY_SCORE = "snake_key_score"
        private const val KEY_APPLES = "snake_key_apples"
        private const val KEY_IS_GAME_OVER = "snake_key_is_game_over"
        private const val KEY_GAME_MODE = "snake_key_game_mode"
        private const val KEY_DIFFICULTY = "snake_key_difficulty"
        private const val KEY_CONTROL_SCHEME = "snake_key_control_scheme"
        private const val KEY_GAME_STATE_TYPE = "snake_key_game_state_type"
        private const val KEY_IS_NEW_HIGH = "snake_key_is_new_high"
    }
}
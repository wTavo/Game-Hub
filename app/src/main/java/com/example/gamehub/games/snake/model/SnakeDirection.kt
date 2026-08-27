package com.example.gamehub.games.snake.model

/**
 * Direcciones cardinales ortogonales en las que se desplaza el gusanito (4 vías puras).
 */
enum class SnakeDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    /**
     * Determina si la dirección actual es opuesta a otra, impidiendo giros directos de 180°.
     */
    fun isOppositeTo(other: SnakeDirection): Boolean {
        return (this == UP && other == DOWN) ||
                (this == DOWN && other == UP) ||
                (this == LEFT && other == RIGHT) ||
                (this == RIGHT && other == LEFT)
    }
}

/**
 * Representa los esquemas de control disponibles para el juego Snake (Gusanito).
 */
enum class SnakeControlType {
    /** Palanca virtual Arcade de 4 vías con camino perimetral continuo */
    JOYSTICK,

    /** Cruceta clásica D-Pad con botones táctiles individuales */
    BUTTONS,

    /** Control táctil por gestos de deslizamiento (Swipe) */
    GESTURES
}
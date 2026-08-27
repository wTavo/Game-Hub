package com.example.gamehub

import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.gamehub.databinding.ActivityMainBinding

/**
 * Actividad Principal y Host de Navegación de la Aplicación.
 * Aloja el NavHostFragment y la barra de navegación inferior (BottomNavigationView),
 * permitiendo alternar de forma modular e instantánea entre los distintos juegos.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuración del Navigation Component con el BottomNavigationView
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
    }

    /**
     * Controla la visibilidad y bloqueo del Bottom Navigation Bar durante las partidas activas.
     * Al iniciar una partida en curso, oculta suavemente la barra hacia abajo para evitar
     * salidas accidentales. Al finalizar o abandonar la partida, la restaura de inmediato.
     *
     * @param visible true si el menú de navegación debe ser visible y seleccionable; false si debe ocultarse.
     * @param animated true para aplicar una animación fluida de deslizamiento vertical.
     */
    fun setBottomNavigationVisible(visible: Boolean, animated: Boolean = true) {
        if (!::binding.isInitialized) return

        val navBar = binding.bottomNavigation
        val divider = binding.navDivider

        if (visible) {
            // Habilitar clics en todos los ítems
            for (i in 0 until navBar.menu.size()) {
                navBar.menu.getItem(i).isEnabled = true
            }

            if (navBar.visibility != View.VISIBLE) {
                navBar.visibility = View.VISIBLE
                divider.visibility = View.VISIBLE
                if (animated) {
                    navBar.translationY = 200f
                    divider.translationY = 200f
                    navBar.alpha = 0f
                    divider.alpha = 0f

                    navBar.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(220L)
                        .setInterpolator(DecelerateInterpolator())
                        .start()

                    divider.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(220L)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                } else {
                    navBar.translationY = 0f
                    navBar.alpha = 1f
                    divider.translationY = 0f
                    divider.alpha = 1f
                }
            }
        } else {
            // Deshabilitar clics inmediatamente para evitar salidas en medio de la partida
            for (i in 0 until navBar.menu.size()) {
                navBar.menu.getItem(i).isEnabled = false
            }

            if (navBar.visibility == View.VISIBLE) {
                if (animated) {
                    navBar.animate()
                        .translationY(200f)
                        .alpha(0f)
                        .setDuration(200L)
                        .setInterpolator(AccelerateInterpolator())
                        .withEndAction {
                            navBar.visibility = View.GONE
                            divider.visibility = View.GONE
                        }
                        .start()

                    divider.animate()
                        .translationY(200f)
                        .alpha(0f)
                        .setDuration(200L)
                        .setInterpolator(AccelerateInterpolator())
                        .start()
                } else {
                    navBar.visibility = View.GONE
                    divider.visibility = View.GONE
                }
            }
        }
    }
}
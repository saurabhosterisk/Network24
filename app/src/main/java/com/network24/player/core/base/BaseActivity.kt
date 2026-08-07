package com.network24.player.core.base

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableFullscreen()
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    private fun enableFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(
            window,
            window.decorView
        )
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableFullscreen()
        }
    }

    /**
     * Optional right-side (END) drawer menu wiring.
     *
     * Use this only in activities whose layout contains:
     * - DrawerLayout with id: drawerLayout
     * - NavigationView with id: rightNav
     *
     * Activities that don't have a drawer can simply not call this.
     */
    protected fun setupOptionalRightDrawerMenu(
        drawerLayout: DrawerLayout?,
        navView: NavigationView?,
        onMenuClick: (Int) -> Boolean
    ) {
        if (drawerLayout == null || navView == null) return

        navView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(GravityCompat.END)
            onMenuClick(item.itemId)
        }
    }

    /** Open the right (END) drawer if present. */
    protected fun openRightDrawer(drawerLayout: DrawerLayout?) {
        drawerLayout?.openDrawer(GravityCompat.END)
    }

    /** Close the right (END) drawer if present. */
    protected fun closeRightDrawer(drawerLayout: DrawerLayout?) {
        drawerLayout?.closeDrawer(GravityCompat.END)
    }

    override fun onDestroy() {

        window.clearFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        super.onDestroy()

    }
}
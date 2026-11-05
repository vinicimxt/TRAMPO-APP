package com.example.trampo.ui

import android.content.Intent
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.core.content.ContextCompat
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.trampo.R
import com.example.trampo.TrampoApplication
import com.example.trampo.ui.login.LoginActivity

abstract class BaseActivity : AppCompatActivity() {
    protected val app get() = application as TrampoApplication
    protected open val requiresSession = true
    protected fun layout(resource: Int) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(resource)
        val light = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK !=
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
        val root = findViewById<View>(R.id.root)
        val insetTarget = (root.parent as? ScrollView) ?: root
        val left = insetTarget.paddingLeft; val top = insetTarget.paddingTop
        val right = insetTarget.paddingRight; val bottom = insetTarget.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(insetTarget) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                view.findFocus()?.let { focused ->
                    focused.post {
                        val extra = if (focused.id == R.id.password) 96 else 16
                        val extraPixels = (extra * resources.displayMetrics.density).toInt()
                        if (insetTarget is ScrollView && root is android.view.ViewGroup) {
                            val rect = android.graphics.Rect()
                            focused.getDrawingRect(rect)
                            root.offsetDescendantRectToMyCoords(focused, rect)
                            val visibleHeight = insetTarget.height - insetTarget.paddingTop - insetTarget.paddingBottom
                            insetTarget.smoothScrollTo(0, (rect.bottom + extraPixels - visibleHeight).coerceAtLeast(0))
                        } else {
                            focused.requestRectangleOnScreen(android.graphics.Rect(0, 0, focused.width,
                                focused.height + extraPixels), false)
                        }
                    }
                }
            }
            insets
        }
        ViewCompat.requestApplyInsets(insetTarget)
        findViewById<Button>(R.id.back)?.setOnClickListener { finish() }
        findViewById<Button>(R.id.logout)?.setOnClickListener { app.auth.logout(); login() }
        listOf(R.id.message, R.id.cep_message, R.id.date_label).forEach { id ->
            findViewById<TextView>(id)?.apply {
                visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
                doAfterTextChanged { value -> visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (requiresSession && app.session.token().isNullOrBlank()) login()
    }
    protected fun login(message: Int? = null) {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (message != null) putExtra("message", message)
        }
        startActivity(intent)
        finish()
    }
    protected fun <T> route(type: Class<T>) {
        startActivity(Intent(this, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
    protected fun <T> open(type: Class<T>, id: Int? = null) {
        startActivity(Intent(this, type).apply { if (id != null) putExtra("id", id) })
    }
    protected fun <T> render(state: UiState<T>, vararg buttons: Int): T? {
        val busy = state is UiState.Loading
        findViewById<ProgressBar>(R.id.progress)?.visibility = if (busy) View.VISIBLE else View.GONE
        buttons.forEach { findViewById<View>(it)?.isEnabled = !busy }
        val message = findViewById<TextView>(R.id.message)
        message?.setTextColor(ContextCompat.getColor(this,
            if (state is UiState.Error) R.color.error else R.color.ink))
        message?.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this,
            if (state is UiState.Error) R.color.error_soft else R.color.brand_soft))
        findViewById<Button>(R.id.retry)?.setText(if (state is UiState.Error) R.string.retry else R.string.refresh)
        message?.text = if (busy) getString(R.string.loading) else ""
        if (state is UiState.Error) {
            if (state.failure.status == 401 && requiresSession) {
                login(R.string.error_session)
                return null
            }
            message?.setText(state.failure.message)
        }
        return (state as? UiState.Success)?.value
    }
}

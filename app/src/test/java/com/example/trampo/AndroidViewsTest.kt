package com.example.trampo

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.example.trampo.data.SessionManager
import com.example.trampo.data.model.UsuarioResponse
import com.example.trampo.ui.login.LoginActivity
import com.example.trampo.ui.conta.SenhaActivity
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidViewsTest {
    @Before fun clearSession() { SessionManager(RuntimeEnvironment.getApplication()).clear() }

    @Test fun launcherWithoutTokenRoutesToLoginWithVisibleLayout() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity.findViewById<View>(R.id.root))
        assertEquals(LoginActivity::class.java.name, shadowOf(activity).nextStartedActivity.component?.className)
        controller.pause().stop().destroy()
    }
    @Test fun loginValidatesEmptyFieldsWithoutNetworkAndDoesNotSavePassword() {
        val controller = Robolectric.buildActivity(LoginActivity::class.java).setup()
        val activity = controller.get()
        activity.findViewById<Button>(R.id.submit).performClick()
        assertNotNull(activity.findViewById<EditText>(R.id.email).error)
        activity.findViewById<EditText>(R.id.email).setText("cliente@example.invalid")
        activity.findViewById<Button>(R.id.submit).performClick()
        val password = activity.findViewById<EditText>(R.id.password)
        assertNotNull(password.error)
        assertFalse(password.isSaveEnabled)
        assertNotEquals(0, password.inputType and android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)
        controller.pause().stop().destroy()
    }
    @Test fun allXmlScreensAndRecyclerItemsInflateWithTheme() {
        val controller = Robolectric.buildActivity(LoginActivity::class.java).setup()
        val inflater = LayoutInflater.from(controller.get())
        val layouts = listOf(R.layout.activity_main, R.layout.activity_login, R.layout.activity_home,
            R.layout.activity_servicos, R.layout.activity_detalhe_servico, R.layout.activity_novo_agendamento,
            R.layout.activity_agendamentos, R.layout.activity_detalhe_agendamento,
            R.layout.item_servico, R.layout.item_agendamento,
            R.layout.activity_cadastro, R.layout.activity_perfil, R.layout.activity_senha,
            R.layout.activity_suporte, R.layout.activity_notificacoes, R.layout.activity_avaliacao, R.layout.item_notificacao) +
            listOf(R.layout.activity_profissional_home, R.layout.activity_profissional_servicos,
                R.layout.activity_servico_editor, R.layout.item_servico_proprio,
                R.layout.activity_profissional_locais, R.layout.activity_local_editor,
                R.layout.item_local_proprio, R.layout.activity_profissional_avaliacoes,
                R.layout.item_avaliacao_profissional)
        layouts.forEach { assertNotNull(inflater.inflate(it, null, false)) }
        controller.pause().stop().destroy()
    }
    @Test fun privateSessionCanBeRecoveredAndCleared() {
        val context = RuntimeEnvironment.getApplication()
        SessionManager(context).save("local-test")
        SessionManager(context).saveUser(UsuarioResponse(7, "Profissional", "pro@example.invalid", "profissional"))
        assertEquals("local-test", SessionManager(context).token())
        assertEquals("profissional", SessionManager(context).user()?.tipo)
        SessionManager(context).clear()
        assertNull(SessionManager(context).token())
        assertNull(SessionManager(context).user())
    }
    @Test fun logoutFromProfileScreenClearsSessionAndReturnsToLogin() {
        val context = RuntimeEnvironment.getApplication()
        SessionManager(context).save("local-test")
        SessionManager(context).saveUser(UsuarioResponse(7, "Profissional", "pro@example.invalid", "profissional"))
        val controller = Robolectric.buildActivity(SenhaActivity::class.java).setup()
        val activity = controller.get()
        activity.findViewById<Button>(R.id.logout).performClick()
        assertNull(SessionManager(context).token())
        assertNull(SessionManager(context).user())
        assertEquals(LoginActivity::class.java.name, shadowOf(activity).nextStartedActivity.component?.className)
        controller.pause().stop().destroy()
    }
}

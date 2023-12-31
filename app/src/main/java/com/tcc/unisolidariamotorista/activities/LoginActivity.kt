package com.tcc.unisolidariamotorista.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tcc.unisolidariamotorista.databinding.ActivityLoginBinding
import com.tcc.unisolidariamotorista.providers.AuthProvider
import com.tcc.unisolidariamotorista.utils.CircleAnimationUtil

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    val authProvider = AuthProvider()

    private var circleAnimationUtil: CircleAnimationUtil? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)


        binding.btnRegister.setOnClickListener { goToRegister() }
        binding.btnLogin.setOnClickListener { login() }
        startCirclesAnimation()
    }

    private fun startCirclesAnimation() {
        val circles = listOf(binding.imgCircleEnd, binding.imgCircleBottom)
        circleAnimationUtil = CircleAnimationUtil(circles)
        circleAnimationUtil?.start()
    }

    private fun login() {
        val email = binding.textFieldEmail.text.toString()
        val password = binding.textFieldPassword.text.toString()

        if (isValidForm(email, password)) {
            authProvider.login(email, password).addOnCompleteListener {
                if (it.isSuccessful) {
                    goToMap()
                }
                else {
                    Toast.makeText(this@LoginActivity, "Erro ao fazer login", Toast.LENGTH_SHORT).show()
                    Log.d("FIREBASE", "ERROR: ${it.exception.toString()}")
                }
            }
        }
    }

    private fun goToMap() {
        val i = Intent(this, MapActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }

    private fun isValidForm(email: String, password: String): Boolean {

        if (email.isEmpty()) {
            Toast.makeText(this, "Digite seu e-mail", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Coloque sua senha", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun goToRegister() {
        val i = Intent(this, RegisterActivity::class.java)
        startActivity(i)
    }

    override fun onStart() {
        super.onStart()
        if (authProvider.existSession()) {
            goToMap()
        }
    }

    override fun onDestroy() {
        circleAnimationUtil?.cancel()
        super.onDestroy()
    }
}
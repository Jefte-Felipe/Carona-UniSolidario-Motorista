package com.tcc.unisolidariamotorista.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tcc.unisolidariamotorista.databinding.ActivityRegisterBinding
import com.tcc.unisolidariamotorista.models.Driver
import com.tcc.unisolidariamotorista.providers.AuthProvider
import com.tcc.unisolidariamotorista.providers.DriverProvider
import com.tcc.unisolidariamotorista.utils.CircleAnimationUtil

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authProvider = AuthProvider()
    private val driverProvider = DriverProvider()

    private var circleAnimationUtil: CircleAnimationUtil? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        binding.btnRegister.setOnClickListener { register() }
        startCirclesAnimation()

        binding.questData.setOnClickListener {
            lgpdData()
        }
    }

    private fun lgpdData() {
        val i = Intent(this, QuestionAnswerActivity::class.java)
        startActivity(i)
    }

    private fun startCirclesAnimation() {
        val circles = listOf(binding.imgCircleEnd)
        circleAnimationUtil = CircleAnimationUtil(circles)
        circleAnimationUtil?.start()
    }

    private fun register() {
        val name = binding.textFieldName.text.toString()
        val lastname = binding.textFieldLastname.text.toString()
        val email = binding.textFieldEmail.text.toString()
        val phone = binding.textFieldPhone.text.toString()
        val password = binding.textFieldPassword.text.toString()
        val confirmPassword = binding.textFieldConfirmPassword.text.toString()

        val (isValid, _) = isValidForm(name, lastname, email, password, confirmPassword)
        if (isValid) {
            authProvider.register(email, password).addOnCompleteListener {
                if (it.isSuccessful) {
                    val driver = Driver(
                        id = authProvider.getId(),
                        name = name,
                        lastname = lastname,
                        phone = phone,
                        email = email
                    )
                    driverProvider.create(driver).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(this@RegisterActivity, "Registro bem-sucedido", Toast.LENGTH_SHORT).show()
                            goToMap()
                        }
                        else {
                            Toast.makeText(this@RegisterActivity, "Ocorreu um erro Os dados do usuário foram armazenados ${it.exception.toString()}", Toast.LENGTH_SHORT).show()
                            Log.d("FIREBASE", "Error: ${it.exception.toString()}")
                        }
                    }
                }
                else {
                    Toast.makeText(this@RegisterActivity, "Registro fallido ${it.exception.toString()}", Toast.LENGTH_LONG).show()
                    Log.d("FIREBASE", "Error: ${it.exception.toString()}")
                }
            }
        }

    }

    private fun goToMap() {
        val i = Intent(this, MapActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }

    private fun isValidForm(
        name: String,
        lastname: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Pair<Boolean, String> {

        val phone = binding.textFieldPhone.text.toString()
        val isPhoneValid = binding.textFieldPhone.isDone

        if (name.isEmpty()) {
            Toast.makeText(this, "Você deve inserir seu nome", Toast.LENGTH_SHORT).show()
            return Pair(false, "")
        }

        if (lastname.isEmpty()) {
            Toast.makeText(this, "Você deve inserir seu sobrenome", Toast.LENGTH_SHORT).show()
            return Pair(false, "")
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Você deve inserir seu e-mail", Toast.LENGTH_SHORT).show()
            return Pair(false, "")
        }

        if (!isPhoneValid) {
            Toast.makeText(this, "Telefone inválido", Toast.LENGTH_SHORT).show()
            return Pair(false, "")
        }

        val unmaskedPhone = binding.textFieldPhone.unMasked

        if (password.isEmpty()) {
            Toast.makeText(this, "Você deve inserir a senha", Toast.LENGTH_SHORT).show()
            return Pair(false, "")
        }

        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "Você deve inserir a confirmação da senha", Toast.LENGTH_SHORT)
                .show()
            return Pair(false, "")
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "As senhas devem corresponder", Toast.LENGTH_SHORT).show()
            return Pair(false, "")
        }

        if (password.length < 6) {
            Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres", Toast.LENGTH_LONG)
                .show()
            return Pair(false, "")
        }

        return Pair(true, unmaskedPhone)
    }

    private fun goToLogin() {
        val i = Intent(this, LoginActivity::class.java)
        startActivity(i)
    }

    override fun onDestroy() {
        circleAnimationUtil?.cancel()
        super.onDestroy()
    }
}
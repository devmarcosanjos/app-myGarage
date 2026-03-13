package com.marcosanjos.mygaragem

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.marcosanjos.mygaragem.databinding.ActivityLoginBinding
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var verificationId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Verifica se já está logado ANTES de inflar o layout
        if (auth.currentUser != null) {
            navigateToMainActivity()
            return
        }

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupView()
    }

    private fun setupView() {
        // Desabilita botão SMS se não tiver número
        binding.btnSendSms.isEnabled = false

        binding.etPhone.doAfterTextChanged { text ->
            val phone = text.toString().trim()
            // Habilita somente se tiver mais que apenas o "+55"
            binding.btnSendSms.isEnabled = phone.length > 4
        }

        binding.btnSendSms.setOnClickListener {
            sendVerificationCode()
        }

        binding.btnVerifySms.setOnClickListener {
            verifyCode()
        }
    }

    private fun sendVerificationCode() {
        val phoneNumber = binding.etPhone.text.toString().trim()
        if (phoneNumber.isEmpty()) {
            binding.etPhone.error = "Informe o número de telefone"
            return
        }

        binding.btnSendSms.isEnabled = false

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(45L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                navigateToMainActivity()
                            }
                        }
                }

                override fun onVerificationFailed(exception: FirebaseException) {
                    binding.btnSendSms.isEnabled = true
                    Toast.makeText(
                        this@LoginActivity,
                        "Erro: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@LoginActivity.verificationId = verificationId
                    binding.btnSendSms.isEnabled = true
                    binding.tilVerifyCode.visibility = View.VISIBLE
                    binding.btnVerifySms.visibility = View.VISIBLE
                    Toast.makeText(
                        this@LoginActivity,
                        "Código de verificação enviado via SMS",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyCode() {
        val code = binding.etVerifyCode.text.toString().trim()
        if (code.isEmpty()) {
            binding.etVerifyCode.error = "Informe o código"
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    navigateToMainActivity()
                } else {
                    Toast.makeText(
                        this,
                        "Código inválido: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

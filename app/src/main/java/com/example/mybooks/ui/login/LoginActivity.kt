package com.example.mybooks.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mybooks.MainActivity
import com.example.mybooks.R
import com.example.mybooks.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        clickListeners()
    }

    private fun clickListeners() {
        binding.apply {

            btnLogin.setOnClickListener {
                if (validatedField()) {
                    moveToNextScreen()
                }
            }
            btnGuest.setOnClickListener {
                moveToNextScreen()
            }
        }
    }

    private fun moveToNextScreen() {
        startActivity(Intent(this, MainActivity::class.java))
    }


    private fun validatedField(): Boolean {
        if (binding.etEmailPhone.text.toString().isEmpty()) {
            showToast("Please Enter Email or Phone Number!")
            return false
        }
        if (binding.etPassword.text.toString().isEmpty()) {
            showToast("Please Enter Password!")
            return false
        }
        return true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
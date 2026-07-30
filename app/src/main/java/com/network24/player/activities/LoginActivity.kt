package com.network24.player.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.network24.player.databinding.ActivityLoginBinding
import com.network24.player.repository.LoginRepository
import com.network24.player.utils.PreferenceManager
import kotlinx.coroutines.launch
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private lateinit var repository: LoginRepository
    private lateinit var prefs: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = LoginRepository()
        prefs = PreferenceManager(this)

        // Temporary testing credentials
        binding.edtServer.setText("http://op.web24.live:8080")
        binding.edtUsername.setText("ntesting")
        binding.edtPassword.setText("ntesting123")
        binding.chkRemember.isChecked = true

        // Login button par focus
        binding.btnLogin.requestFocus()


        // Restore saved credentials
        if (prefs.isRememberMe()) {

            binding.edtServer.setText(prefs.getServer())
            binding.edtUsername.setText(prefs.getUsername())
            binding.edtPassword.setText(prefs.getPassword())
            binding.chkRemember.isChecked = true
        }

        binding.btnLogin.setOnClickListener {

            login()
        }
    }

    private fun login() {

        val server = binding.edtServer.text.toString().trim()

        val username = binding.edtUsername.text.toString().trim()

        val password = binding.edtPassword.text.toString().trim()

        if (server.isEmpty()) {

            binding.edtServer.error = "Enter Server URL"
            return
        }

        if (username.isEmpty()) {

            binding.edtUsername.error = "Enter Username"
            return
        }

        if (password.isEmpty()) {

            binding.edtPassword.error = "Enter Password"
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {

            try {

                val response =
                    repository.login(server, username, password)

                binding.progressBar.visibility =
                    android.view.View.GONE

                if (response.isSuccessful &&
                    response.body() != null &&
                    response.body()!!.user_info?.auth == 1
                ) {

                    if (binding.chkRemember.isChecked) {

                        prefs.saveLogin(
                            server,
                            username,
                            password,
                            true
                        )

                    } else {

                        prefs.clear()
                    }

                    Toast.makeText(
                        this@LoginActivity,
                        "Login Successful",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(
                        Intent(
                            this@LoginActivity,
                            HomeActivity::class.java
                        )
                    )

                    finish()

                } else {

                    Toast.makeText(
                        this@LoginActivity,
                        "Invalid Username or Password",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: IOException) {

                binding.progressBar.visibility =
                    android.view.View.GONE

                Toast.makeText(
                    this@LoginActivity,
                    "Unable to connect to server",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {

                binding.progressBar.visibility =
                    android.view.View.GONE

                Toast.makeText(
                    this@LoginActivity,
                    e.localizedMessage ?: "Unknown Error",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
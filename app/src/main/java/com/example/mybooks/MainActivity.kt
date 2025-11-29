package com.example.mybooks

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mybooks.data.repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var userRepository: UserRepository
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val db: FirebaseFirestore by lazy { Firebase.firestore }

    private lateinit var statusText: TextView
    private lateinit var signInButton: Button
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var signInContainer: FrameLayout

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { token ->
                firebaseAuthWithGoogle(token)
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        signInButton = findViewById(R.id.sign_in_button)
        bottomNav = findViewById(R.id.bottom_nav_view)
        signInContainer = findViewById(R.id.sign_in_container)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        userRepository = UserRepository(googleSignInClient)

        signInButton.setOnClickListener {
            val signInIntent = userRepository.getGoogleSignInIntent()
            signInLauncher.launch(signInIntent)
        }

        updateUI()
    }

    private fun updateUI() {
        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser

                if (currentUser != null) {
                    // User is signed in
                    signInContainer.visibility = View.GONE

                    // Fetch user role
                    val userRole = try {
                        val userDoc = db.collection("users")
                            .document(currentUser.uid)
                            .get()
                            .await()
                        userDoc.getString("role") ?: "manage"  // Default to manage for testing
                    } catch (e: Exception) {
                        "manage"
                    }

                    setupNavigation(userRole)
                } else {
                    // User is not signed in - ALLOW ACCESS FOR TESTING
                    signInContainer.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Running in test mode (no authentication)", Toast.LENGTH_SHORT).show()
                    setupNavigation("manage")  // Give manage access for testing
                }
            } catch (e: Exception) {
                statusText.text = "Error: ${e.message}"
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNavigation(userRole: String) {
        try {
            when (userRole) {
                "none" -> {
                    // Users with no role can't access anything
                    signInContainer.visibility = View.VISIBLE
                    statusText.text = "Your account is pending approval.\nPlease contact an administrator."
                    signInButton.visibility = View.GONE
                    bottomNav.visibility = View.GONE
                }
                else -> {
                    // Setup navigation controller
                    val navHostFragment = supportFragmentManager
                        .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment

                    if (navHostFragment != null) {
                        val navController = navHostFragment.navController
                        bottomNav.setupWithNavController(navController)
                        bottomNav.visibility = View.VISIBLE

                        val menu = bottomNav.menu

                        when (userRole) {
                            "viewer" -> {
                                // Viewers can see Dashboard and Profile, but not Books management
                                menu.findItem(R.id.navigation_books)?.isVisible = false
                            }
                            else -> {
                                // "manage" and "admin" have access to all items
                                menu.findItem(R.id.navigation_dashboard)?.isVisible = true
                                menu.findItem(R.id.navigation_books)?.isVisible = true
                                menu.findItem(R.id.navigation_profile)?.isVisible = true
                            }
                        }
                    } else {
                        Toast.makeText(this, "Navigation error. Please restart the app.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up navigation: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        lifecycleScope.launch {
            try {
                val result = userRepository.firebaseAuthWithGoogle(idToken)
                if (result.isSuccess) {
                    Toast.makeText(this@MainActivity, "Sign in successful!", Toast.LENGTH_SHORT).show()
                    updateUI()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Authentication failed: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
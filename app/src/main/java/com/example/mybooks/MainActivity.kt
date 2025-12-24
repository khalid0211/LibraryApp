package com.example.mybooks

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.mybooks.data.repository.UserRepository
import com.example.mybooks.databinding.ActivityMainBinding
import com.example.mybooks.ui.login.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private lateinit var userRepository: UserRepository
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val db: FirebaseFirestore by lazy { Firebase.firestore }
    private lateinit var navController: NavController
    public var searchQuery: (String) -> Unit = {}

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
        setContentView(binding.root)
        //  drawerLayout = findViewById(R.id.drawer_layout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.drawerLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
                topMargin = systemBars.top
            }
            insets
        }


        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        userRepository = UserRepository(googleSignInClient)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.signInButton.setOnClickListener {
            val signInIntent = userRepository.getGoogleSignInIntent()
            signInLauncher.launch(signInIntent)
        }
        clickListeners()
        updateUI()
    }

    private fun clickListeners() {

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                searchQuery(s.toString())
            }

        })

        binding.drawerMenuBtn.setOnClickListener {
            openCloseDrawer(binding.drawerLayout)
        }
        binding.customDrawer.customHeader.ivBackMenu.setOnClickListener {
            openCloseDrawer(binding.drawerLayout)
        }
        binding.customDrawer.btnHome.setOnClickListener {
            selectDrawerItem(it)
            navController.navigate(R.id.navigation_books)
            openCloseDrawer(binding.drawerLayout)
        }

        binding.customDrawer.btnViewBooks.setOnClickListener {
            selectDrawerItem(it)
        }

        binding.customDrawer.btnManageBooks.setOnClickListener {
            selectDrawerItem(it)
        }
        binding.customDrawer.btnBookShelfs.setOnClickListener {
            selectDrawerItem(it)
            navController.navigate(R.id.navigation_bookshelves)
            openCloseDrawer(binding.drawerLayout)
        }

        binding.customDrawer.btnOwners.setOnClickListener {
            selectDrawerItem(it)
        }

        binding.customDrawer.btnSetting.setOnClickListener {
            // selectDrawerItem(it)
//            navController.navigate(R.id.navigation_profile)
//            openCloseDrawer(binding.drawerLayout)
        }
        binding.customDrawer.btnLogout.setOnClickListener {
            openCloseDrawer(binding.drawerLayout)
            showLogoutDialogue()
        }

    }

    public fun onSearch(query: (String) -> Unit) {
        searchQuery = query
    }

    private fun showLogoutDialogue() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.logout_dialogue_layout, null)
        val cancelButton = dialogView.findViewById<TextView>(R.id.cancel_button)
        val logoutBtn = dialogView.findViewById<TextView>(R.id.logout_btn)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialogue)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        logoutBtn.setOnClickListener {
            auth.signOut()
            dialog.dismiss()
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()

        }


        dialog.show()
    }

    private fun selectDrawerItem(selectedView: View) {
        val parent = selectedView.parent as ViewGroup
        for (i in 0 until parent.childCount) {
            parent.getChildAt(i).isSelected = false
        }
        selectedView.isSelected = true
    }

    fun openCloseDrawer(drawerLayout: DrawerLayout) {
        if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.openDrawer(GravityCompat.START)
        } else {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }
    private fun updateUI() {
        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser

                if (currentUser != null) {
                    // User is signed in
                    binding.signInContainer.visibility = View.GONE

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
                    binding.signInContainer.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Running in test mode (no authentication)", Toast.LENGTH_SHORT).show()
                    setupNavigation("manage")  // Give manage access for testing
                }
            } catch (e: Exception) {
                binding.statusText.text = "Error: ${e.message}"
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNavigation(userRole: String) {
        /*  try {
              when (userRole) {
                  "none" -> {
                      // Users with no role can't access anything
                      binding.signInContainer.visibility = View.VISIBLE
                      binding.statusText.text = "Your account is pending approval.\nPlease contact an administrator."
                      binding.signInButton.visibility = View.GONE
                      binding.bottomNav.visibility = View.GONE
                  }
                  else -> {
                      // Setup navigation controller
                      val navHostFragment = supportFragmentManager
                          .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment

                      if (navHostFragment != null) {
                          val navController = navHostFragment.navController
                          binding.bottomNav.setupWithNavController(navController)
                          binding.bottomNav.visibility = View.VISIBLE

                          val menu = binding.bottomNav.menu

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
          }*/
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
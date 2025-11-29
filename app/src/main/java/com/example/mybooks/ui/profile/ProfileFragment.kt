package com.example.mybooks.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mybooks.MainActivity
import com.example.mybooks.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFragment : Fragment() {

    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val db: FirebaseFirestore by lazy { Firebase.firestore }

    private lateinit var emailText: TextView
    private lateinit var nameText: TextView
    private lateinit var roleText: TextView
    private lateinit var signOutButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emailText = view.findViewById(R.id.profile_email)
        nameText = view.findViewById(R.id.profile_name)
        roleText = view.findViewById(R.id.profile_role)
        signOutButton = view.findViewById(R.id.sign_out_button)

        loadProfile()

        signOutButton.setOnClickListener {
            signOut()
        }
    }

    private fun loadProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser

                if (currentUser != null) {
                    emailText.text = currentUser.email ?: "N/A"
                    nameText.text = currentUser.displayName ?: "N/A"

                    // Fetch role from Firestore
                    val userDoc = db.collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    val role = userDoc.getString("role") ?: "none"
                    roleText.text = role.uppercase()

                    // Color code the role
                    when (role) {
                        "admin" -> roleText.setTextColor(0xFFE91E63.toInt())
                        "manage" -> roleText.setTextColor(0xFF2196F3.toInt())
                        "viewer" -> roleText.setTextColor(0xFF4CAF50.toInt())
                        else -> roleText.setTextColor(0xFF9E9E9E.toInt())
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signOut() {
        auth.signOut()
        Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()

        // Restart MainActivity
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}

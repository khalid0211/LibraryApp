package com.example.mybooks.data.repository

import android.content.Intent
import com.example.mybooks.data.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class UserRepository(private val googleSignInClient: GoogleSignInClient) {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun firebaseAuthWithGoogle(idToken: String): Result<User> = withContext(Dispatchers.IO) {
        return@withContext try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return@withContext Result.failure(Exception("Firebase user is null"))

            val userDocRef = db.collection("users").document(firebaseUser.uid)
            val userDoc = userDocRef.get().await()

            val user: User
            if (userDoc.exists()) {
                // User exists, update last login and fetch role
                user = userDoc.toObject(User::class.java)
                    ?: return@withContext Result.failure(Exception("Failed to parse user document"))
                userDocRef.update("lastLogin", Timestamp.now()).await()
            } else {
                // New user, create with "none" role
                val email = firebaseUser.email
                    ?: return@withContext Result.failure(Exception("User email is null"))
                val name = firebaseUser.displayName
                    ?: return@withContext Result.failure(Exception("User display name is null"))

                user = User(
                    email = email,
                    name = name,
                    role = "none",
                    status = "active",
                    lastLogin = Timestamp.now()
                )
                userDocRef.set(user).await()
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
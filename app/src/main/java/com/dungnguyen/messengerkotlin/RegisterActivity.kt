package com.dungnguyen.messengerkotlin

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        register_button_register.setOnClickListener {
            performRegister()
        }

        already_have_account_textview.setOnClickListener {
            Log.d("RegisterActivity", "Show login activity")

            // launch the login activity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        selectphoto_button.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
    }

    var selectedPhotoUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            Log.d("RegisterActivity", "Photo was selected")

            selectedPhotoUri = data.data

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)

            selectphoto_imageview_register.setImageBitmap(bitmap)
            selectphoto_button.alpha = 0f

        }
    }

    private fun performRegister() {
        val TAG = "RegisterActivity"
        val email = email_edittext_register.text.toString()
        val password = password_edittext_register.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter text in email, and password", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Email is: " + email)
        Log.d(TAG, "Password is: $password")

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener

                // else if successful
                Log.d("RegisterActivity",
                    "Successfully created user with uid: ${it.result?.user?.uid}")

                uploadImageToFirebaseStorage()
            }
            .addOnFailureListener {
                Log.d(TAG, "Fail to create user: ${it.message}")
                Toast.makeText(this, it.message.toString(), Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImageToFirebaseStorage() {
        if (selectedPhotoUri == null) return
        val fileName = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/image/$fileName")
        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                Log.d("RegisterActivity", "Successfully uploaded image: ${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener {
                    Log.d("RegisterActivity","File location: $it")

                    saveUserToFirebaseDatabase(it.toString())
                }
            }
            .addOnFailureListener {
                Log.d("RegisterActivity","FAILED to saved PHOTO to Firebase Storage")
            }
    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val user = User(uid, username_edittext_register.text.toString(), profileImageUrl)
        ref.setValue(user)
            .addOnSuccessListener {
                Log.d("RegisterActivity","Saved user to Firebase Database")
            }
            .addOnFailureListener {
                Log.d("RegisterActivity", "FAILED to saved user: $it")
            }
    }
}

class User(val uid: String, val username: String, val profileImageUrl: String)

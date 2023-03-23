package com.fahim.rnd_android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : AppCompatActivity() {
    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var gso: GoogleSignInOptions
    lateinit var signInButton: SignInButton

    private val RC_SIGN_IN = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        signInButton = findViewById<SignInButton>(R.id.sign_in_button);
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInButton.setOnClickListener(View.OnClickListener {
            singIn()
        })
    }
    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if(account!=null) {
            val intent = Intent(this, UserProfile::class.java)
            startActivity(intent)
        }
    }
    fun singIn() {
        val intent = mGoogleSignInClient!!.signInIntent
        startActivityForResult(intent, RC_SIGN_IN)

    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task =
                GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account :GoogleSignInAccount? = task.getResult(ApiException::class.java)

                val intent = Intent(this, UserProfile::class.java)
                startActivity(intent)

            } catch (e: ApiException) {
                // The ApiException status code indicates the detailed failure reason.
                // Please refer to the GoogleSignInStatusCodes class reference for more information.
                Log.e("TAG","signInResult:failed code=" + e.statusCode)
            }
        }
    }

    private fun handleSignInResult(tase: Task<GoogleSignInAccount>) {

        val account = tase.result
        if (account != null) {
            getDriveService()
            Log.e("account", account.toString())
            Toast.makeText(this, "Please Log In first!", LENGTH_SHORT).show()
        }
    }

    private fun getDriveService(): Drive? {
        GoogleSignIn.getLastSignedInAccount(this)?.let { googleAccount ->
            val credential = GoogleAccountCredential.usingOAuth2(
                this, listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = googleAccount.account!!
            return Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(getString(R.string.app_name))
                .build()
        }
        return null
    }

    private fun accessDriveFiles() {
        getDriveService()?.let { googleDriveService ->
            CoroutineScope(Dispatchers.IO).launch {
                var pageToken: String?
                do {
                    val result = googleDriveService.files().list().apply {
                        spaces = "drive"
                        fields = "nextPageToken, files(id, name)"
                        pageToken = this.pageToken
                    }.execute()

                    result.files.forEach { file ->
                        Log.d("FILE", ("name=${file.name} id=${file.id}"))
                    }
                } while (pageToken != null)
            }
        }
    }

    fun uploadFileToGDrive() {
        getDriveService()?.let { googleDriveService ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val localFileDirectory = File(getExternalFilesDir("backup")!!.toURI())
                    val actualFile = File("${localFileDirectory}/FILE_NAME_BACKUP")
                    val gFile = com.google.api.services.drive.model.File()
                    gFile.name = actualFile.name
                    val fileContent = FileContent("text/plain", actualFile)
                    googleDriveService.Files().create(gFile, fileContent).execute()
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }
        } ?: Toast.makeText(this, "Please Log In first!", LENGTH_SHORT).show()
    }

    fun downloadFileFromGDrive(id: String) {
        getDriveService()?.let { googleDriveService ->
            CoroutineScope(Dispatchers.IO).launch {
                googleDriveService.Files().get(id).execute()
            }
        } ?: Toast.makeText(this, "Please Log In first!", LENGTH_SHORT).show()
    }
}
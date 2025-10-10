package com.example.wildercards;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import com.google.android.material.button.MaterialButton; // Import MaterialButton
import com.google.android.material.textfield.TextInputLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView signUpText, forgotPasswordText;
    private MaterialButton googleButton; // Changed to MaterialButton
    private Dialog mLoadingDialog;
    private TextInputLayout emailInputLayout;

    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase
        mAuth = FirebaseAuth.getInstance();

        // Views - make sure these IDs exist in your layout
        emailInputLayout = findViewById(R.id.emailInputLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signUpText = findViewById(R.id.signUpLink);
        forgotPasswordText = findViewById(R.id.forgotPassword);
        googleButton = findViewById(R.id.customGoogleSignIn); // Now correctly finds the MaterialButton

        emailInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                emailInputLayout.setHint(getString(R.string.email_hint));
            } else {
                if (emailInput.getText().toString().isEmpty()) {
                    emailInputLayout.setHint(getString(R.string.login_email_hint));
                }
            }
        });

        // Email/password login
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoadingDialog();
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        hideLoadingDialog();
                        if (task.isSuccessful()) {
                            goToMain();
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Forgot password -> opens ForgotPasswordActivity
        forgotPasswordText.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        // Sign up redirect
        signUpText.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));

        // Google sign-in click
        googleButton.setOnClickListener(v -> signInWithGoogle());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Auto-login if already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMain();
        }
    }

    // Start Google Sign-In flow
    private void signInWithGoogle() {
        // Make sure R.string.default_web_client_id exists (from google-services.json or added manually)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        showLoadingDialog();
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN); // deprecated warning ok for now
    }

    // Handle Google Sign-In result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        hideLoadingDialog();

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    String idToken = account.getIdToken();
                    if (idToken != null) {
                        firebaseAuthWithGoogle(idToken);
                    } else {
                        Toast.makeText(this, "No ID token from Google", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Exchange Google idToken for Firebase credential
    private void firebaseAuthWithGoogle(String idToken) {
        showLoadingDialog();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            hideLoadingDialog();
            if (task.isSuccessful()) {
                goToMain();
            } else {
                Toast.makeText(this, "Google login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void showLoadingDialog() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new Dialog(this);
            mLoadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mLoadingDialog.setContentView(R.layout.dialog_loading);
            if (mLoadingDialog.getWindow() != null) {
                mLoadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            mLoadingDialog.setCancelable(false);
            mLoadingDialog.setCanceledOnTouchOutside(false);
        }
        if (!mLoadingDialog.isShowing()) {
            mLoadingDialog.show();
        }
    }

    private void hideLoadingDialog() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
        mLoadingDialog = null;
    }
}

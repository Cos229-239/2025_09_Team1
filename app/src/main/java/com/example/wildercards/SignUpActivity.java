package com.example.wildercards;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private EditText nameInput, usernameInput, ageInput, emailInput, passwordInput, confirmPasswordInput;
    private ViewFlipper viewFlipper;
    private Dialog mLoadingDialog;
    private TextInputLayout emailInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        viewFlipper = findViewById(R.id.viewFlipper);

        // Page 1
        nameInput = findViewById(R.id.nameInput);
        usernameInput = findViewById(R.id.usernameInput);
        Button nextButton1 = findViewById(R.id.nextButton1);

        // Page 2
        ageInput = findViewById(R.id.ageInput);
        Button backButton1 = findViewById(R.id.backButton1);
        Button nextButton2 = findViewById(R.id.nextButton2);

        // Page 3
        emailInputLayout = findViewById(R.id.emailInputLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        Button backButton2 = findViewById(R.id.backButton2);
        Button signUpButton = findViewById(R.id.signUpButton);

        TextView loginLink = findViewById(R.id.loginLink);

        emailInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                emailInputLayout.setHint(getString(R.string.email_hint));
            } else {
                if (emailInput.getText().toString().isEmpty()) {
                    emailInputLayout.setHint(getString(R.string.signup_email_hint));
                }
            }
        });

        nextButton1.setOnClickListener(v -> {
            if (validatePage1()) {
                viewFlipper.showNext();
            }
        });

        backButton1.setOnClickListener(v -> viewFlipper.showPrevious());

        nextButton2.setOnClickListener(v -> {
            if (validatePage2()) {
                viewFlipper.showNext();
            }
        });

        backButton2.setOnClickListener(v -> viewFlipper.showPrevious());

        signUpButton.setOnClickListener(v -> {
            if (validatePage3()) {
                createAccount();
            }
        });

        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private boolean validatePage1() {
        if (TextUtils.isEmpty(nameInput.getText().toString().trim())) {
            nameInput.setError("Name is required");
            return false;
        }
        if (TextUtils.isEmpty(usernameInput.getText().toString().trim())) {
            usernameInput.setError("Username is required");
            return false;
        }
        return true;
    }

    private boolean validatePage2() {
        if (TextUtils.isEmpty(ageInput.getText().toString().trim())) {
            ageInput.setError("Age is required");
            return false;
        }
        return true;
    }

    private boolean validatePage3() {
        if (TextUtils.isEmpty(emailInput.getText().toString().trim())) {
            emailInput.setError("Email is required");
            return false;
        }
        if (TextUtils.isEmpty(passwordInput.getText().toString().trim())) {
            passwordInput.setError("Password is required");
            return false;
        }
        if (!passwordInput.getText().toString().trim().equals(confirmPasswordInput.getText().toString().trim())) {
            confirmPasswordInput.setError("Passwords do not match");
            return false;
        }
        return true;
    }

    private void createAccount() {
        showLoadingDialog();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveUserInformation(firebaseUser.getUid());
                        }
                    } else {
                        hideLoadingDialog();
                        Toast.makeText(SignUpActivity.this, "Sign-up failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserInformation(String userId) {
        String name = nameInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String ageStr = ageInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("username", username);
        user.put("email", email);
        try {
            user.put("age", Long.parseLong(ageStr));
        } catch (NumberFormatException e) {
            hideLoadingDialog();
            ageInput.setError("Invalid age format");
            return;
        }

        firestore.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    hideLoadingDialog();
                    Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideLoadingDialog();
                    Toast.makeText(SignUpActivity.this, "Failed to save user details.", Toast.LENGTH_SHORT).show();
                });
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

package com.example.wildercards;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import com.google.firebase.auth.FirebaseAuth;
import android.widget.Toast;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailInput, passwordInput, confirmPasswordInput, nameInput;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        Button signUpButton = findViewById(R.id.signUpButton);
        TextView loginLink = findViewById(R.id.loginLink);

        signUpButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                            finish();
                       } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        });

        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
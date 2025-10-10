package com.example.wildercards;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity{

    private EditText emailInput;
    private Button resetPasswardButtion;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailInput = findViewById(R.id.emailInput);
        resetPasswardButtion = findViewById(R.id.resetPasswordButton);
        mAuth = FirebaseAuth.getInstance();

        resetPasswardButtion.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()){
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_LONG).show();
                    finish(); //go back to login
                } else {
                    Toast.makeText(this, "Error:" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}


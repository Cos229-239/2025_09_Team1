package com.example.wildercards;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


import com.bumptech.glide.Glide;
import com.example.wildercards.databinding.ActivityProfileBinding;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private ActivityProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(binding.profileImage);
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        binding = ActivityProfileBinding.bind(findViewById(R.id.main));
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        loadUserProfile();
        setupClickListeners();
        setupAppBar();
    }

    private void setupAppBar() {
        binding.appBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            private boolean isShow = true;
            private int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    binding.collapsingToolbar.setTitle("Profile");
                    binding.profileImageToolbar.setVisibility(View.VISIBLE);
                    isShow = true;
                } else if (isShow) {
                    binding.collapsingToolbar.setTitle(" ");
                    binding.profileImageToolbar.setVisibility(View.INVISIBLE);
                    isShow = false;
                }
            }
        });
    }

    private void setupClickListeners(){
        binding.profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        binding.buttonSaveChanges.setOnClickListener(v -> saveUserProfile());

        binding.buttonLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserProfile(){
        showLoadingDialog();
        String userId = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            hideLoadingDialog();
            Toast.makeText(this, "User is not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    hideLoadingDialog();
                    if (document != null && document.exists()) {
                        binding.textName.setText(document.getString("name"));
                        binding.textUsername.setText(String.format("@%s", document.getString("username")));
                        binding.textEmail.setText(String.format("Email: %s", document.getString("email")));

                        binding.editTextName.setText(document.getString("name"));
                        binding.editTextUsername.setText(document.getString("username"));
                        binding.editTextEmail.setText(document.getString("email"));

                        if (document.contains("age")) {
                            Long age = document.getLong("age");
                            if(age != null){
                                binding.textAge.setText(String.format("Age: %d", age));
                                binding.editTextAge.setText(String.valueOf(age));
                            }
                        }

                        String imageUrl = document.getString("profileImageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this).load(imageUrl).into(binding.profileImage);
                            Glide.with(this).load(imageUrl).into(binding.profileImageToolbar);
                        }
                    } else {
                        Toast.makeText(this, "Profile not found. Please save your info.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoadingDialog();
                    Toast.makeText(ProfileActivity.this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserProfile() {
        showLoadingDialog();
        String userId = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            hideLoadingDialog();
            Toast.makeText(this, "Cannot save, user is not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            StorageReference storageRef = storage.getReference().child("profile_images/" + userId);
            storageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUrl -> {
                                updateFirestoreUser(userId, downloadUrl.toString());
                            }))
                    .addOnFailureListener(e -> {
                        hideLoadingDialog();
                        Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            updateFirestoreUser(userId, null);
        }
    }

    private void updateFirestoreUser(String userId, String imageUrl) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", binding.editTextName.getText().toString().trim());
        userData.put("username", binding.editTextUsername.getText().toString().trim());
        userData.put("email", binding.editTextEmail.getText().toString().trim());

        try {
            userData.put("age", Long.parseLong(binding.editTextAge.getText().toString().trim()));
        } catch (NumberFormatException e) {
            // Do not add age if the field is empty or invalid.
        }

        if (imageUrl != null) {
            userData.put("profileImageUrl", imageUrl);
        }

        firestore.collection("users").document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    hideLoadingDialog();
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    selectedImageUri = null;
                    loadUserProfile();
                })
                .addOnFailureListener(e -> {
                    hideLoadingDialog();
                    Toast.makeText(this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
                });
    }
}

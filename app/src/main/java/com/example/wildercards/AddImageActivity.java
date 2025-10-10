package com.example.wildercards;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
// Import your BaseActivity - adjust the package name as needed
// import com.yourpackage.BaseActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class AddImageActivity extends BaseActivity {

    private MaterialButton btnUpload, btnTakePhoto;
    private ImageView imageView; // Add an ImageView to your layout to display the selected/captured image

    private Uri currentPhotoUri;
    private String currentPhotoPath;

    // Permission request codes
    private static final int PERMISSION_REQUEST_CODE = 100;

    // Activity Result Launchers for modern API
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;
    private ActivityResultLauncher<String[]> multiplePermissionLauncher;

    // To store and return the selected/captured image
    private Uri selectedImageUri;
    private Bitmap selectedImageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_image);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        initViews();
        initActivityLaunchers();
        setupClickListeners();
    }

    // request camera and storage permission
    private void initViews() {
        btnUpload = findViewById(R.id.btnUpload);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        // Initialize ImageView if you have one in your layout
        imageView = findViewById(R.id.imageView);
    }

    private void initActivityLaunchers() {
        // Gallery launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            handleSelectedImage(imageUri);
                        }
                    }
                }
        );

        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Show preview and ask for confirmation
                        showImagePreviewDialog();
                    }
                }
        );

        // Permission launchers
        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openGallery();
                    } else {
                        showPermissionDeniedDialog("Storage");
                    }
                }
        );

        multiplePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    for (Boolean granted : permissions.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        openCamera();
                    } else {
                        showPermissionDeniedDialog("Camera/Storage");
                    }
                }
        );
    }

    private void setupClickListeners() {
        btnUpload.setOnClickListener(v -> checkGalleryPermissionAndOpen());
        btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndOpen());
    }

    // ========== UPLOAD IMAGE FUNCTIONALITY ==========

    private void checkGalleryPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } else {
            // Android 5 and below
            openGallery();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void handleSelectedImage(Uri imageUri) {
        selectedImageUri = imageUri;

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            selectedImageBitmap = BitmapFactory.decodeStream(inputStream);

            Log.d("AddImage", "handleSelectedImage: bitmap decoded");

            imageView.setImageBitmap(selectedImageBitmap);

            // Display image if you have an ImageView
            // imageView.setImageBitmap(selectedImageBitmap);

            // Optional: Save to gallery if needed
            // saveImageToGallery(selectedImageBitmap);

            Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show();

            openConfirmActivity();
            // Return the image URI or bitmap to calling activity if needed
            // returnImageResult();

            Log.d("AddImage", "handleSelectedImage: called openConfirmActivity");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    // ========== CAMERA FUNCTIONALITY ==========

    private void checkCameraPermissionAndOpen() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        // Check storage permission for saving photos
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            multiplePermissionLauncher.launch(permissionsNeeded.toArray(new String[0]));
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Create a file to save the photo
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            currentPhotoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider",
                    photoFile);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void showImagePreviewDialog() {
        // Load the captured image
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);

        // Create a dialog with ImageView to show preview
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save this photo?");

        // You can add a custom view with ImageView here to show preview
        // For simplicity, we'll just show text options

        builder.setMessage("Do you want to save this photo to gallery?");

        builder.setPositiveButton("Save", (dialog, which) -> {
            saveImageToGallery(bitmap);
        });

        builder.setNegativeButton("Retake", (dialog, which) -> {
            // Delete the temporary file and retake
            File file = new File(currentPhotoPath);
            if (file.exists()) {
                file.delete();
            }
            openCamera();
        });

        builder.setNeutralButton("Cancel", (dialog, which) -> {
            // Delete the temporary file
            File file = new File(currentPhotoPath);
            if (file.exists()) {
                file.delete();
            }
        });

        builder.show();
    }

    // ========== SAVE TO GALLERY ==========

    private void saveImageToGallery(Bitmap bitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";

        boolean saved = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (Use MediaStore)
            saved = saveImageToGalleryQ(bitmap, fileName);
        } else {
            // Android 9 and below
            saved = saveImageToGalleryLegacy(bitmap, fileName);
        }

        if (saved) {
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
            selectedImageBitmap = bitmap;
            // openConfirmActivity();
            // Display image if you have an ImageView
            // imageView.setImageBitmap(bitmap);

            // Return the result
            // returnImageResult();
        } else {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean saveImageToGalleryQ(Bitmap bitmap, String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                selectedImageUri = uri;
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean saveImageToGalleryLegacy(Bitmap bitmap, String fileName) {
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File imageFile = new File(storageDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            // Notify gallery about the new file
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            selectedImageUri = Uri.fromFile(imageFile);
            mediaScanIntent.setData(selectedImageUri);
            sendBroadcast(mediaScanIntent);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ========== RETURN RESULT ==========

//    private void returnImageResult() {
//        // If this activity was started for result, return the image
//        Intent resultIntent = new Intent();
//
//        if (selectedImageUri != null) {
//            resultIntent.putExtra("image_uri", selectedImageUri.toString());
//        }
//
//        if (currentPhotoPath != null) {
//            resultIntent.putExtra("image_path", currentPhotoPath);
//        }
//
//        // You can also pass the bitmap as byte array (be careful with size)
//        // ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        // selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
//        // byte[] byteArray = stream.toByteArray();
//        // resultIntent.putExtra("image_bitmap", byteArray);
//
//        setResult(RESULT_OK, resultIntent);
//
//        // Don't finish the activity automatically unless you want to
//        // finish();
//    }

    private void returnImageResult() {
        Intent confirmIntent = new Intent(this, ConfirmImageActivity.class);

        if (selectedImageUri != null) {
            confirmIntent.putExtra("image_uri", selectedImageUri.toString());
        }

        if (currentPhotoPath != null) {
            confirmIntent.putExtra("image_path", currentPhotoPath);
        }

        startActivity(confirmIntent);
    }


    // ========== PERMISSION HANDLING ==========

    private void showPermissionDeniedDialog(String permissionType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage(permissionType + " permission is required for this feature. Please grant the permission in Settings.");

        builder.setPositiveButton("Go to Settings", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openConfirmActivity() {
        Log.d("AddImage", "openConfirmActivity: selectedImageUri = " + selectedImageUri
                + ", currentPhotoPath = " + currentPhotoPath);

        Intent confirmIntent = new Intent(this, ConfirmImageActivity.class);

        if (selectedImageUri != null) {
            confirmIntent.putExtra("image_uri", selectedImageUri.toString());
            Log.d("AddImage", "openConfirmActivity: put image_uri extra");
        }

        if (currentPhotoPath != null) {
            confirmIntent.putExtra("image_path", currentPhotoPath);
            Log.d("AddImage", "openConfirmActivity: put image_path extra");
        }

        startActivity(confirmIntent);
        Log.d("AddImage", "openConfirmActivity: startActivity called");
    }


    // ========== PUBLIC METHODS TO GET IMAGE ==========

    public Uri getSelectedImageUri() {
        return selectedImageUri;
    }

    public Bitmap getSelectedImageBitmap() {
        return selectedImageBitmap;
    }

    public String getImagePath() {
        return currentPhotoPath;
    }

}
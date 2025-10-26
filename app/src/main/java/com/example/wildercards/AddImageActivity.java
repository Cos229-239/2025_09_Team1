package com.example.wildercards;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.material.button.MaterialButton;

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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Activity for adding images either by selecting from gallery or capturing with camera.
 * Handles permission requests and image orientation correction using EXIF data.
 */
public class AddImageActivity extends BaseActivity {

    // UI Components
    private MaterialButton btnUpload, btnTakePhoto;
    private ImageView imageView;

    // Image data holders
    private Uri currentPhotoUri;
    private String currentPhotoPath;
    private Uri selectedImageUri;
    private Bitmap selectedImageBitmap;

    // Activity Result Launchers for modern API
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;
    private ActivityResultLauncher<String[]> multiplePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_image);

        // Apply system window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initActivityLaunchers();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear the image view when returning to this activity
        if (imageView != null) {
            imageView.setImageBitmap(null);
        }
    }

    /**
     * Initialize UI components
     */
    private void initViews() {
        btnUpload = findViewById(R.id.btnUpload);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        imageView = findViewById(R.id.imageView);
    }

    /**
     * Initialize activity result launchers for gallery, camera, and permissions
     */
    private void initActivityLaunchers() {
        // Gallery selection launcher
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

        // Camera capture launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        showImagePreviewDialog();
                    }
                }
        );

        // Single permission launcher for gallery access
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

        // Multiple permissions launcher for camera access
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

    /**
     * Setup click listeners for buttons
     */
    private void setupClickListeners() {
        btnUpload.setOnClickListener(v -> checkGalleryPermissionAndOpen());
        btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndOpen());
    }

    // ========== GALLERY IMAGE SELECTION ==========

    /**
     * Check for appropriate storage permission based on Android version and open gallery
     */
    private void checkGalleryPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12 requires READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } else {
            // Android 5 and below - no runtime permissions needed
            openGallery();
        }
    }

    /**
     * Open gallery to select an image
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    /**
     * Handle the selected image from gallery
     * Applies EXIF orientation correction before displaying and saving
     */
    private void handleSelectedImage(Uri imageUri) {
        selectedImageUri = imageUri;

        try {
            // Load image with correct orientation from EXIF data
            selectedImageBitmap = getCorrectlyOrientedBitmap(imageUri);

            // Display the corrected image
            imageView.setImageBitmap(selectedImageBitmap);

            // Save to gallery
            saveImageToGallery(selectedImageBitmap);

            Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show();

            // Navigate to confirmation screen
            returnImageResult();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    // ========== CAMERA IMAGE CAPTURE ==========

    /**
     * Check for camera and storage permissions before opening camera
     */
    private void checkCameraPermissionAndOpen() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        // Check storage permission for Android 9 and below
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

    /**
     * Open camera to capture a photo
     */
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Create a file to save the full-resolution photo
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            // Get content URI using FileProvider for security
            currentPhotoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider",
                    photoFile);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
            cameraLauncher.launch(takePictureIntent);
        }
    }

    /**
     * Create a temporary image file to store the captured photo
     * @return File object for the new image
     */
    private File createImageFile() throws IOException {
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

    /**
     * Show dialog to confirm saving the captured photo or retaking it
     */
    private void showImagePreviewDialog() {
        // Load the captured image with correct orientation from EXIF data
        Bitmap bitmap = getCorrectlyOrientedBitmap(currentPhotoPath);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save this photo?");
        builder.setMessage("Do you want to save this photo to gallery?");

        builder.setPositiveButton("Save", (dialog, which) -> {
            saveImageToGallery(bitmap);
            returnImageResult();
        });

        builder.setNegativeButton("Retake", (dialog, which) -> {
            // Delete the temporary file and open camera again
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

    // ========== IMAGE ORIENTATION CORRECTION ==========

    /**
     * Load bitmap from file path with correct orientation using EXIF data
     * Camera photos often have rotation metadata that needs to be applied
     * @param imagePath File path to the image
     * @return Correctly oriented bitmap
     */
    private Bitmap getCorrectlyOrientedBitmap(String imagePath) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            return rotateBitmap(bitmap, orientation);
        } catch (IOException e) {
            e.printStackTrace();
            return BitmapFactory.decodeFile(imagePath);
        }
    }

    /**
     * Load bitmap from URI with correct orientation using EXIF data
     * @param uri Content URI of the image
     * @return Correctly oriented bitmap
     */
    private Bitmap getCorrectlyOrientedBitmap(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            inputStream = getContentResolver().openInputStream(uri);
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            inputStream.close();

            return rotateBitmap(bitmap, orientation);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Rotate or flip bitmap based on EXIF orientation value
     * @param bitmap Original bitmap
     * @param orientation EXIF orientation value
     * @return Rotated/flipped bitmap
     */
    private Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1, -1);
                break;
            default:
                return bitmap;
        }

        try {
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(),
                    matrix, true);
            bitmap.recycle();
            return rotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    // ========== SAVE IMAGE TO GALLERY ==========

    /**
     * Save bitmap to device gallery
     * Uses MediaStore API for Android 10+ and legacy file system for older versions
     * @param bitmap Bitmap to save
     */
    private void saveImageToGallery(Bitmap bitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";

        boolean saved = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ uses MediaStore API
            saved = saveImageToGalleryQ(bitmap, fileName);
        } else {
            // Android 9 and below use legacy file system
            saved = saveImageToGalleryLegacy(bitmap, fileName);
        }

        if (saved) {
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
            selectedImageBitmap = bitmap;
        } else {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Save image using MediaStore API (Android 10+)
     * @param bitmap Bitmap to save
     * @param fileName Desired file name
     * @return true if saved successfully
     */
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

    /**
     * Save image using legacy file system (Android 9 and below)
     * @param bitmap Bitmap to save
     * @param fileName Desired file name
     * @return true if saved successfully
     */
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

    /**
     * Navigate to ConfirmImageActivity with the selected/captured image URI
     */
    private void returnImageResult() {
        Intent confirmIntent = new Intent(this, ConfirmImageActivity.class);

        if (selectedImageUri != null) {
            confirmIntent.putExtra("image_uri", selectedImageUri.toString());
        } else if (currentPhotoUri != null) {
            confirmIntent.putExtra("image_uri", currentPhotoUri.toString());
        } else {
            Toast.makeText(this, "No image to confirm", Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(confirmIntent);
    }

    // ========== PERMISSION HANDLING ==========

    /**
     * Show dialog when permission is denied, with option to open app settings
     * @param permissionType Type of permission that was denied
     */
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

    // ========== PUBLIC GETTERS ==========

    /**
     * Get the URI of the selected/captured image
     * @return Image URI
     */
    public Uri getSelectedImageUri() {
        return selectedImageUri;
    }

    /**
     * Get the bitmap of the selected/captured image
     * @return Image bitmap
     */
    public Bitmap getSelectedImageBitmap() {
        return selectedImageBitmap;
    }

    /**
     * Get the file path of the captured image
     * @return Image file path
     */
    public String getImagePath() {
        return currentPhotoPath;
    }
}
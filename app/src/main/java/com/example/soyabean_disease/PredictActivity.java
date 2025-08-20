package com.example.soyabean_disease;


import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.content.Intent;

import org.tensorflow.lite.Interpreter;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;

public class PredictActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final float YOLO_CONFIDENCE_THRESHOLD = 0.5f;
    private static final float NMS_THRESHOLD = 0.5f;
    private static final float HEALTH_THRESHOLD = 0.6f;

    private ImageView imageView;
    private Button btnCapture, btnSelect, btnAnalyze;
    private TextView tvResult, tvConfidence;
    private ProgressBar progressBar;

    private Bitmap currentBitmap;
    private Interpreter yoloModel;
    private Uri tempCameraUri;
    private Toolbar toolbar;
    private Interpreter diseaseModel;

    private String[] diseaseClasses;

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private ActionBarDrawerToggle toggle;

    private float noLeafConfidence = 0.0f;



    private final int DISEASE_INPUT_SIZE = 224; // Assuming standard MobileNetV2 input size


// Import this if not already:


    // Add this field in your activity/class if not already:
    private Interpreter interpreter;

// Inside your setup or method:



    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) launchCropper(uri);

            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && tempCameraUri != null) launchCropper(tempCameraUri);

            });


    private void launchCropper(Uri imageUri) {
        CropImageOptions options = new CropImageOptions();
        options.fixAspectRatio = true;
        options.aspectRatioX = 1;
        options.aspectRatioY = 1;
        cropImageLauncher.launch(new CropImageContractOptions(imageUri, options));
    }


    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    try {
                        currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), result.getUriContent());
                        imageView.setImageBitmap(currentBitmap);
                        resetResults();
                        analyzeImage();
                    } catch (IOException e) {
                        showToast("Error loading cropped image");
                        Log.e("ImageLoad", "Error loading image", e);
                    }
                } else {
                    showToast("Cropping failed");
                }
            });

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_predict);

        loadModels();
        // Set up Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up DrawerLayout and NavigationView
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);

        // Toggle (Hamburger icon)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Optional: Handle drawer item clicks
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.nav_predict) {
                // Already on PredictActivity
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(PredictActivity.this, HistoryActivity.class));
            } else if (id == R.id.nav_about) {
                startActivity(new Intent(this, AboutActivity.class));

            }
            else if (id == R.id.nav_legal) {
                startActivity(new Intent(this, LegalNotices.class));
            }

            else if (id == R.id.nav_language) {
                showLanguageDialog();
            }
            drawerLayout.closeDrawers();
            return true;


        });
        initializeViews();
        setupListeners();


        diseaseClasses = new String[]{
                getString(R.string.mosaic_virus),
                getString(R.string.southern_blight),
                getString(R.string.sudden_death_syndrome),
                getString(R.string.yellow_mosaic),
                getString(R.string.bacterial_blight),
                getString(R.string.brown_spot),
                getString(R.string.crestamento),
                getString(R.string.ferrugen),
                getString(R.string.powdery_mildew),
                getString(R.string.septoria)
        };


    }

    private void showLanguageDialog() {
        final String[] languages = {"English", "Hindi","Gujarati","Tamil","Telugu"};

        final String[] codes = {"en", "hi","gu","ta","te"};


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Language");
        builder.setSingleChoiceItems(languages, -1, (dialog, which) -> {
            saveLocale(codes[which]);       // ✅ Save language code
            LocaleHelper.setLocale(this, codes[which]); // ✅ Set locale
            recreate(); // ✅ Restart to apply
            dialog.dismiss();
        });
        builder.create().show();
    }
    private void saveLocale(String langCode) {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("My_Lang", langCode);
        editor.apply();
    }


    private void setLocale(String langCode) {
        LocaleHelper.setLocale(this, langCode);
        recreate();
    }


    private void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "en");
        setLocale(language);
    }



    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return toggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        imageView = findViewById(R.id.imageView);
        btnCapture = findViewById(R.id.btnCapture);
        btnSelect = findViewById(R.id.btnSelect);

        tvResult = findViewById(R.id.tvResult);
        tvConfidence = findViewById(R.id.tvConfidence);
        progressBar = findViewById(R.id.progressBar);

        Toolbar toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

    }


    private void setupListeners() {

        btnCapture.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                tempCameraUri = createImageUri();
                cameraLauncher.launch(tempCameraUri);

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
        });


        btnSelect.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        // btnAnalyze.setOnClickListener(v -> {
        // if (currentBitmap != null) analyzeImage();
        // else showToast("Please select an image first");
        // });


    }

    private void loadModels() {
        try {
            // Load detection model
            MappedByteBuffer detectionModel = FileUtil.loadMappedFile(this, "best_float16.tflite");
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            options.setUseXNNPACK(true);
            interpreter = new Interpreter(detectionModel, options);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorDialog("Failed to load detection model.");
        }

        try {
            // Load classification model
            MappedByteBuffer classificationModel = FileUtil.loadMappedFile(this, "soybean_model.tflite");
            Interpreter.Options options2 = new Interpreter.Options();
            options2.setNumThreads(4);
            options2.setUseXNNPACK(true);
            diseaseModel = new Interpreter(classificationModel, options2);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorDialog("Failed to load classification model.");
        }
    }

    // Extracted common alert dialog code
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Model Load Error")
                .setMessage(message)
                .setPositiveButton("Exit", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void analyzeImage() {
        progressBar.setVisibility(View.VISIBLE);
        tvResult.setText("Analyzing...");
        tvConfidence.setText("");

        new Thread(() -> {
            // ✅ Null safety
            if (interpreter == null || diseaseModel == null || currentBitmap == null) {
                runOnUiThread(() -> {
                    tvResult.setText("Model not loaded or image is null.");
                    tvResult.setTextColor(Color.RED);
                    resetUI();
                });
                return;
            }

            // 🔍 Run detection
            Detection detection = detectLeafAndClassify(currentBitmap);

            // 🪵 Logging
            if (detection != null)
                Log.d("Detection", "ClassId: " + detection.classId + ", Confidence: " + detection.confidence);
            else
                Log.d("Detection", "Detection result is null");

            if (detection == null || detection.confidence < 0.4f) {
                runOnUiThread(() -> {
                    tvResult.setText(getString(R.string.no_soyabean_leaf_detected));
                    tvResult.setTextColor(Color.RED);
                    savePredictionToRoom(getString(R.string.no_soyabean_leaf_detected), 0.0f);
                    resetUI();
                });
                return;
            }

            runOnUiThread(() -> {
                switch (detection.classId) {
                    case 0: // Healthy
                        tvResult.setText(getString(R.string.healthy_soyabean_leaf));
                        tvResult.setTextColor(Color.GREEN);
                        savePredictionToRoom(getString(R.string.healthy_soyabean_leaf), detection.confidence);
                        break;

                    case 2: // Unhealthy
                        Bitmap cropped = cropDetection(currentBitmap, detection.box);
                        String diseaseName = classifyDisease(cropped);
                        tvResult.setText(getString(R.string.disease_detected) + "\n" + diseaseName);
                        tvResult.setTextColor(Color.RED);
                        savePredictionToRoom(diseaseName, detection.confidence);
                        break;

                    case 1: // Not leaf
                    default:
                        tvResult.setText(getString(R.string.no_soyabean_leaf_detected));
                        tvResult.setTextColor(Color.RED);
                        savePredictionToRoom(getString(R.string.no_soyabean_leaf_detected), detection.confidence);
                        break;
                }

                resetUI();
            });

        }).start();
    }



    private void savePredictionToRoom(String result, float confidence) {
        String imagePath = saveBitmapToInternalStorage(currentBitmap);
        long timestamp = System.currentTimeMillis();

        PredictionEntry entry = new PredictionEntry(imagePath, result, confidence, timestamp);

        new Thread(() -> {
            PredictionDatabase.getInstance(PredictActivity.this)
                    .predictionDao()
                    .insert(entry);
        }).start();
    }
    private String saveBitmapToInternalStorage(Bitmap bitmap) {
        try {
            String filename = "prediction_" + System.currentTimeMillis() + ".png";
            File file = new File(getFilesDir(), filename);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
    public static ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        int inputSize = 640;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true);
        int[] pixels = new int[inputSize * inputSize];
        resizedBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize);

        for (int pixel : pixels) {
            byteBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f); // R
            byteBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);  // G
            byteBuffer.putFloat((pixel & 0xFF) / 255.0f);         // B
        }

        return byteBuffer;
    }
    private Bitmap cropDetection(Bitmap source, RectF box) {
        int left = Math.max(0, Math.round(box.left));
        int top = Math.max(0, Math.round(box.top));
        int width = Math.min(source.getWidth() - left, Math.round(box.width()));
        int height = Math.min(source.getHeight() - top, Math.round(box.height()));
        return Bitmap.createBitmap(source, left, top, width, height);
    }


    private String classifyDisease(Bitmap croppedLeaf) {
        Bitmap resized = Bitmap.createScaledBitmap(croppedLeaf, DISEASE_INPUT_SIZE, DISEASE_INPUT_SIZE, true);
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * DISEASE_INPUT_SIZE * DISEASE_INPUT_SIZE * 3);
        inputBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[DISEASE_INPUT_SIZE * DISEASE_INPUT_SIZE];
        resized.getPixels(pixels, 0, DISEASE_INPUT_SIZE, 0, 0, DISEASE_INPUT_SIZE, DISEASE_INPUT_SIZE);

        for (int pixel : pixels) {
            inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
            inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
            inputBuffer.putFloat((pixel & 0xFF) / 255.0f);
        }

        float[][] output = new float[1][diseaseClasses.length];
        diseaseModel.run(inputBuffer, output);

        // Apply softmax
        float sum = 0f;
        for (float val : output[0]) sum += Math.exp(val);
        for (int i = 0; i < output[0].length; i++) output[0][i] = (float) Math.exp(output[0][i]) / sum;

        // Find best class
        int bestIndex = 0;
        float bestScore = output[0][0];
        for (int i = 1; i < output[0].length; i++) {
            if (output[0][i] > bestScore) {
                bestScore = output[0][i];
                bestIndex = i;
            }
        }

        Log.d("DiseasePrediction", "Best: " + diseaseClasses[bestIndex] + ", Prob: " + bestScore);
        return diseaseClasses[bestIndex];
    }

    private Detection detectLeafAndClassify(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 640, 640, true);

        // Prepare input
        ByteBuffer input = convertBitmapToByteBuffer(resized);  // Your image preprocessing method

        // Output shape for YOLOv8: [1][8400][7] → 4 bbox + 1 obj + 3 classes
        float[][][] output = new float[1][8400][7];

        // Run inference
        interpreter.run(input, output);

        // Parse detections
        float confidenceThreshold = 0.4f;
        Detection bestDetection = null;
        float bestScore = 0f;

        for (int i = 0; i < 8400; i++) {
            float[] prediction = output[0][i];

            float x = prediction[0];
            float y = prediction[1];
            float w = prediction[2];
            float h = prediction[3];
            float objectness = prediction[4];

            // class scores
            float class0 = prediction[5]; // healthy
            float class1 = prediction[6]; // notleaf
            float class2 = prediction[7]; // unhealthy

            // Get best class score & ID
            float[] classScores = new float[]{class0, class1, class2};
            int classId = 0;
            float maxClassScore = classScores[0];
            for (int j = 1; j < classScores.length; j++) {
                if (classScores[j] > maxClassScore) {
                    maxClassScore = classScores[j];
                    classId = j;
                }
            }

            float score = objectness * maxClassScore;
            if (score > confidenceThreshold && score > bestScore) {
                // Convert center x/y + width/height → bounding box
                float left = x - w / 2;
                float top = y - h / 2;
                float right = x + w / 2;
                float bottom = y + h / 2;

                RectF box = new RectF(left, top, right, bottom);
                bestDetection = new Detection(box, classId, score);
                bestScore = score;
            }
        }

        return bestDetection; // may be null if no detection exceeds threshold
    }

    private List<Detection> applyNMS(List<Detection> detections) {
        List<Detection> result = new ArrayList<>();
        PriorityQueue<Detection> pq = new PriorityQueue<>((a, b) -> Float.compare(b.confidence, a.confidence));
        pq.addAll(detections);

        while (!pq.isEmpty()) {
            Detection curr = pq.poll();
            result.add(curr);
            detections.remove(curr);
            detections.removeIf(d -> iou(curr.box, d.box) > NMS_THRESHOLD);
        }
        return result;
    }

    private float iou(@NonNull RectF a, @NonNull RectF b) {
        float areaA = (a.right - a.left) * (a.bottom - a.top);
        float areaB = (b.right - b.left) * (b.bottom - b.top);

        float intersectionLeft = Math.max(a.left, b.left);
        float intersectionTop = Math.max(a.top, b.top);
        float intersectionRight = Math.min(a.right, b.right);
        float intersectionBottom = Math.min(a.bottom, b.bottom);

        float intersectionWidth = Math.max(0, intersectionRight - intersectionLeft);
        float intersectionHeight = Math.max(0, intersectionBottom - intersectionTop);

        float intersectionArea = intersectionWidth * intersectionHeight;
        float unionArea = areaA + areaB - intersectionArea;

        return intersectionArea / unionArea;
    }


    private @NonNull MappedByteBuffer loadModel(String name) throws IOException {
        return FileUtil.loadMappedFile(this, name);
    }

    private Uri createImageUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "leaf_" + System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void resetUI() {
        progressBar.setVisibility(View.GONE);
        //btnAnalyze.setEnabled(true);
    }

    private void resetResults() {
        tvResult.setText("");
        tvConfidence.setText("");
        tvResult.setTextColor(Color.BLACK);
    }

    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    private static class Detection {
        public RectF box;
        float confidence;
        int classId;




        public Detection(RectF box, int classId, float confidence) {
            this.box = box;
            this.classId = classId;
            this.confidence = confidence;

        }

    }
}
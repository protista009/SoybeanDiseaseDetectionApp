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
    private static final float YOLO_CONFIDENCE_THRESHOLD = 0.4f;
    private static final float NMS_THRESHOLD = 0.5f;
    private static final float HEALTH_THRESHOLD = 0.4f;

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
        loadModels();

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
        final String[] languages = {"English", "Hindi"};
        final String[] codes = {"en", "hi"};

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
        Interpreter.Options options = null;
        try {
            options = new Interpreter.Options();
            options.setNumThreads(4);
            options.setUseXNNPACK(true);
            yoloModel = new Interpreter(loadModel("soybean_model_yolo.tflite"), options);
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Model Load Error")
                    .setMessage("Failed to load TFLite model.")
                    .setPositiveButton("Exit", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();

        }
        try {
            options = new Interpreter.Options();
            options.setNumThreads(4);
            options.setUseXNNPACK(true);
            diseaseModel = new Interpreter(loadModel("soybean_model.tflite"), options);
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Model Load Error")
                    .setMessage("Failed to load TFLite model.")
                    .setPositiveButton("Exit", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();

        }



    }

    private void analyzeImage() {
        progressBar.setVisibility(View.VISIBLE);
        tvResult.setText("Analyzing...");
        tvConfidence.setText("");

        new Thread(() -> {
            Detection detection = detectLeafAndClassify();

            if (detection == null) {
                runOnUiThread(() -> {
                    tvResult.setText(getString(R.string.no_soyabean_leaf_detected));
                    tvResult.setTextColor(Color.RED);
                    savePredictionToRoom(getString(R.string.no_soyabean_leaf_detected), noLeafConfidence);
                    resetUI();
                });
                return;
            }

            runOnUiThread(() -> {
                switch (detection.classId) {
                    case 0: // Healthy
                        if (detection.confidence > HEALTH_THRESHOLD) {
                            tvResult.setText(getString(R.string.healthy_soyabean_leaf));
                            tvResult.setTextColor(Color.GREEN);
                            savePredictionToRoom(getString(R.string.healthy_soyabean_leaf), detection.confidence);
                        } else {
                            tvResult.setText(getString(R.string.uncertain));
                            tvResult.setTextColor(Color.YELLOW);
                            savePredictionToRoom(getString(R.string.uncertain), detection.confidence);
                        }
                        break;

                    case 2: // Unhealthy
                        if (detection.confidence > 0.4f) {
                            Bitmap cropped = cropDetection(currentBitmap, detection.box);
                            String diseaseName = classifyDisease(cropped); // 👈 secondary model
                            tvResult.setText(getString(R.string.disease_detected) + "\n" + diseaseName);
                            tvResult.setTextColor(Color.RED);
                            savePredictionToRoom(diseaseName, detection.confidence);
                        } else {
                            tvResult.setText(getString(R.string.uncertain));
                            tvResult.setTextColor(Color.YELLOW);
                            savePredictionToRoom(getString(R.string.uncertain), detection.confidence);
                        }
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
        int inputSize = 640; // Fixed internally
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true);
        int[] pixels = new int[inputSize * inputSize];
        resizedBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize);

        for (int pixel : pixels) {
            byteBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f); // Red
            byteBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);  // Green
            byteBuffer.putFloat((pixel & 0xFF) / 255.0f);         // Blue
        }

        return byteBuffer;
    }
    private Bitmap cropDetection(Bitmap bitmap, float[] box) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int left = Math.max(0, (int) (box[0] * width / 640));
        int top = Math.max(0, (int) (box[1] * height / 640));
        int right = Math.min(width, (int) (box[2] * width / 640));
        int bottom = Math.min(height, (int) (box[3] * height / 640));

        int cropWidth = right - left;
        int cropHeight = bottom - top;

        if (cropWidth <= 0 || cropHeight <= 0) {
            Log.e("CropDetection", "Invalid crop size: " + cropWidth + "x" + cropHeight);
            return bitmap; // Fallback to entire image if box is invalid
        }

        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight);
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

    private Detection detectLeafAndClassify() {
        try {
            Bitmap resized = Bitmap.createScaledBitmap(currentBitmap, 640, 640, true);
            ByteBuffer input = convertBitmapToByteBuffer(resized);

            float[][][] output = new float[1][25200][8];  // updated: 8 values per prediction
            yoloModel.run(input, output);

            List<Detection> detections = new ArrayList<>();
            float maxConfidenceSeen = 0.0f;

            for (float[] pred : output[0]) {
                float x = pred[0];
                float y = pred[1];
                float w = pred[2];
                float h = pred[3];
                float objectness = pred[4];

                // Softmax across 3 classes
                float[] classScores = new float[3];
                float expSum = 0f;
                for (int i = 0; i < 3; i++) {
                    classScores[i] = (float) Math.exp(pred[5 + i]);
                    expSum += classScores[i];
                }
                for (int i = 0; i < 3; i++) {
                    classScores[i] /= expSum;
                }

                // Find best class
                int classId = 0;
                float classProb = classScores[0];
                for (int i = 1; i < 3; i++) {
                    if (classScores[i] > classProb) {
                        classProb = classScores[i];
                        classId = i;
                    }
                }

                float confidence = objectness * classProb;
                if (confidence > maxConfidenceSeen) {
                    maxConfidenceSeen = confidence;
                }

                if (confidence > YOLO_CONFIDENCE_THRESHOLD) {
                    float[] box = {
                            x - w / 2, y - h / 2,
                            x + w / 2, y + h / 2
                    };
                    detections.add(new Detection(box, confidence, classId));
                }
            }

            List<Detection> finalDetections = applyNMS(detections);
            if (!finalDetections.isEmpty()) {
                return finalDetections.get(0); // Return best detection
            }
            noLeafConfidence = maxConfidenceSeen;

        } catch (Exception e) {
            Log.e("YOLO", "Detection error", e);
        }
        return null;
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

    private float iou(float[] a, float[] b) {
        float x1 = Math.max(a[0], b[0]);
        float y1 = Math.max(a[1], b[1]);
        float x2 = Math.min(a[2], b[2]);
        float y2 = Math.min(a[3], b[3]);
        float inter = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        float areaA = (a[2] - a[0]) * (a[3] - a[1]);
        float areaB = (b[2] - b[0]) * (b[3] - b[1]);
        return inter / (areaA + areaB - inter);
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
        float[] box;
        float confidence;
        int classId;

        Detection(float[] box, float confidence, int classId) {
            this.box = box;
            this.confidence = confidence;
            this.classId = classId;
        }
    }
} 
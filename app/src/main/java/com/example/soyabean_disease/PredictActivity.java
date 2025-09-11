package com.example.soyabean_disease;

import android.Manifest;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;

import android.location.Location;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
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

import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;




import org.tensorflow.lite.gpu.GpuDelegate;  // Correct import for TFLite GPU delegate


import com.google.android.material.navigation.NavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;


import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.Date;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PredictActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final float YOLO_CONFIDENCE_THRESHOLD = 0.4f;
    private static final float NMS_THRESHOLD = 0.5f;
    private static final float HEALTH_THRESHOLD = 0.4f;

    private String[] diseaseClasses;

    private ImageView imageView;
    private Button btnCapture, btnSelect;
    private TextView tvResult, tvConfidence;


    private ProgressBar progressBar;

    private Bitmap currentBitmap;
    private Interpreter diseaseModel;
    private Interpreter yoloModel;

    private Uri tempCameraUri;



    private ActionBarDrawerToggle toggle;




    private final float noLeafConfidence = 0.0f;






    // --- Inference config ---
    private static final int YOLO_INPUT = 640;
    private static final int YOLO_OUTPUT_BOXES = 25200; // your model
    private static final int YOLO_OUTPUT_ATTR = 7;      // [x,y,w,h,obj,class0,class1]

    // Delegates & options
    private GpuDelegate gpuDelegate;

    // Reusable buffers to avoid GC churn
    private ByteBuffer yoloInputBuffer;
    private final int[] yoloPixels = new int[YOLO_INPUT * YOLO_INPUT];

    private final int DISEASE_INPUT_SIZE = 224;
    private ByteBuffer clsInputBuffer;
    private final int[] clsPixels = new int[DISEASE_INPUT_SIZE * DISEASE_INPUT_SIZE];
    private Button btnShowPrecautions;





    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) launchCropper(uri);

            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && tempCameraUri != null) launchCropper(tempCameraUri);

            });





    private void launchCropper(Uri imageUri) {
        CropImageOptions cropOptions = new CropImageOptions();
        cropOptions.activityTitle="Edit Image";
        cropOptions.cropMenuCropButtonTitle="Done";
        cropOptions.fixAspectRatio = true;
        cropOptions.aspectRatioX = 1;
        cropOptions.aspectRatioY = 1;

        CropImageContractOptions options = new CropImageContractOptions(imageUri, cropOptions);

        cropImageLauncher.launch(options);
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
    private void fetchWeather(double lat, double lon, TextView tvTemp, TextView tvLocation) {
        String apiKey = "9d6b6789f75815313ee65b13a56fb209"; // 🔑 Replace with your actual API key
        String userLang = Locale.getDefault().getLanguage();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApi api = retrofit.create(WeatherApi.class);
        Call<WeatherResponse> call = api.getWeather(lat, lon, apiKey, "metric", userLang);
        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@androidx.annotation.NonNull Call<WeatherResponse> call, @androidx.annotation.NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse data = response.body();
                    tvTemp.setText(String.format(Locale.getDefault(), "%.1f°C", data.main.temp));
                    tvLocation.setText(data.name);

                } else {
                    Toast.makeText(PredictActivity.this, "Weather data unavailable", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@androidx.annotation.NonNull Call<WeatherResponse> call, @androidx.annotation.NonNull Throwable t) {
                Toast.makeText(PredictActivity.this, "Weather fetch failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    FusedLocationProviderClient fusedLocationClient;
    private ProgressBar loadingSpinner;

   
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predict);

        // Toolbar + Drawer setup
        setupToolbarAndDrawer();

        // Views
        TextView tvTemperature = findViewById(R.id.tvTemperature);
        TextView tvDate = findViewById(R.id.tvDate);
        TextView tvTime = findViewById(R.id.tvTime);
        TextView tvLocation = findViewById(R.id.tvLocation);
        loadingSpinner = findViewById(R.id.loadingSpinner); // add this in layout

        // Show current date & time immediately
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        tvDate.setText(sdfDate.format(new Date()));
        tvTime.setText(sdfTime.format(new Date()));

        // ✅ Load weather after UI is ready
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fetchWeatherAsync(tvTemperature, tvLocation);

        // ✅ Load ML models in background
        showLoadingSpinner();
        Executors.newSingleThreadExecutor().execute(() -> {
            loadModels();  // heavy operation
            runOnUiThread(() -> {
                hideLoadingSpinner();
                Toast.makeText(this, "loaded!", Toast.LENGTH_SHORT).show();
            });
        });

        // Disease classes (just strings, safe to set here)
        diseaseClasses = new String[]{
                getString(R.string.caterpillar),
                getString(R.string.sudden_death_syndrome),
                getString(R.string.yellow_mosaic),
//                getString(R.string.brown_spot),
//                getString(R.string.frog_eye),
//                getString(R.string.powdery_mildew),

        };

        initializeViews();
        setupListeners();
    }

    private void hideLoadingSpinner() {
        if (loadingSpinner != null) loadingSpinner.setVisibility(View.GONE);
    }

    private void showLoadingSpinner() {
        if (loadingSpinner != null) loadingSpinner.setVisibility(View.VISIBLE);
    }

    private void fetchWeatherAsync(TextView tvTemperature, TextView tvLocation) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // ✅ Cached location available
                        Executors.newSingleThreadExecutor().execute(() -> {
                            fetchWeather(location.getLatitude(), location.getLongitude(), tvTemperature, tvLocation);
                        });
                    } else {
                        LocationRequest locationRequest;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // ✅ Android 12+ (API 31 and above)
                            locationRequest = new LocationRequest.Builder(10000L) // interval
                                    .setMinUpdateIntervalMillis(5000L) // fastest interval
                                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                                    .build();
                        } else {
                            // ✅ Older versions
                            locationRequest = LocationRequest.create()
                                    .setInterval(10000L)
                                    .setFastestInterval(5000L)
                                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        }

                        fusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                new LocationCallback() {
                                    @Override
                                    public void onLocationResult(@androidx.annotation.NonNull LocationResult locationResult) {
                                        if (!locationResult.getLocations().isEmpty()) {
                                            Location freshLocation = locationResult.getLastLocation();

                                            Executors.newSingleThreadExecutor().execute(() -> {
                                                assert freshLocation != null;
                                                fetchWeather(freshLocation.getLatitude(),
                                                        freshLocation.getLongitude(),
                                                        tvTemperature, tvLocation);
                                            });

                                            // ✅ Stop updates after first fix
                                            fusedLocationClient.removeLocationUpdates(this);
                                        }
                                    }
                                },
                                Looper.getMainLooper()
                        );
                    }
                });
    }


    private void setupToolbarAndDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.nav_predict) {
                startActivity(new Intent(PredictActivity.this, PredictActivity.class));
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(PredictActivity.this, HistoryActivity.class));
            } else if (id == R.id.nav_about) {
                startActivity(new Intent(this, AboutActivity.class));
            } else if (id == R.id.nav_legal) {
                startActivity(new Intent(this, LegalNotices.class));
            } else if (id == R.id.nav_language) {
                showLanguageDialog();
            }
            drawerLayout.closeDrawers();

            return true;
        });
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







    @Override
    public boolean onOptionsItemSelected(@androidx.annotation.NonNull @NonNull MenuItem item) {
        return toggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        imageView = findViewById(R.id.imageView);
        btnCapture = findViewById(R.id.btnCapture);
        btnSelect = findViewById(R.id.btnSelect);

        tvResult = findViewById(R.id.tvResult);
        tvConfidence = findViewById(R.id.tvConfidence);
        progressBar = findViewById(R.id.progressBar);
        btnShowPrecautions = findViewById(R.id.btnShowPrecautions);
        btnShowPrecautions.setVisibility(View.GONE);



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
        // Create model options (GPU first, then fall back to CPU/XNNPACK if GPU fails)
        Interpreter.Options clsOptions;
        Interpreter.Options yoloOptions;
        try {
            gpuDelegate = new GpuDelegate();
            yoloOptions = new Interpreter.Options().addDelegate(gpuDelegate);
            yoloOptions.setNumThreads(Runtime.getRuntime().availableProcessors());
            clsOptions = new Interpreter.Options().addDelegate(gpuDelegate);
            clsOptions.setNumThreads(Runtime.getRuntime().availableProcessors());
        } catch (Throwable t) {
            Log.w("TFLite", "GPU delegate not available, falling back to CPU", t);
            yoloOptions = new Interpreter.Options();
            clsOptions = new Interpreter.Options();
            yoloOptions.setUseXNNPACK(true);
            clsOptions.setUseXNNPACK(true);
            yoloOptions.setNumThreads(4);
            clsOptions.setNumThreads(4);
        }

        try {
            yoloModel = new Interpreter(loadModel("best-fp167.tflite"), yoloOptions);
        } catch (Exception e) {
            Log.e("ModelLoad", "Failed to init YOLO model", e);
            showModelError("Failed to load leaf detector model.");
            return;
        }

        try {
            diseaseModel = new Interpreter(loadModel("mobilenetv2_model2.tflite"), clsOptions);
        } catch (Exception e) {
            Log.e("ModelLoad", "Failed to init classifier model", e);
            showModelError("Failed to load disease classification model.");
            return;
        }

        // Allocate reusable direct buffers once
        yoloInputBuffer = ByteBuffer.allocateDirect(YOLO_INPUT * YOLO_INPUT * 3 * 4).order(ByteOrder.nativeOrder());
        clsInputBuffer = ByteBuffer.allocateDirect(DISEASE_INPUT_SIZE * DISEASE_INPUT_SIZE * 3 * 4).order(ByteOrder.nativeOrder());
    }

    private void showModelError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Model Load Error")
                .setMessage(message)
                .setPositiveButton("Exit", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }


    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void analyzeImage() {
        progressBar.setVisibility(View.VISIBLE);
        tvResult.setText("Analyzing...");
        tvConfidence.setText("");


        new Thread(() -> {
            Detection detection = detectLeafAndClassify(currentBitmap);

            if (detection == null) {
                runOnUiThread(() -> {
                    tvResult.setText(getString(R.string.no_soyabean_leaf_detected)
                            + "\n"
                            + getString(R.string.desclaimer));
                    tvResult.setTextColor(Color.RED);
                    tvConfidence.setTextColor(Color.RED);






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
                            tvConfidence.setText(String.format("Confidence: %.2f%%", detection.confidence * 100));
                            tvConfidence.setTextColor(Color.GREEN);



                            savePredictionToRoom(getString(R.string.healthy_soyabean_leaf), detection.confidence);
                        } else {
                            tvResult.setText(getString(R.string.uncertain));
                            tvResult.setTextColor(Color.YELLOW);
                            tvConfidence.setText(String.format("Confidence: %.2f%%", detection.confidence * 100));
                            tvConfidence.setTextColor(Color.YELLOW);



                            savePredictionToRoom(getString(R.string.uncertain), detection.confidence);
                        }
                        break;

                    case 1: // Unhealthy
                        if (detection.confidence > 0.3f) {
                            Bitmap cropped = cropDetection(currentBitmap, detection.box);
                            String diseaseName = classifyDisease(cropped); // 👈 secondary model
                            tvResult.setText(getString(R.string.disease_detected) + "\n" + diseaseName ) ;
                            tvResult.setTextColor(Color.RED);
                            tvConfidence.setText(String.format("Confidence: %.2f%%", detection.confidence * 100));
                            tvConfidence.setTextColor(Color.RED);

                            btnShowPrecautions.setVisibility(View.VISIBLE);

                            // Handle button click
                            btnShowPrecautions.setOnClickListener(v -> {
                                Intent intent = new Intent(PredictActivity.this, MainActivity.class);
                                intent.putExtra("disease_name", diseaseName);
                                startActivity(intent);
                            });

                            savePredictionToRoom(diseaseName, detection.confidence);
                        } else {
                            tvResult.setText(getString(R.string.uncertain));
                            tvResult.setTextColor(Color.YELLOW);
                            tvConfidence.setText(String.format("Confidence: %.2f%%", detection.confidence * 100));
                            tvConfidence.setTextColor(Color.YELLOW);





                            savePredictionToRoom(getString(R.string.uncertain), detection.confidence);
                        }
                        break;


                    default:
                        tvResult.setText(getString(R.string.no_soyabean_leaf_detected)
                                + "\n"
                                + getString(R.string.desclaimer));
                        tvResult.setTextColor(Color.RED);
                        tvConfidence.setText(String.format("Confidence: %.2f%%", detection.confidence * 100));
                        tvConfidence.setTextColor(Color.RED);


                        savePredictionToRoom(getString(R.string.no_soyabean_leaf_detected), detection.confidence);
                        break;
                }

                resetUI();
            });

        }).start();
    }

    private void showPrecautions(String diseaseName) {

    }

    private void savePredictionToRoom(String result, float confidence) {
        String imagePath = saveBitmapToInternalStorage(currentBitmap);
        long timestamp = System.currentTimeMillis();


        PredictionEntry entry = new PredictionEntry(imagePath, result, confidence, timestamp);

        new Thread(() -> PredictionDatabase.getInstance(PredictActivity.this)
                .predictionDao()
                .insert(entry)).start();
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
    private void fillYoloInputBuffer(Bitmap src) {
        // Resize into a temporary 640x640 bitmap
        Bitmap resized = Bitmap.createScaledBitmap(src, YOLO_INPUT, YOLO_INPUT, true);
        resized.getPixels(yoloPixels, 0, YOLO_INPUT, 0, 0, YOLO_INPUT, YOLO_INPUT);

        yoloInputBuffer.rewind();
        for (int p : yoloPixels) {
            // Normalize 0..1 floats
            yoloInputBuffer.putFloat(((p >> 16) & 0xFF) / 255.0f); // R
            yoloInputBuffer.putFloat(((p >> 8) & 0xFF) / 255.0f);  // G
            yoloInputBuffer.putFloat((p & 0xFF) / 255.0f);         // B
        }
    }

    private Bitmap cropDetection(Bitmap bitmap, RectF box) {
        int left = Math.max(0, (int) box.left);
        int top = Math.max(0, (int) box.top);
        int right = Math.min(bitmap.getWidth(), (int) box.right);
        int bottom = Math.min(bitmap.getHeight(), (int) box.bottom);

        int cropWidth = right - left;
        int cropHeight = bottom - top;

        if (cropWidth <= 0 || cropHeight <= 0) {
            Log.e("CropDetection", "Invalid crop size: " + cropWidth + "x" + cropHeight);
            return bitmap; // Fallback to full image
        }

        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight);
    }


    //    private String classifyDisease(Bitmap croppedLeaf) {
//        Bitmap resized = Bitmap.createScaledBitmap(croppedLeaf, DISEASE_INPUT_SIZE, DISEASE_INPUT_SIZE, true);
//        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * DISEASE_INPUT_SIZE * DISEASE_INPUT_SIZE * 3);
//        inputBuffer.order(ByteOrder.nativeOrder());
//
//        int[] pixels = new int[DISEASE_INPUT_SIZE * DISEASE_INPUT_SIZE];
//        resized.getPixels(pixels, 0, DISEASE_INPUT_SIZE, 0, 0, DISEASE_INPUT_SIZE, DISEASE_INPUT_SIZE);
//
//        for (int pixel : pixels) {
//            inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
//            inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
//            inputBuffer.putFloat((pixel & 0xFF) / 255.0f);
//        }
//
//        float[][] output = new float[1][diseaseClasses.length];
//        diseaseModel.run(inputBuffer, output);
//
//        // Apply softmax
//        float sum = 0f;
//        for (float val : output[0]) sum += Math.exp(val);
//        for (int i = 0; i < output[0].length; i++) output[0][i] = (float) Math.exp(output[0][i]) / sum;
//
//        // Find best class
//        int bestIndex = 0;
//        float bestScore = output[0][0];
//        for (int i = 1; i < output[0].length; i++) {
//            if (output[0][i] > bestScore) {
//                bestScore = output[0][i];
//                bestIndex = i;
//            }
//        }
//
//        Log.d("DiseasePrediction", "Best: " + diseaseClasses[bestIndex] + ", Prob: " + bestScore);
//        return diseaseClasses[bestIndex];
//    }
    private String classifyDisease(Bitmap croppedLeaf) {
        Bitmap resized = Bitmap.createScaledBitmap(croppedLeaf, DISEASE_INPUT_SIZE, DISEASE_INPUT_SIZE, true);
        resized.getPixels(clsPixels, 0, DISEASE_INPUT_SIZE, 0, 0, DISEASE_INPUT_SIZE, DISEASE_INPUT_SIZE);

        clsInputBuffer.rewind();
        for (int p : clsPixels) {
            clsInputBuffer.putFloat(((p >> 16) & 0xFF) / 255.0f);
            clsInputBuffer.putFloat(((p >> 8) & 0xFF) / 255.0f);
            clsInputBuffer.putFloat((p & 0xFF) / 255.0f);
        }

        float[][] output = new float[1][3]; // two classes in your current setup
        diseaseModel.run(clsInputBuffer, output);

        // Softmax + argmax
        float sum = 0f;
        for (int i = 0; i < 3; i++) {
            sum += Math.exp(output[0][i]);
        }

        int bestIdx = 0;
        float bestProb = 0f;
        for (int i = 0; i < 3; i++) {
            float prob = (float) Math.exp(output[0][i]) / sum;
            if (prob > bestProb) {
                bestProb = prob;
                bestIdx = i;
            }
        }

        Log.d("DiseasePrediction", "Best: " + diseaseClasses[bestIdx] + ", Prob: " + bestProb);
        return diseaseClasses[bestIdx];
    }


    private Detection detectLeafAndClassify(Bitmap original) {
        // 1) Preprocess (fills yoloInputBuffer)
        fillYoloInputBuffer(original);

        // 2) Inference
        float[][][] yoloOutput = new float[1][YOLO_OUTPUT_BOXES][YOLO_OUTPUT_ATTR];
        yoloModel.run(yoloInputBuffer, yoloOutput);

        // 3) Find best detection (you can swap in NMS if you want multiple)
        float bestScore = 0f;
        int bestClassId = -1;
        RectF bestBox640 = null;

        for (int i = 0; i < YOLO_OUTPUT_BOXES; i++) {
            float[] p = yoloOutput[0][i];

            float cx = p[0];
            float cy = p[1];
            float w  = p[2];
            float h  = p[3];
            float obj = p[4];

            float s0 = p[5]; // healthy
            float s1 = p[6]; // unhealthy

            // class with max score
            int classId = (s1 > s0) ? 1 : 0;
            float classScore = Math.max(s0, s1);
            float score = obj * classScore;

            if (score < YOLO_CONFIDENCE_THRESHOLD) continue;

            // box in 640x640 space (your model outputs normalized 0..1 or absolute?)
            // If normalized (0..1), multiply by 640; if already absolute, skip the multiply.
            // Most TFLite YOLO heads export normalized — so multiply:
            float cx640 = cx * YOLO_INPUT;
            float cy640 = cy * YOLO_INPUT;
            float w640  = w  * YOLO_INPUT;
            float h640  = h  * YOLO_INPUT;

            float left   = cx640 - w640 / 2f;
            float top    = cy640 - h640 / 2f;
            float right  = cx640 + w640 / 2f;
            float bottom = cy640 + h640 / 2f;

            // Clamp to 640x640
            left = Math.max(0, Math.min(YOLO_INPUT - 1, left));
            top = Math.max(0, Math.min(YOLO_INPUT - 1, top));
            right = Math.max(0, Math.min(YOLO_INPUT - 1, right));
            bottom = Math.max(0, Math.min(YOLO_INPUT - 1, bottom));

            if (score > bestScore) {
                bestScore = score;
                bestClassId = classId;
                bestBox640 = new RectF(left, top, right, bottom);
            }
        }

        if (bestBox640 == null) return null;

        // 4) Map 640x640 box back to original size
        float scaleX = (float) original.getWidth() / YOLO_INPUT;
        float scaleY = (float) original.getHeight() / YOLO_INPUT;
        RectF mapped = new RectF(
                bestBox640.left * scaleX,
                bestBox640.top * scaleY,
                bestBox640.right * scaleX,
                bestBox640.bottom * scaleY
        );

        return new Detection(mapped, bestClassId, bestScore);
    }



    private List<Detection> applyNMS(List<Detection> detections) {
        List<Detection> result = new ArrayList<>();
        PriorityQueue<Detection> pq = new PriorityQueue<>((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        pq.addAll(detections);

        while (!pq.isEmpty()) {
            Detection curr = pq.poll();
            result.add(curr);
            detections.remove(curr);
            detections.removeIf(d -> {
                assert curr != null;
                return iou(curr.getBox(), d.getBox()) > NMS_THRESHOLD;
            });
        }
        return result;
    }


    private float iou(RectF a, RectF b) {
        float x1 = Math.max(a.left, b.left);
        float y1 = Math.max(a.top, b.top);
        float x2 = Math.min(a.right, b.right);
        float y2 = Math.min(a.bottom, b.bottom);

        float interArea = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        float areaA = (a.right - a.left) * (a.bottom - a.top);
        float areaB = (b.right - b.left) * (b.bottom - b.top);

        return interArea / (areaA + areaB - interArea);
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { if (yoloModel != null) yoloModel.close(); } catch (Throwable ignored) {}
        try { if (diseaseModel != null) diseaseModel.close(); } catch (Throwable ignored) {}
        try { if (gpuDelegate != null) gpuDelegate.close(); } catch (Throwable ignored) {}
    }



    public static class Detection {
        private final RectF box;
        private final float confidence;
        private final int classId;

        public Detection(RectF box, int classId, float confidence) {
            this.box = box;
            this.classId = classId;
            this.confidence = confidence;
        }

        public RectF getBox() { return box; }
        public float getConfidence() { return confidence; }
        public int getClassId() { return classId; }
    }


}



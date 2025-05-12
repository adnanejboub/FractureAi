package com.fractureai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class uploadImagesActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private LinearLayout uploadArea;
    private ImageView uploadIcon;
    private TextView uploadText, progressText, analysisInfoTitle, analysisInfoSubtitle;
    private ProgressBar progressBar;
    private Button btnProcessAnalysis;
    private LinearLayout loadingContainer;
    private TextView loadingText;
    private ImageView loadingAnimation;
    private LinearLayout loadingOverlay;
    private ImageView overlayLoadingIcon;
    private Uri imageUri;
    private Bitmap selectedBitmap;
    private Interpreter tfliteInterpreter;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String TAG = "UploadImagesActivity";
    private static final int MAX_FIRESTORE_SIZE = 1_048_576; // Limite Firestore en bytes (1 Mo)

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    try {
                        selectedBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                        Log.d(TAG, "Image chargée : " + (selectedBitmap != null ? "Oui" : "Non"));
                        if (selectedBitmap != null) {
                            uploadIcon.setImageBitmap(selectedBitmap);
                            uploadIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            uploadText.setVisibility(View.GONE);
                            btnProcessAnalysis.setEnabled(true);
                            analysisInfoTitle.setText("Image chargée");
                            analysisInfoSubtitle.setText("Prête pour l'analyse");

                            Animation zoomAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in_out);
                            uploadIcon.startAnimation(zoomAnimation);

                            loadingContainer.setVisibility(View.VISIBLE);
                            loadingAnimation.setImageResource(R.drawable.ic_success);
                            loadingText.setText("Image chargée avec succès !");
                            Animation successAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
                            loadingAnimation.startAnimation(successAnimation);

                            Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);
                            btnProcessAnalysis.startAnimation(pulseAnimation);
                        } else {
                            Log.e(TAG, "selectedBitmap est null");
                            Toast.makeText(this, "Erreur lors du chargement de l'image", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Erreur lors du chargement de l'image : " + e.getMessage());
                        Toast.makeText(this, "Erreur lors du chargement de l'image", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "URI de l'image est null");
                }
            });

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    imagePickerLauncher.launch("image/*");
                } else {
                    Toast.makeText(this, "Permission de stockage refusée. Impossible de sélectionner une image.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        try {
            setContentView(R.layout.activity_upload_images);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du chargement du layout : " + e.getMessage());
            Toast.makeText(this, "Erreur de chargement de l'interface", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, SignInEmail.class);
            startActivity(intent);
            finish();
            return;
        }

        try {
            initViews();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation des vues : " + e.getMessage());
            Toast.makeText(this, "Erreur d'initialisation de l'interface", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupListeners();
        loadLocalModel();
        updateNavigationMenu();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        btnMenu = findViewById(R.id.btn_menu);
        navigationView = findViewById(R.id.nav_view);
        uploadArea = findViewById(R.id.upload_area);
        uploadIcon = findViewById(R.id.upload_icon);
        uploadText = findViewById(R.id.upload_text);
        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);
        btnProcessAnalysis = findViewById(R.id.btn_process_analysis);
        analysisInfoTitle = findViewById(R.id.analysis_info_title);
        analysisInfoSubtitle = findViewById(R.id.analysis_info_subtitle);
        loadingContainer = findViewById(R.id.loading_container);
        loadingText = findViewById(R.id.loading_text);
        loadingAnimation = findViewById(R.id.loading_animation);
        loadingOverlay = findViewById(R.id.loading_overlay);
        overlayLoadingIcon = findViewById(R.id.overlay_loading_icon);

        btnProcessAnalysis.setEnabled(false);
    }

    private void setupListeners() {
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        if (uploadArea != null) {
            uploadArea.setOnClickListener(v -> launchImagePicker());
        }

        if (btnProcessAnalysis != null) {
            btnProcessAnalysis.setOnClickListener(v -> analyzeImage());
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void launchImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ : Utiliser READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_IMAGES});
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12 : Utiliser READ_EXTERNAL_STORAGE si nécessaire
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
                return;
            }
        }
        // Lancer le sélecteur d'image si les permissions sont accordées
        imagePickerLauncher.launch("image/*");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                imagePickerLauncher.launch("image/*");
            } else {
                Toast.makeText(this, "Permission de stockage refusée. Impossible de sélectionner une image.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadLocalModel() {
        try {
            String[] assetFiles = getAssets().list("");
            Log.d(TAG, "Fichiers dans assets : " + java.util.Arrays.toString(assetFiles));
            boolean modelFound = false;
            if (assetFiles != null) {
                for (String file : assetFiles) {
                    if (file.equals("model.tflite")) {
                        modelFound = true;
                        break;
                    }
                }
            }
            if (!modelFound) {
                throw new IOException("Fichier model.tflite non trouvé dans les assets");
            }

            try (FileInputStream inputStream = new FileInputStream(getAssets().openFd("model.tflite").getFileDescriptor())) {
                FileChannel fileChannel = inputStream.getChannel();
                long startOffset = getAssets().openFd("model.tflite").getStartOffset();
                long declaredLength = getAssets().openFd("model.tflite").getDeclaredLength();
                MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
                tfliteInterpreter = new Interpreter(buffer);
                Log.d(TAG, "Modèle local chargé avec succès");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du chargement du modèle local : " + e.getMessage(), e);
            Toast.makeText(this, "Erreur lors du chargement du modèle local : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void analyzeImage() {
        if (selectedBitmap == null || tfliteInterpreter == null) {
            Toast.makeText(this, "Veuillez sélectionner une image et attendre le chargement du modèle", Toast.LENGTH_SHORT).show();
            return;
        }

        btnProcessAnalysis.setEnabled(false);
        btnProcessAnalysis.setText("En train d'analyser...");
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);
        uploadArea.setEnabled(false);
        loadingOverlay.setVisibility(View.VISIBLE);
        overlayLoadingIcon.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate));

        new Thread(() -> {
            try {
                simulateProgressAnimation();

                Bitmap resizedBitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(resizedBitmap);
                canvas.drawColor(Color.BLACK);
                float scale = Math.min(640f / selectedBitmap.getWidth(), 640f / selectedBitmap.getHeight());
                int newWidth = (int) (selectedBitmap.getWidth() * scale);
                int newHeight = (int) (selectedBitmap.getHeight() * scale);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(selectedBitmap, newWidth, newHeight, true);
                int left = (640 - newWidth) / 2;
                int top = (640 - newHeight) / 2;
                canvas.drawBitmap(scaledBitmap, left, top, null);

                ByteBuffer inputBuffer = prepareImageBuffer(resizedBitmap);

                float[][][] output = new float[1][6][8400];
                tfliteInterpreter.run(inputBuffer, output);
                Log.d(TAG, "Inférence réussie : output shape = [1, 6, 8400]");

                List<Detection> detections = processModelOutput(output);
                List<Detection> finalDetections = applyNMS(detections, 0.3f);

                String resultLabel = determineResultLabel(finalDetections);
                float maxConfidence = determineMaxConfidence(finalDetections);

                String base64Image = compressAndConvertBitmapToBase64(selectedBitmap);
                String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

                Map<String, Object> analysisData = prepareAnalysisData(resultLabel, maxConfidence, base64Image, timestamp, finalDetections);

                saveToFirestoreAndShowLoading(analysisData, resultLabel, maxConfidence, base64Image, timestamp, finalDetections);

            } catch (Exception e) {
                Log.e(TAG, "Erreur dans analyzeImage : " + e.getMessage(), e);
                runOnUiThread(() -> {
                    resetUiAfterAnalysis();
                    Toast.makeText(this, "Erreur lors de l'analyse : " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void simulateProgressAnimation() {
        try {
            for (int i = 0; i <= 100; i += 5) {
                int progress = i;
                handler.post(() -> {
                    progressBar.setProgress(progress);
                    progressText.setText(progress + "%");
                });
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Erreur dans la progression : " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private ByteBuffer prepareImageBuffer(Bitmap bitmap) {
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(640 * 640 * 3 * 4);
        inputBuffer.order(ByteOrder.nativeOrder());
        int[] pixels = new int[640 * 640];
        bitmap.getPixels(pixels, 0, 640, 0, 0, 640, 640);

        for (int pixel : pixels) {
            inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
            inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
            inputBuffer.putFloat((pixel & 0xFF) / 255.0f);
        }

        inputBuffer.rewind();
        return inputBuffer;
    }

    private List<Detection> processModelOutput(float[][][] output) {
        List<Detection> detections = new ArrayList<>();
        float confThreshold = 0.5f;

        for (int i = 0; i < 8400; i++) {
            float objectness = output[0][4][i];
            if (objectness < confThreshold) {
                Log.d(TAG, "Détection rejetée : objectness=" + objectness);
                continue;
            }

            float xCenter = output[0][0][i];
            float yCenter = output[0][1][i];
            float width = output[0][2][i];
            float height = output[0][3][i];
            float classProb = output[0][5][i];

            Log.d(TAG, "Coordonnées brutes : xCenter=" + xCenter + ", yCenter=" + yCenter + ", width=" + width + ", height=" + height);

            float xMin = (xCenter - width / 2) * 640;
            float yMin = (yCenter - height / 2) * 640;
            float xMax = (xCenter + width / 2) * 640;
            float yMax = (yCenter + height / 2) * 640;

            xMin = Math.max(0, Math.min(xMin / 640, 1.0f));
            yMin = Math.max(0, Math.min(yMin / 640, 1.0f));
            xMax = Math.max(0, Math.min(xMax / 640, 1.0f));
            yMax = Math.max(0, Math.min(yMax / 640, 1.0f));

            Detection detection = new Detection(xMin, yMin, xMax, yMax, objectness, classProb);
            detections.add(detection);
            Log.d(TAG, "Détection ajoutée : xMin=" + xMin + ", yMin=" + yMin + ", xMax=" + xMax + ", yMax=" + yMax + ", confiance=" + objectness);
        }

        Log.d(TAG, "Nombre total de détections avant NMS : " + detections.size());
        return detections;
    }

    private String determineResultLabel(List<Detection> finalDetections) {
        return !finalDetections.isEmpty() ? "Fracture" : "Aucune fracture";
    }

    private float determineMaxConfidence(List<Detection> finalDetections) {
        if (!finalDetections.isEmpty()) {
            float maxConfidence = 0.0f;
            for (Detection detection : finalDetections) {
                maxConfidence = Math.max(maxConfidence, detection.confidence);
            }
            return maxConfidence;
        }
        return 0.0f;
    }

    private String compressAndConvertBitmapToBase64(Bitmap bitmap) {
        // Réduire la taille initiale pour éviter les erreurs de mémoire
        int maxDimension = 640;
        float scale = Math.min((float) maxDimension / bitmap.getWidth(), (float) maxDimension / bitmap.getHeight());
        int newWidth = Math.round(bitmap.getWidth() * scale);
        int newHeight = Math.round(bitmap.getHeight() * scale);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);

        String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        int base64Size = base64Image.getBytes().length;
        Log.d(TAG, "Taille de l'image Base64 : " + base64Size + " bytes");

        int quality = 70;
        while (base64Size > MAX_FIRESTORE_SIZE && quality > 10) {
            quality -= 10;
            baos.reset();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            base64Size = base64Image.getBytes().length;
            Log.d(TAG, "Nouvelle tentative avec qualité " + quality + " - Taille : " + base64Size + " bytes");
        }

        if (base64Size > MAX_FIRESTORE_SIZE) {
            Log.e(TAG, "Taille de l'image Base64 dépasse toujours la limite de Firestore après compression : " + base64Size + " bytes");
            runOnUiThread(() -> Toast.makeText(this, "L'image est trop grande pour être stockée. Veuillez utiliser une image plus petite.", Toast.LENGTH_LONG).show());
            return "";
        }

        // Libérer la mémoire
        resizedBitmap.recycle();
        return base64Image;
    }

    private Map<String, Object> prepareAnalysisData(String resultLabel, float maxConfidence, String base64Image, String timestamp, List<Detection> finalDetections) {
        Map<String, Object> analysisData = new HashMap<>();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            analysisData.put("userId", currentUser.getUid());
        } else {
            Log.e(TAG, "Utilisateur non connecté");
            runOnUiThread(() -> Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show());
            return null;
        }

        if (base64Image.isEmpty()) {
            Log.e(TAG, "Base64 image est vide, stockage annulé");
            return null;
        }

        analysisData.put("imageBase64", base64Image);
        analysisData.put("resultLabel", resultLabel);
        analysisData.put("confidence", maxConfidence);
        analysisData.put("timestamp", timestamp);

        if (!finalDetections.isEmpty()) {
            List<Map<String, Float>> fractureCoords = new ArrayList<>();
            for (Detection detection : finalDetections) {
                Map<String, Float> coords = new HashMap<>();
                coords.put("xMin", detection.xMin);
                coords.put("yMin", detection.yMin);
                coords.put("xMax", detection.xMax);
                coords.put("yMax", detection.yMax);
                coords.put("confidence", detection.confidence);
                fractureCoords.add(coords);
            }
            analysisData.put("fractureCoordinates", fractureCoords);
            Log.d(TAG, "Coordonnées des fractures ajoutées : " + fractureCoords);
        } else {
            analysisData.put("fractureCoordinates", new ArrayList<>());
        }

        return analysisData;
    }

    private void saveToFirestoreAndShowLoading(Map<String, Object> analysisData, String resultLabel, float maxConfidence, String base64Image, String timestamp, List<Detection> finalDetections) {
        if (analysisData == null) {
            runOnUiThread(() -> {
                resetUiAfterAnalysis();
                Toast.makeText(this, "Erreur : Impossible de stocker les données", Toast.LENGTH_LONG).show();
            });
            return;
        }

        db.collection("analysis_results")
                .add(analysisData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Données stockées avec succès dans Firestore avec ID: " + documentReference.getId());
                    runOnUiThread(() -> {
                        loadingOverlay.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        progressText.setVisibility(View.GONE);
                        loadingContainer.setVisibility(View.VISIBLE);
                        loadingAnimation.setImageResource(R.drawable.ic_success);
                        loadingText.setText("Analyse terminée avec succès !");
                        Animation successAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
                        loadingAnimation.startAnimation(successAnimation);

                        handler.postDelayed(() -> {
                            loadingContainer.setVisibility(View.GONE);
                            resetUiAfterAnalysis();

                            Intent intent = new Intent(uploadImagesActivity.this, AnalysisResultActivity.class);
                            intent.putExtra("resultLabel", resultLabel);
                            intent.putExtra("confidence", maxConfidence);
                            intent.putExtra("imageBase64", base64Image);
                            intent.putExtra("timestamp", timestamp);

                            if (!finalDetections.isEmpty()) {
                                intent.putExtra("fractureCoordinates", (ArrayList<Map<String, Float>>) analysisData.get("fractureCoordinates"));
                            }

                            Log.d(TAG, "Lancement de AnalysisResultActivity avec resultLabel: " + resultLabel
                                    + ", confidence: " + maxConfidence
                                    + ", timestamp: " + timestamp
                                    + ", imageBase64 length: " + (base64Image != null ? base64Image.length() : 0));

                            try {
                                startActivity(intent);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception lors du lancement de AnalysisResultActivity: " + e.getMessage(), e);
                                Toast.makeText(uploadImagesActivity.this,
                                        "Erreur lors de l'affichage des résultats: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }, 2000);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors du stockage dans Firestore : " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        resetUiAfterAnalysis();
                        Toast.makeText(this, "Erreur lors du stockage des résultats : " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                });
    }

    private void resetUiAfterAnalysis() {
        progressBar.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);
        loadingContainer.setVisibility(View.GONE);
        loadingOverlay.setVisibility(View.GONE);
        btnProcessAnalysis.setEnabled(true);
        btnProcessAnalysis.setText("Commencer l’analyse");
        uploadArea.setEnabled(true);
    }

    private static class Detection {
        float xMin, yMin, xMax, yMax;
        float confidence;
        float classProb;

        Detection(float xMin, float yMin, float xMax, float yMax, float confidence, float classProb) {
            this.xMin = xMin;
            this.yMin = yMin;
            this.xMax = xMax;
            this.yMax = yMax;
            this.confidence = confidence;
            this.classProb = classProb;
        }
    }

    private List<Detection> applyNMS(List<Detection> detections, float iouThreshold) {
        List<Detection> sortedDetections = new ArrayList<>(detections);
        Collections.sort(sortedDetections, (d1, d2) -> Float.compare(d2.confidence, d1.confidence));

        List<Detection> finalDetections = new ArrayList<>();
        while (!sortedDetections.isEmpty()) {
            Detection best = sortedDetections.remove(0);
            finalDetections.add(best);

            Iterator<Detection> iterator = sortedDetections.iterator();
            while (iterator.hasNext()) {
                Detection other = iterator.next();
                float iou = calculateIoU(best, other);
                if (iou > iouThreshold) {
                    Log.d(TAG, "Détection supprimée par NMS : IoU=" + iou);
                    iterator.remove();
                }
            }
        }
        Log.d(TAG, "Nombre total de détections après NMS : " + finalDetections.size());
        return finalDetections;
    }

    private float calculateIoU(Detection box1, Detection box2) {
        float x1 = Math.max(box1.xMin, box2.xMin);
        float y1 = Math.max(box1.yMin, box2.yMin);
        float x2 = Math.min(box1.xMax, box2.xMax);
        float y2 = Math.min(box1.yMax, box2.yMax);

        float intersection = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        float area1 = (box1.xMax - box1.xMin) * (box1.yMax - box1.yMin);
        float area2 = (box2.xMax - box2.xMin) * (box2.yMax - box2.yMin);
        float union = area1 + area2 - intersection;

        return intersection / union;
    }

    private void updateNavigationMenu() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (navigationView == null) {
            Log.e(TAG, "NavigationView est null");
            return;
        }
        Menu menu = navigationView.getMenu();

        MenuItem loginItem = menu.findItem(R.id.nav_login);
        MenuItem logoutItem = menu.findItem(R.id.nav_logout);
        MenuItem historyItem = menu.findItem(R.id.nav_history);
        MenuItem profileItem = menu.findItem(R.id.nav_profile);
        MenuItem scanItem = menu.findItem(R.id.nav_scan);
        MenuItem helpItem = menu.findItem(R.id.nav_help);
        MenuItem aboutItem = menu.findItem(R.id.nav_about);

        if (currentUser != null) {
            if (historyItem != null) historyItem.setVisible(true);
            if (profileItem != null) profileItem.setVisible(true);
            if (scanItem != null) scanItem.setVisible(true);
            if (helpItem != null) helpItem.setVisible(true);
            if (aboutItem != null) aboutItem.setVisible(true);
            if (loginItem != null) loginItem.setVisible(false);
            if (logoutItem != null) logoutItem.setVisible(true);
            if (scanItem != null) navigationView.setCheckedItem(R.id.nav_scan);
        } else {
            if (historyItem != null) historyItem.setVisible(false);
            if (profileItem != null) profileItem.setVisible(false);
            if (scanItem != null) scanItem.setVisible(false);
            if (helpItem != null) helpItem.setVisible(true);
            if (aboutItem != null) aboutItem.setVisible(true);
            if (loginItem != null) loginItem.setVisible(true);
            if (logoutItem != null) logoutItem.setVisible(false);
            if (menu.findItem(R.id.nav_home) != null) navigationView.setCheckedItem(R.id.nav_home);
        }

        navigationView.invalidate();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        try {
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (id == R.id.nav_scan) {
                Toast.makeText(this, "Vous êtes déjà sur Scanner", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                finish();
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, Profil.class));
            } else if (id == R.id.nav_help) {
                startActivity(new Intent(this, ChatBoot.class));
            } else if (id == R.id.nav_about) {
                startActivity(new Intent(this, AboutUs.class));
            } else if (id == R.id.nav_login) {
                startActivity(new Intent(this, SignInEmail.class));
            } else if (id == R.id.nav_logout) {
                drawerLayout.closeDrawer(GravityCompat.START);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    mAuth.signOut();
                    Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
                    updateNavigationMenu();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }, 500);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur dans onNavigationItemSelected : " + e.getMessage());
            Toast.makeText(this, "Erreur lors de la navigation", Toast.LENGTH_SHORT).show();
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
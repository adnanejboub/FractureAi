package com.fractureai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Map;

public class AnalysisResultActivity extends AppCompatActivity {

    private static final String TAG = "AnalysisResult";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_analysis_result);

            // Initialiser les vues
            ImageView resultImage = findViewById(R.id.result_image);
            TextView resultClass = findViewById(R.id.result_class);
            TextView resultConfidence = findViewById(R.id.result_confidence);
            TextView resultTimestamp = findViewById(R.id.result_timestamp);
            TextView fractureCoordinatesText = findViewById(R.id.fracture_coordinates);
            Button btnBack = findViewById(R.id.btn_back);

            // Vérifier que les vues ne sont pas null
            if (resultImage == null || resultClass == null || resultConfidence == null ||
                    resultTimestamp == null || fractureCoordinatesText == null || btnBack == null) {
                Log.e(TAG, "Une ou plusieurs vues sont null");
                Toast.makeText(this, "Erreur d'affichage, vérifiez le layout", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Récupérer les données de l'intent
            Intent intent = getIntent();
            String base64Image = intent.getStringExtra("imageBase64");
            String label = intent.getStringExtra("resultLabel");
            float confidence = intent.getFloatExtra("confidence", 0.0f);
            String timestamp = intent.getStringExtra("timestamp");
            ArrayList<Map<String, Object>> fractureCoordinates = (ArrayList<Map<String, Object>>) intent.getSerializableExtra("fractureCoordinates");

            Log.d(TAG, "Données reçues - imageBase64 length: " + (base64Image != null ? base64Image.length() : 0) +
                    ", label: " + label + ", confidence: " + confidence + ", timestamp: " + timestamp +
                    ", fractureCoordinates: " + (fractureCoordinates != null ? fractureCoordinates.size() : "null"));

            // Afficher l'image avec les fractures détectées
            if (base64Image != null && !base64Image.isEmpty()) {
                try {
                    // Décoder l'image Base64 avec gestion de la mémoire
                    byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = calculateInSampleSize(decodedBytes.length);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, options);

                    if (bitmap != null) {
                        // Créer une copie modifiable de l'image
                        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        bitmap.recycle(); // Libérer la Bitmap originale
                        Canvas canvas = new Canvas(mutableBitmap);
                        Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(5f);

                        // Dessiner les rectangles pour chaque fracture détectée
                        if (fractureCoordinates != null && !fractureCoordinates.isEmpty()) {
                            int imageWidth = mutableBitmap.getWidth();
                            int imageHeight = mutableBitmap.getHeight();

                            for (Map<String, Object> coords : fractureCoordinates) {
                                // Convertir les valeurs Double en Float
                                Float xMin = convertToFloat(coords.get("xMin"));
                                Float yMin = convertToFloat(coords.get("yMin"));
                                Float xMax = convertToFloat(coords.get("xMax"));
                                Float yMax = convertToFloat(coords.get("yMax"));
                                Float coordConfidence = convertToFloat(coords.get("confidence"));

                                if (xMin == null || yMin == null || xMax == null || yMax == null || coordConfidence == null) {
                                    Log.w(TAG, "Coordonnées invalides dans fractureCoordinates");
                                    continue;
                                }

                                float pixelXMin = xMin * imageWidth;
                                float pixelYMin = yMin * imageHeight;
                                float pixelXMax = xMax * imageWidth;
                                float pixelYMax = yMax * imageHeight;

                                canvas.drawRect(pixelXMin, pixelYMin, pixelXMax, pixelYMax, paint);
                            }
                            fractureCoordinatesText.setText("Fractures détectées.");
                        } else {
                            fractureCoordinatesText.setText("Aucune fracture détectée.");
                        }

                        resultImage.setImageBitmap(mutableBitmap);
                    } else {
                        throw new IllegalStateException("Échec du décodage de l'image Bitmap");
                    }
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "Erreur de mémoire lors du décodage de l'image : " + e.getMessage());
                    resultImage.setImageResource(android.R.drawable.ic_menu_report_image);
                    fractureCoordinatesText.setText("Erreur : Image trop grande pour être affichée.");
                    Toast.makeText(this, "Image trop grande, essayez une image plus petite", Toast.LENGTH_LONG).show();
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Base64 invalide : " + e.getMessage());
                    resultImage.setImageResource(android.R.drawable.ic_menu_report_image);
                    fractureCoordinatesText.setText("Erreur : Image corrompue ou format invalide.");
                    Toast.makeText(this, "Erreur de téléchargement : Image corrompue", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors du décodage ou du dessin de l'image : " + e.getMessage(), e);
                    resultImage.setImageResource(android.R.drawable.ic_menu_report_image);
                    fractureCoordinatesText.setText("Erreur lors du chargement de l'image.");
                    Toast.makeText(this, "Erreur de téléchargement de l'image : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "base64Image est null ou vide");
                resultImage.setImageResource(android.R.drawable.ic_menu_report_image);
                fractureCoordinatesText.setText("Aucune image disponible.");
                Toast.makeText(this, "Aucune image disponible", Toast.LENGTH_SHORT).show();
            }

            // Afficher les résultats
            resultClass.setText(label != null ? label : "Inconnu");
            resultConfidence.setText(String.format("%.2f%%", confidence * 100));
            resultTimestamp.setText(timestamp != null ? timestamp : "Inconnu");

            // Configurer le bouton de retour
            btnBack.setOnClickListener(v -> finish());

            // Gestion des insets
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur critique dans onCreate : " + e.getMessage(), e);
            Toast.makeText(this, "Erreur critique : " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private Float convertToFloat(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return ((Double) value).floatValue();
        } else if (value instanceof Float) {
            return (Float) value;
        } else if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else {
            Log.w(TAG, "Type inattendu pour la valeur : " + value.getClass().getName());
            return null;
        }
    }

    private int calculateInSampleSize(int dataLength) {
        int inSampleSize = 1;
        if (dataLength > 1_000_000) { // Réduire pour les images > 1 Mo
            inSampleSize = 2;
        } else if (dataLength > 4_000_000) { // Réduire encore pour les images > 4 Mo
            inSampleSize = 4;
        }
        Log.d(TAG, "inSampleSize calculé : " + inSampleSize);
        return inSampleSize;
    }
}
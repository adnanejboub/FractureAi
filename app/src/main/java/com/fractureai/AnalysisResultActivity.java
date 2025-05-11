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
            TextView fractureCoordinatesText = findViewById(R.id.fracture_coordinates); // Nouveau TextView pour les coordonnées
            Button btnBack = findViewById(R.id.btn_back);

            // Récupérer les données de l'intent
            Intent intent = getIntent();
            String base64Image = intent.getStringExtra("imageBase64");
            String label = intent.getStringExtra("resultLabel");
            float confidence = intent.getFloatExtra("confidence", 0.0f);
            String timestamp = intent.getStringExtra("timestamp");
            ArrayList<Map<String, Float>> fractureCoordinates = (ArrayList<Map<String, Float>>) intent.getSerializableExtra("fractureCoordinates");

            Log.d("AnalysisResult", "Données reçues - imageBase64: " + (base64Image != null) + ", label: " + label + ", confidence: " + confidence + ", timestamp: " + timestamp + ", fractureCoordinates: " + (fractureCoordinates != null ? fractureCoordinates.toString() : "null"));

            // Afficher l'image avec les fractures détectées
            if (base64Image != null) {
                try {
                    byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                    // Créer une copie modifiable de l'image
                    Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(mutableBitmap);
                    Paint paint = new Paint();
                    paint.setColor(Color.RED); // Couleur des rectangles
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5f); // Épaisseur du contour

                    // Dessiner les rectangles pour chaque fracture détectée
                    if (fractureCoordinates != null && !fractureCoordinates.isEmpty()) {
                        int imageWidth = mutableBitmap.getWidth();
                        int imageHeight = mutableBitmap.getHeight();

                        StringBuilder coordsText = new StringBuilder("Coordonnées des fractures :\n");
                        for (Map<String, Float> coords : fractureCoordinates) {
                            // Convertir les coordonnées normalisées en pixels
                            float xMin = coords.get("xMin") * imageWidth;
                            float yMin = coords.get("yMin") * imageHeight;
                            float xMax = coords.get("xMax") * imageWidth;
                            float yMax = coords.get("yMax") * imageHeight;

                            // Dessiner un rectangle sur l'image
                            canvas.drawRect(xMin, yMin, xMax, yMax, paint);

                            // Ajouter les coordonnées au texte (facultatif)
                            coordsText.append(String.format("xMin: %.2f, yMin: %.2f, xMax: %.2f, yMax: %.2f, Confiance: %.2f%%\n",
                                    xMin, yMin, xMax, yMax, coords.get("confidence") * 100));
                        }
                        fractureCoordinatesText.setText(coordsText.toString());
                    } else {
                        fractureCoordinatesText.setText("Aucune fracture détectée.");
                    }

                    // Afficher l'image avec les rectangles
                    resultImage.setImageBitmap(mutableBitmap);
                } catch (Exception e) {
                    Log.e("AnalysisResult", "Erreur lors du décodage ou du dessin de l'image : " + e.getMessage());
                    resultImage.setImageResource(android.R.drawable.ic_menu_report_image);
                    Toast.makeText(this, "Erreur lors du chargement de l'image", Toast.LENGTH_SHORT).show();
                    fractureCoordinatesText.setText("Erreur lors du chargement de l'image.");
                }
            } else {
                resultImage.setImageResource(android.R.drawable.ic_menu_report_image);
                Log.w("AnalysisResult", "base64Image est null");
                fractureCoordinatesText.setText("Aucune image disponible.");
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

            // Vérification finale
            if (resultImage == null || resultClass == null || resultConfidence == null || resultTimestamp == null || fractureCoordinatesText == null || btnBack == null) {
                Log.e("AnalysisResult", "Une ou plusieurs vues sont null");
                Toast.makeText(this, "Erreur d'affichage, vérifiez le layout", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("AnalysisResult", "Erreur critique dans onCreate : " + e.getMessage(), e);
            Toast.makeText(this, "Erreur critique : " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
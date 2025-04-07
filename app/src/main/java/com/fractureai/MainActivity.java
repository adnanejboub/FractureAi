package com.fractureai;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Gestion des insets pour les barres système
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bouton "Analyse instantanée"
        Button btnAnalyse = findViewById(R.id.btn_analyse);
        btnAnalyse.setOnClickListener(v -> {
            Toast.makeText(this, "Analyse instantanée lancée", Toast.LENGTH_SHORT).show();
            // Ajoutez ici la logique pour lancer l'analyse
        });

        // Bouton "Assistant IA"
        Button btnAssistant = findViewById(R.id.btn_assistant);
        btnAssistant.setOnClickListener(v -> {
            Toast.makeText(this, "Assistant IA lancé", Toast.LENGTH_SHORT).show();
            // Ajoutez ici la logique pour lancer l'assistant
        });
    }
}
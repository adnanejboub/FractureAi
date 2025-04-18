package com.fractureai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private ImageButton userButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        userButton = findViewById(R.id.btn_user);

        if (userButton == null) {
            Log.e("MainActivity", "Bouton utilisateur n'exsite pas");
            return;
        }
        userButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(MainActivity.this, SignInEmail.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("MainActivity", "Erreur lors du démarrage de l'activité SignInEmail : " + e.getMessage());
                }
            }
        });
    }
}
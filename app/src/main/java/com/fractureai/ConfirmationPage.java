package com.fractureai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ConfirmationPage extends AppCompatActivity {
    private Button btn_close;
    private ImageView logo_app;
    private TextView email_address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirmation_page);


        logo_app = findViewById(R.id.logo);
        btn_close = findViewById(R.id.btn_close);
        email_address = findViewById(R.id.email_address);


        if (logo_app == null) {
            Log.e("ConfirmationPage", "Logo de l'app n'existe pas");
            return;
        }

        if (btn_close == null) {
            Log.e("ConfirmationPage", "Bouton Fermer n'existe pas");
            return;
        }

        if (email_address == null) {
            Log.e("ConfirmationPage", "TextView email_address n'existe pas");
            return;
        }


        String email = getIntent().getStringExtra("email");
        if (email != null && !email.isEmpty()) {
            email_address.setText(email);
        } else {
            email_address.setText("inconnu@example.com");
            Log.w("ConfirmationPage", "Aucun email fourni dans l'Intent");
        }


        logo_app.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(ConfirmationPage.this, MainActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e("ConfirmationPage", "Erreur lors du démarrage de l'activité MainActivity : " + e.getMessage());
            }
        });


        btn_close.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(ConfirmationPage.this, SignInEmail.class);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Log.e("ConfirmationPage", "Erreur lors du démarrage de l'activité SignInEmail : " + e.getMessage());
            }
        });
    }
}
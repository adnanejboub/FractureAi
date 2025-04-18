package com.fractureai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ConfirmationPage extends AppCompatActivity {
   private Button btn_close ;
   private ImageView logo_app ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirmation_page);
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        logo_app = findViewById(R.id.logo) ;
        btn_close = findViewById(R.id.btn_close) ;

        if (logo_app == null) {
            Log.e("ConfirmationPage", "Logo de l'app n'existe pas ");
            return;
        }

        if (btn_close == null) {
            Log.e("ConfirmationPage", "Buton Fermer n'existe pas ");
            return;
        }

        logo_app.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(ConfirmationPage.this , MainActivity.class) ;
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("ConfirmationPage", "Erreur lors du démarrage de l'activité MainActivity : " + e.getMessage());
                }

            }
        });

        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(ConfirmationPage.this , SignInEmail.class) ;
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("ConfirmationPage", "Erreur lors du démarrage de l'activité SignInEmail : " + e.getMessage());
                }

            }
        });







    }
}

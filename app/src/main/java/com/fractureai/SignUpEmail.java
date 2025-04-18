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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SignUpEmail extends AppCompatActivity {

    private Button btnPhone;
    private TextView loginText;

    private ImageView logo_app ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up_email);

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        } else {
            Log.e("SignUpEmail", "Root view not found");
        }

        btnPhone = findViewById(R.id.btn_phone);
        loginText = findViewById(R.id.login_text);
        logo_app = findViewById(R.id.logo) ;

        if (btnPhone == null) {
            Log.e("SignUpEmail", "Bouton Téléphone n'existe pas");
            return;
        }
        if (loginText == null) {
            Log.e("SignUpEmail", "Texte de connexion n'existe pas");
            return;
        }

        if (logo_app == null) {
            Log.e("SignUpEmail", "Logo de l'app n'existe pas ");
            return;
        }

        logo_app.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(SignUpEmail.this , MainActivity.class) ;
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("SignUpEmail", "Erreur lors du démarrage de l'activité MainActivity : " + e.getMessage());
                }

            }
        });

        btnPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(SignUpEmail.this, SignUpTelephone.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("SignUpEmail", "Erreur lors du démarrage de l'activité SignUpTelephone : " + e.getMessage());
                }
            }
        });

        loginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(SignUpEmail.this, SignInEmail.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("SignUpEmail", "Erreur lors du démarrage de l'activité SignIn : " + e.getMessage());
                }
            }
        });
    }
}
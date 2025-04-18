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

public class SignUpTelephone extends AppCompatActivity {

    private Button btnEmail;
    private ImageView logo_app ;

    private TextView loginText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up_telephone);

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        } else {
            Log.e("SignUpTelephone", "Root view not found");
        }

        btnEmail = findViewById(R.id.btn_email);
        loginText = findViewById(R.id.login_text);
        logo_app = findViewById(R.id.logo) ;

        if (btnEmail == null) {
            Log.e("SignUpTelephone", "Bouton Email n'existe pas ");
            return;
        }
        if (loginText == null) {
            Log.e("SignUpTelephone", "Texte de connexion n'existe pas ");
            return;
        }

        if (logo_app == null) {
            Log.e("SignUpTelephone", "Logo de l'app n'existe pas ");
            return;
        }

        logo_app.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(SignUpTelephone.this , MainActivity.class) ;
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("SignUpTelephone", "Erreur lors du démarrage de l'activité MainActivity : " + e.getMessage());
                }

            }
        });

        btnEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(SignUpTelephone.this, SignUpEmail.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("SignUpTelephone", "Erreur lors du démarrage de l'activité SignUpEmail : " + e.getMessage());
                }
            }
        });

        loginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(SignUpTelephone.this, SignIn.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("SignUpTelephone", "Erreur lors du démarrage de l'activité SignIn : " + e.getMessage());
                }
            }
        });
    }
}
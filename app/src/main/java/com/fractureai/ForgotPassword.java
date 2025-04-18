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

public class ForgotPassword extends AppCompatActivity {
    private TextView back_to_login ;
    private ImageView logo_app ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        back_to_login = findViewById(R.id.back_to_login) ;
        logo_app = findViewById(R.id.logo) ;

        if(back_to_login == null) {
            Log.e("ForgotPassword", "le text retour à la page de connexion n'existe pas");
            return;
        }

        if (logo_app == null) {
            Log.e("ForgotPassword", "Logo de l'app n'existe pas ");
            return;
        }

        logo_app.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(ForgotPassword.this , MainActivity.class) ;
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("ForgotPassword", "Erreur lors du démarrage de l'activité MainActivity : " + e.getMessage());
                }

            }
        });

       back_to_login.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               try{
                   Intent intent = new Intent(ForgotPassword.this , SignInEmail.class);
                   startActivity(intent);
               } catch (Exception e) {
                   Log.e("ForgotPassword", "Erreur pour la redirection à l'activité de sign_in_email:" + e.getMessage());
               }

           }
       });

    }
}
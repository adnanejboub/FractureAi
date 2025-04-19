package com.fractureai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SignUpEmail extends AppCompatActivity {

    private Button btnPhone, btnRegister;
    private TextView loginText;
    private ImageView logo_app;
    private EditText inputName, inputEmail, inputPassword, inputConfirmPassword;
    private LinearLayout btnGoogle, btnFacebook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up_email);

        // Appliquer les marges liées aux barres système
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Initialisation des composants
        logo_app = findViewById(R.id.logo);
        btnPhone = findViewById(R.id.btn_phone);
        loginText = findViewById(R.id.login_text);
        btnRegister = findViewById(R.id.btn_register);
        inputName = findViewById(R.id.input_name);
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        inputConfirmPassword = findViewById(R.id.input_confirm_password);
        btnGoogle = findViewById(R.id.btn_google);
        btnFacebook = findViewById(R.id.btn_facebook);

        // Actions sur clic
        logo_app.setOnClickListener(v -> {
            try {
                startActivity(new Intent(SignUpEmail.this, MainActivity.class));
            } catch (Exception e) {
                Log.e("SignUpEmail", "Erreur logo_app : " + e.getMessage());
            }
        });

        btnPhone.setOnClickListener(v -> {
            try {
                startActivity(new Intent(SignUpEmail.this, SignUpTelephone.class));
            } catch (Exception e) {
                Log.e("SignUpEmail", "Erreur btn_phone : " + e.getMessage());
            }
        });

        loginText.setOnClickListener(v -> {
            try {
                startActivity(new Intent(SignUpEmail.this, SignInEmail.class));
            } catch (Exception e) {
                Log.e("SignUpEmail", "Erreur loginText : " + e.getMessage());
            }
        });

        btnRegister.setOnClickListener(v -> {
            // Exemple simple de validation (à adapter selon tes besoins)
            String name = inputName.getText().toString().trim();
            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString();
            String confirmPassword = inputConfirmPassword.getText().toString();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Log.e("Inscription", "Tous les champs sont requis !");
                return;
            }

            if (!password.equals(confirmPassword)) {
                Log.e("Inscription", "Les mots de passe ne correspondent pas !");
                return;
            }

            // Simuler l'inscription réussie (tu peux ensuite appeler une API)
            Log.i("Inscription", "Utilisateur inscrit : " + name + ", " + email);
        });

        btnGoogle.setOnClickListener(v -> Log.i("SignUp", "Connexion avec Google"));
        btnFacebook.setOnClickListener(v -> Log.i("SignUp", "Connexion avec Facebook"));
    }
}

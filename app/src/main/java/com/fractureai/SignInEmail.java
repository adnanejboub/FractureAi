package com.fractureai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class SignInEmail extends AppCompatActivity {

    private Button btnPhone;
    private TextView forgotPassword;
    private Button seConnect;
    private TextView signupText;
    private ImageView logo_app;
    private EditText etEmail, etPassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Activer EdgeToEdge avant le layout
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in_email);

        // Initialiser Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialiser les vues après setContentView
        etEmail = findViewById(R.id.input_email);
        etPassword = findViewById(R.id.input_password);
        seConnect = findViewById(R.id.seConnecter);
        btnPhone = findViewById(R.id.btn_phone);
        forgotPassword = findViewById(R.id.forgot_password);
        signupText = findViewById(R.id.signup_text);
        logo_app = findViewById(R.id.logo);

        // Gestion des insets
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Vérification existence des vues (log uniquement)
        if (logo_app == null) Log.e("SignInEmail", "Logo de l'app n'existe pas");
        if (btnPhone == null) Log.e("SignInEmail", "Le bouton téléphone n'existe pas");
        if (forgotPassword == null) Log.e("SignInEmail", "Le texte forgotPassword n'existe pas");
        if (signupText == null) Log.e("SignInEmail", "Le texte signupText n'existe pas");

        // Listeners
        logo_app.setOnClickListener(v -> startActivitySafely(MainActivity.class, "MainActivity"));
        seConnect.setOnClickListener(v -> authenticateUser());
        btnPhone.setOnClickListener(v -> startActivitySafely(SignIn.class, "SignIn"));
        forgotPassword.setOnClickListener(v -> startActivitySafely(ForgotPassword.class, "ForgotPassword"));
        signupText.setOnClickListener(v -> startActivitySafely(SignUpEmail.class, "SignUpEmail"));
    }

    private void startActivitySafely(Class<?> activityClass, String tag) {
        try {
            Intent intent = new Intent(SignInEmail.this, activityClass);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("SignInEmail", "Erreur lors du démarrage de l'activité " + tag + " : " + e.getMessage());
        }
    }

    private void authenticateUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs !", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignInEmail.this, MainActivity.class));
                        finish(); // Pour éviter de revenir à l'écran de login
                    } else {
                        Toast.makeText(this, "Erreur : " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

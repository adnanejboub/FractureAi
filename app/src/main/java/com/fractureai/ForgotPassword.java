package com.fractureai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {
    private TextView back_to_login;
    private ImageView logo_app;
    private EditText input_email;
    private Button btn_reset_password;
    private View loading_overlay;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);


        mAuth = FirebaseAuth.getInstance();


        back_to_login = findViewById(R.id.back_to_login);
        logo_app = findViewById(R.id.logo);
        input_email = findViewById(R.id.input_email);
        btn_reset_password = findViewById(R.id.btn_reset_password);
        loading_overlay = findViewById(R.id.loading_overlay);


        if (back_to_login == null) {
            Log.e("ForgotPassword", "Le texte retour à la page de connexion n'existe pas");
            return;
        }

        if (logo_app == null) {
            Log.e("ForgotPassword", "Le logo de l'app n'existe pas");
            return;
        }

        if (input_email == null) {
            Log.e("ForgotPassword", "Le champ email n'existe pas");
            return;
        }

        if (btn_reset_password == null) {
            Log.e("ForgotPassword", "Le bouton de réinitialisation n'existe pas");
            return;
        }

        if (loading_overlay == null) {
            Log.e("ForgotPassword", "La superposition de chargement n'existe pas");
            return;
        }


        logo_app.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(ForgotPassword.this, MainActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e("ForgotPassword", "Erreur lors du démarrage de l'activité MainActivity : " + e.getMessage());
            }
        });


        back_to_login.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(ForgotPassword.this, SignInEmail.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e("ForgotPassword", "Erreur pour la redirection à l'activité de sign_in_email : " + e.getMessage());
            }
        });


        btn_reset_password.setOnClickListener(v -> {
            String email = input_email.getText().toString().trim();


            if (email.isEmpty()) {
                input_email.setError("L'email est requis");
                input_email.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                input_email.setError("Entrez un email valide");
                input_email.requestFocus();
                return;
            }


            btn_reset_password.setEnabled(false);
            btn_reset_password.setText("Envoi en cours...");
            loading_overlay.setVisibility(View.VISIBLE);


            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        // Réactiver le bouton et masquer la superposition
                        btn_reset_password.setEnabled(true);
                        btn_reset_password.setText("Réinitialiser le mot de passe");
                        loading_overlay.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPassword.this,
                                    "Un email de réinitialisation a été envoyé à " + email,
                                    Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(ForgotPassword.this, ConfirmationPage.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(ForgotPassword.this,
                                    "Erreur : " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
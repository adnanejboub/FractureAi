package com.fractureai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

/**
 * Activité pour gérer les paramètres de sécurité de l'utilisateur, y compris le changement de mot de passe.
 */
public class SecurityProfil extends AppCompatActivity {

    private EditText inputCurrentPassword, inputNewPassword, inputConfirmPassword;
    private Button btnUpdatePassword, btnCancel, btnInfoPersonnelles, btnSecurite;
    private ImageView profilePicture;
    private ProgressBar progressBar, uploadProgress;
    private FrameLayout loadingOverlay;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_profil);

        // Initialisation de Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = user.getUid();

        // Initialisation des composants UI
        inputCurrentPassword = findViewById(R.id.input_current_password);
        inputNewPassword = findViewById(R.id.input_new_password);
        inputConfirmPassword = findViewById(R.id.input_confirm_password);
        btnUpdatePassword = findViewById(R.id.btn_update_password);
        btnCancel = findViewById(R.id.btn_cancel);
        btnInfoPersonnelles = findViewById(R.id.btn_info_personnelles);
        btnSecurite = findViewById(R.id.btn_securite);
        profilePicture = findViewById(R.id.profile_picture);
        progressBar = findViewById(R.id.progress_bar);
        uploadProgress = findViewById(R.id.upload_progress);
        loadingOverlay = findViewById(R.id.loading_overlay);

        // Chargement de la photo de profil
        loadProfilePicture();

        // Configuration des listeners pour les boutons
        btnUpdatePassword.setOnClickListener(v -> changePassword());
        btnCancel.setOnClickListener(v -> finish());
        btnInfoPersonnelles.setOnClickListener(v -> {
            Intent intent = new Intent(SecurityProfil.this, Profil.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        btnSecurite.setOnClickListener(v -> {
            // Déjà sur l'onglet sécurité
        });
    }

    /**
     * Charge la photo de profil depuis Firestore et l'affiche avec un recadrage circulaire.
     */
    private void loadProfilePicture() {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String profilePicBase64 = documentSnapshot.getString("profilePictureBase64");
                if (profilePicBase64 != null && !profilePicBase64.isEmpty()) {
                    // Conversion de Base64 en Bitmap
                    byte[] decodedBytes = Base64.decode(profilePicBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                    RequestOptions requestOptions = new RequestOptions()
                            .centerCrop()
                            .transform(new CircleCrop())
                            .error(R.drawable.ic_profil);

                    Glide.with(this)
                            .load(bitmap)
                            .apply(requestOptions)
                            .into(profilePicture);
                } else {
                    Glide.with(this)
                            .load(R.drawable.ic_profil)
                            .transform(new CircleCrop())
                            .into(profilePicture);
                }
            } else {
                Toast.makeText(this, "Aucune donnée utilisateur trouvée", Toast.LENGTH_SHORT).show();
                Glide.with(this)
                        .load(R.drawable.ic_profil)
                        .transform(new CircleCrop())
                        .into(profilePicture);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Échec du chargement de la photo : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Glide.with(this)
                    .load(R.drawable.ic_profil)
                    .transform(new CircleCrop())
                    .into(profilePicture);
        });
    }

    /**
     * Gère le processus de changement de mot de passe avec validation et mise à jour via Firebase.
     */
    private void changePassword() {
        String currentPassword = inputCurrentPassword.getText().toString().trim();
        String newPassword = inputNewPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();

        // Validation des champs
        if (currentPassword.isEmpty()) {
            inputCurrentPassword.setError("Veuillez entrer votre mot de passe actuel");
            return;
        }
        if (newPassword.isEmpty()) {
            inputNewPassword.setError("Veuillez entrer un nouveau mot de passe");
            return;
        }
        if (newPassword.length() < 6) {
            inputNewPassword.setError("Le mot de passe doit contenir au moins 6 caractères");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            inputConfirmPassword.setError("Les mots de passe ne correspondent pas");
            return;
        }

        // Affichage de l'overlay de chargement
        loadingOverlay.setVisibility(View.VISIBLE);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            // Ré-authentification de l'utilisateur
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential).addOnSuccessListener(aVoid -> {
                // Mise à jour du mot de passe
                user.updatePassword(newPassword).addOnSuccessListener(aVoid1 -> {
                    // Masquage de l'overlay de chargement
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(SecurityProfil.this, "Mot de passe mis à jour avec succès", Toast.LENGTH_SHORT).show();
                    finish();
                }).addOnFailureListener(e -> {
                    // Masquage de l'overlay de chargement en cas d'erreur
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(SecurityProfil.this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                // Masquage de l'overlay de chargement en cas d'erreur de ré-authentification
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(SecurityProfil.this, "Mot de passe actuel incorrect ou erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            // Masquage de l'overlay de chargement si l'utilisateur n'est pas connecté
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
        }
    }
}
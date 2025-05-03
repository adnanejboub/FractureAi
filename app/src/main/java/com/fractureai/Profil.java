package com.fractureai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Path;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class Profil extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int STORAGE_PERMISSION_CODE = 100;

    private ImageView profilePicture, btnUpload, btnDeletePhoto;
    private EditText inputName, inputEmail, inputPhone, inputAddress, inputCity;
    private Button btnSave, btnCancel, btnInfoPersonnelles, btnSecurite;
    private ProgressBar uploadProgress;
    private CountryCodePicker countryCodePicker;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Uri imageUri;
    private Bitmap selectedBitmap;
    private String userId;
    private boolean hasProfilePicture = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = user.getUid();

        // Initialize UI components
        profilePicture = findViewById(R.id.profile_picture);
        btnUpload = findViewById(R.id.btn_upload);
        btnDeletePhoto = findViewById(R.id.btn_delete_photo);
        uploadProgress = findViewById(R.id.upload_progress);
        inputName = findViewById(R.id.input_name);
        inputEmail = findViewById(R.id.input_email);
        inputPhone = findViewById(R.id.input_phone);
        inputAddress = findViewById(R.id.input_address);
        inputCity = findViewById(R.id.input_city);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        btnInfoPersonnelles = findViewById(R.id.btn_info_personnelles);
        btnSecurite = findViewById(R.id.btn_securite);
        countryCodePicker = findViewById(R.id.country_code_picker);

        // Link CountryCodePicker with EditText
        countryCodePicker.registerCarrierNumberEditText(inputPhone);

        // Load user data
        loadUserData();

        // Set click listeners
        btnUpload.setOnClickListener(v -> checkStoragePermission());
        btnDeletePhoto.setOnClickListener(v -> confirmDeletePhoto());
        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> finish());
        btnInfoPersonnelles.setOnClickListener(v -> {
            // Already on personal info tab
        });
        btnSecurite.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null && currentUser.getProviderData().stream()
                    .anyMatch(info -> "password".equals(info.getProviderId()))) {
                Intent intent = new Intent(Profil.this, SecurityProfil.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Cette fonctionnalité est uniquement disponible pour les utilisateurs authentifiés par email", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeletePhoto() {
        if (!hasProfilePicture) {
            Toast.makeText(this, "Pas de photo de profil à supprimer", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Supprimer la photo")
                .setMessage("Êtes-vous sûr de vouloir supprimer votre photo de profil ?")
                .setPositiveButton("Oui", (dialog, which) -> deleteProfilePhoto())
                .setNegativeButton("Non", null)
                .show();
    }

    private void deleteProfilePhoto() {
        uploadProgress.setVisibility(View.VISIBLE);

        DocumentReference userRef = db.collection("users").document(userId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("profilePictureBase64", "");

        userRef.update(updates).addOnSuccessListener(aVoid -> {
            Glide.with(Profil.this)
                    .load(R.drawable.ic_profil)
                    .transform(new CircleCrop())
                    .into(profilePicture);

            hasProfilePicture = false;
            uploadProgress.setVisibility(View.GONE);
            imageUri = null;
            selectedBitmap = null;
            btnDeletePhoto.setVisibility(View.GONE);
            Toast.makeText(Profil.this, "Photo de profil supprimée", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            uploadProgress.setVisibility(View.GONE);
            Toast.makeText(Profil.this, "Erreur lors de la mise à jour du profil : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void checkStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES :
                Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, STORAGE_PERMISSION_CODE);
        } else {
            openImagePicker();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            Toast.makeText(this, "Permission d'accès aux images refusée.", Toast.LENGTH_LONG).show();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        try {
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Erreur lors de l'ouverture de la galerie : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                selectedBitmap = cropToCircle(bitmap);
                loadSelectedImage();
            } catch (Exception e) {
                Toast.makeText(this, "Erreur lors du traitement de l'image : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode != RESULT_CANCELED) {
            Toast.makeText(this, "Erreur lors de la sélection de l'image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap cropToCircle(Bitmap source) {
        if (source == null) return null;

        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        if (squaredBitmap != source) {
            source.recycle();
        }

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Path path = new Path();
        float radius = size / 2f;
        path.addCircle(radius, radius, radius, Path.Direction.CW);

        canvas.clipPath(path);
        canvas.drawBitmap(squaredBitmap, 0, 0, null);

        squaredBitmap.recycle();
        return output;
    }

    private void loadSelectedImage() {
        RequestOptions requestOptions = new RequestOptions()
                .centerCrop()
                .transform(new CircleCrop())
                .error(R.drawable.ic_profil);

        Glide.with(this)
                .load(selectedBitmap)
                .apply(requestOptions)
                .into(profilePicture);

        btnDeletePhoto.setVisibility(View.VISIBLE);
        hasProfilePicture = true;
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] byteArray = baos.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void loadUserData() {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                inputName.setText(documentSnapshot.getString("name"));
                inputEmail.setText(documentSnapshot.getString("email"));
                String phone = documentSnapshot.getString("phone");
                if (phone != null && !phone.isEmpty()) {
                    try {
                        countryCodePicker.setFullNumber(phone);
                    } catch (Exception e) {
                        inputPhone.setText(phone);
                        Toast.makeText(this, "Erreur lors du chargement du numéro : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                inputAddress.setText(documentSnapshot.getString("address"));
                inputCity.setText(documentSnapshot.getString("city"));
                String profilePicBase64 = documentSnapshot.getString("profilePictureBase64");

                if (profilePicBase64 != null && !profilePicBase64.isEmpty()) {
                    hasProfilePicture = true;
                    btnDeletePhoto.setVisibility(View.VISIBLE);

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
                    hasProfilePicture = false;
                    btnDeletePhoto.setVisibility(View.GONE);

                    Glide.with(this)
                            .load(R.drawable.ic_profil)
                            .transform(new CircleCrop())
                            .into(profilePicture);
                }
            } else {
                Toast.makeText(this, "Aucune donnée utilisateur trouvée", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Échec du chargement du profil : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveProfile() {
        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String phone = countryCodePicker.getFullNumberWithPlus().trim();
        String address = inputAddress.getText().toString().trim();
        String city = inputCity.getText().toString().trim();

        if (name.isEmpty()) {
            inputName.setError("Le nom est requis");
            return;
        }
        if (email.isEmpty()) {
            inputEmail.setError("L'email est requis");
            return;
        }
        if (phone.isEmpty() || !countryCodePicker.isValidFullNumber()) {
            inputPhone.setError("Numéro de téléphone invalide");
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("address", address);
        userData.put("city", city);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && !email.equals(user.getEmail())) {
            user.updateEmail(email).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(this, "Échec de la mise à jour de l'email : " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (selectedBitmap != null) {
            uploadProgress.setVisibility(View.VISIBLE);
            try {
                String base64Image = bitmapToBase64(selectedBitmap);
                if (base64Image.length() > 900000) {
                    uploadProgress.setVisibility(View.GONE);
                    Toast.makeText(this, "L'image est trop grande.", Toast.LENGTH_LONG).show();
                    return;
                }
                userData.put("profilePictureBase64", base64Image);
                updateFirestore(userData);
                hasProfilePicture = true;
                btnDeletePhoto.setVisibility(View.VISIBLE);
                uploadProgress.setVisibility(View.GONE);
            } catch (Exception e) {
                uploadProgress.setVisibility(View.GONE);
                Toast.makeText(this, "Erreur lors du traitement de l'image : " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            DocumentReference userRef = db.collection("users").document(userId);
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && hasProfilePicture) {
                    String profilePicBase64 = documentSnapshot.getString("profilePictureBase64");
                    if (profilePicBase64 != null && !profilePicBase64.isEmpty()) {
                        userData.put("profilePictureBase64", profilePicBase64);
                    }
                }
                updateFirestore(userData);
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Erreur lors de la récupération de l'image : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateFirestore(Map<String, Object> userData) {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.set(userData).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Profil mis à jour avec succès", Toast.LENGTH_SHORT).show();
            loadUserData();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Échec de la mise à jour du profil : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
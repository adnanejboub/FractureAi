package com.fractureai;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SignUpEmail extends AppCompatActivity {

    private static final String TAG = "SignUpEmail";

    private Button btnPhone, btnRegister;
    private TextView loginText;
    private ImageView logo_app;
    private EditText inputName, inputEmail, inputPassword, inputConfirmPassword;
    private LinearLayout btnGoogle, btnFacebook;
    private FrameLayout loadingOverlay;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Google Sign-In
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    // Facebook Sign-In
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up_email);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign-In
        configureGoogleSignIn();

        // Configure Facebook Login
        configureFacebookLogin();

        // Setup Activity Launchers for Google Sign-In
        setupActivityLaunchers();

        // Initialize UI elements
        setupUI();

        // Setup click listeners
        setupClickListeners();
    }

    /**
     * Configure Google Sign-In options
     */
    private void configureGoogleSignIn() {
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la configuration de Google Sign-In : " + e.getMessage(), e);
            Toast.makeText(this, "Configuration Google Sign-In échouée. Contactez le support.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Configure Facebook Login
     */
    private void configureFacebookLogin() {
        try {
            mCallbackManager = CallbackManager.Factory.create();
            LoginManager.getInstance().registerCallback(mCallbackManager,
                    new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            Log.d(TAG, "facebook:onSuccess:" + loginResult);
                            handleFacebookAccessToken(loginResult.getAccessToken());
                        }

                        @Override
                        public void onCancel() {
                            runOnUiThread(() -> {
                                loadingOverlay.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                btnGoogle.setEnabled(true);
                                btnFacebook.setEnabled(true);
                                Log.d(TAG, "facebook:onCancel");
                                Toast.makeText(SignUpEmail.this, "Authentification Facebook annulée", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(@NonNull FacebookException error) {
                            runOnUiThread(() -> {
                                loadingOverlay.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                btnGoogle.setEnabled(true);
                                btnFacebook.setEnabled(true);
                                Log.d(TAG, "facebook:onError", error);
                                Toast.makeText(SignUpEmail.this, "Erreur d'authentification Facebook: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la configuration de Facebook Login : " + e.getMessage(), e);
            Toast.makeText(this, "Configuration Facebook Login échouée. Contactez le support.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Setup Activity Launchers for Google Sign-In
     */
    private void setupActivityLaunchers() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        runOnUiThread(() -> {
                            loadingOverlay.setVisibility(View.GONE);
                            btnRegister.setEnabled(true);
                            btnGoogle.setEnabled(true);
                            btnFacebook.setEnabled(true);
                            Log.d(TAG, "Google Sign-Up: L'utilisateur a annulé ou une erreur s'est produite");
                            Toast.makeText(SignUpEmail.this, "L'inscription avec Google n'a pas pu être complétée. Veuillez réessayer.", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        );
    }

    /**
     * Handle Google Sign-In result
     */
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String idToken = account.getIdToken();
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken, account.getDisplayName(), account.getEmail());
                } else {
                    runOnUiThread(() -> {
                        loadingOverlay.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        btnGoogle.setEnabled(true);
                        btnFacebook.setEnabled(true);
                        Log.e(TAG, "Google Sign-Up: ID token null");
                        Toast.makeText(SignUpEmail.this, "Erreur d'authentification Google. Veuillez réessayer.", Toast.LENGTH_LONG).show();
                    });
                }
            }
        } catch (ApiException e) {
            runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                btnGoogle.setEnabled(true);
                btnFacebook.setEnabled(true);
                Log.e(TAG, "Google Sign-Up a échoué avec le code: " + e.getStatusCode(), e);
                String errorMessage;
                switch (e.getStatusCode()) {
                    case 12500: // SIGN_IN_CANCELLED
                        errorMessage = "L'inscription Google a été annulée";
                        break;
                    case 12501: // SIGN_IN_FAILED
                        errorMessage = "L'inscription Google a échoué. Vérifiez votre connexion internet";
                        break;
                    case 12502: // SIGN_IN_CURRENTLY_IN_PROGRESS
                        errorMessage = "Une inscription Google est déjà en cours";
                        break;
                    default:
                        errorMessage = "Erreur Google Sign-Up (code " + e.getStatusCode() + ")";
                }
                Toast.makeText(SignUpEmail.this, errorMessage, Toast.LENGTH_LONG).show();
            });
        }
    }

    /**
     * Initialize UI elements
     */
    private void setupUI() {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        } else {
            Log.e(TAG, "Vue racine non trouvée");
        }

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
        loadingOverlay = findViewById(R.id.loading_overlay);

        if (logo_app == null || btnPhone == null || loginText == null || btnRegister == null ||
                inputName == null || inputEmail == null || inputPassword == null ||
                inputConfirmPassword == null || btnGoogle == null || btnFacebook == null || loadingOverlay == null) {
            Log.e(TAG, "Une ou plusieurs vues n'existent pas");
            Toast.makeText(this, "Erreur d'initialisation de l'interface", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Setup click listeners for UI elements
     */
    private void setupClickListeners() {
        logo_app.setOnClickListener(v -> {
            try {
                startActivity(new Intent(SignUpEmail.this, MainActivity.class));
            } catch (Exception e) {
                Log.e(TAG, "Erreur logo_app : " + e.getMessage());
            }
        });

        btnPhone.setOnClickListener(v -> {
            try {
                startActivity(new Intent(SignUpEmail.this, SignUpTelephone.class));
            } catch (Exception e) {
                Log.e(TAG, "Erreur btn_phone : " + e.getMessage());
            }
        });

        loginText.setOnClickListener(v -> {
            try {
                startActivity(new Intent(SignUpEmail.this, SignInEmail.class));
            } catch (Exception e) {
                Log.e(TAG, "Erreur loginText : " + e.getMessage());
            }
        });

        btnRegister.setOnClickListener(v -> registerWithEmailPassword());

        btnGoogle.setOnClickListener(v -> signUpWithGoogle());

        btnFacebook.setOnClickListener(v -> signUpWithFacebook());
    }

    /**
     * Check if the device is connected to the internet
     */
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    /**
     * Sign up with Google
     */
    private void signUpWithGoogle() {
        loadingOverlay.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);
        btnGoogle.setEnabled(false);
        btnFacebook.setEnabled(false);

        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            if (!isNetworkConnected()) {
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    btnGoogle.setEnabled(true);
                    btnFacebook.setEnabled(true);
                    Toast.makeText(SignUpEmail.this, "Vérifiez votre connexion internet et réessayez", Toast.LENGTH_LONG).show();
                });
                return;
            }

            try {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    btnGoogle.setEnabled(true);
                    btnFacebook.setEnabled(true);
                    Log.e(TAG, "Erreur lors du lancement de Google Sign-Up: " + e.getMessage(), e);
                    Toast.makeText(SignUpEmail.this, "Impossible de lancer l'inscription Google. Veuillez réessayer", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Sign up with Facebook
     */
    private void signUpWithFacebook() {
        loadingOverlay.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);
        btnGoogle.setEnabled(false);
        btnFacebook.setEnabled(false);

        if (!isNetworkConnected()) {
            loadingOverlay.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
            btnGoogle.setEnabled(true);
            btnFacebook.setEnabled(true);
            Toast.makeText(this, "Vérifiez votre connexion internet et réessayez", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            LoginManager.getInstance().logOut();
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        } catch (Exception e) {
            loadingOverlay.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
            btnGoogle.setEnabled(true);
            btnFacebook.setEnabled(true);
            Log.e(TAG, "Erreur lors du lancement de Facebook Sign-Up: " + e.getMessage(), e);
            Toast.makeText(this, "Impossible de lancer l'inscription Facebook. Veuillez réessayer", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Register user with email and password
     */
    private void registerWithEmailPassword() {
        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString();
        String confirmPassword = inputConfirmPassword.getText().toString();

        if (name.isEmpty()) {
            inputName.setError("Le nom est requis");
            inputName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            inputEmail.setError("L'email est requis");
            inputEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            inputPassword.setError("Le mot de passe est requis");
            inputPassword.requestFocus();
            return;
        }

        if (password.length() < 8) {
            inputPassword.setError("Le mot de passe doit contenir au moins 8 caractères");
            inputPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Les mots de passe ne correspondent pas");
            inputConfirmPassword.requestFocus();
            return;
        }

        if (!isNetworkConnected()) {
            Toast.makeText(this, "Vérifiez votre connexion internet et réessayez", Toast.LENGTH_LONG).show();
            return;
        }

        loadingOverlay.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);
        btnGoogle.setEnabled(false);
        btnFacebook.setEnabled(false);
        Toast.makeText(SignUpEmail.this, "Inscription en cours...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Update user profile with display name
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            // Save user data to Firestore
                                            saveUserToFirestore(user.getUid(), name, email, "", "", "");
                                            Log.d(TAG, "User profile updated.");
                                            Toast.makeText(SignUpEmail.this, "Inscription réussie", Toast.LENGTH_SHORT).show();
                                            redirectToSignIn();
                                        } else {
                                            runOnUiThread(() -> {
                                                loadingOverlay.setVisibility(View.GONE);
                                                btnRegister.setEnabled(true);
                                                btnGoogle.setEnabled(true);
                                                btnFacebook.setEnabled(true);
                                                Log.w(TAG, "User profile update failed", profileTask.getException());
                                                Toast.makeText(SignUpEmail.this, "Erreur lors de la mise à jour du profil", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    });
                        }
                    } else {
                        runOnUiThread(() -> {
                            loadingOverlay.setVisibility(View.GONE);
                            btnRegister.setEnabled(true);
                            btnGoogle.setEnabled(true);
                            btnFacebook.setEnabled(true);
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignUpEmail.this, "Échec de l'inscription: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    /**
     * Authenticate with Google and save to Firestore
     */
    private void firebaseAuthWithGoogle(String idToken, String name, String email) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + idToken.substring(0, Math.min(10, idToken.length())) + "...");

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    runOnUiThread(() -> {
                        loadingOverlay.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Save user data to Firestore
                                saveUserToFirestore(user.getUid(), name != null ? name : "", email != null ? email : "", "", "", "");
                                Toast.makeText(SignUpEmail.this, "Inscription Google réussie !", Toast.LENGTH_SHORT).show();
                                redirectToSignIn();
                            }
                        } else {
                            btnRegister.setEnabled(true);
                            btnGoogle.setEnabled(true);
                            btnFacebook.setEnabled(true);
                            Log.e(TAG, "signInWithCredential:failure", task.getException());
                            String errorMessage = "Erreur d'inscription Google";
                            if (task.getException() != null) {
                                errorMessage += ": " + task.getException().getMessage();
                            }
                            Toast.makeText(SignUpEmail.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                });
    }

    /**
     * Handle Facebook Access Token and save to Firestore
     */
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    runOnUiThread(() -> {
                        loadingOverlay.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Save user data to Firestore
                                saveUserToFirestore(user.getUid(), user.getDisplayName() != null ? user.getDisplayName() : "",
                                        user.getEmail() != null ? user.getEmail() : "", "", "", "");
                                Toast.makeText(SignUpEmail.this, "Inscription Facebook réussie !", Toast.LENGTH_SHORT).show();
                                redirectToSignIn();
                            }
                        } else {
                            btnRegister.setEnabled(true);
                            btnGoogle.setEnabled(true);
                            btnFacebook.setEnabled(true);
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            String errorMessage = "Échec de l'inscription Facebook";
                            if (task.getException() != null) {
                                if (task.getException().getMessage() != null &&
                                        task.getException().getMessage().contains("adresse email existe déjà")) {
                                    errorMessage = "Un compte existe déjà avec cette adresse e-mail. Veuillez vous connecter avec cette méthode.";
                                } else {
                                    errorMessage += ": " + task.getException().getMessage();
                                }
                            }
                            Toast.makeText(SignUpEmail.this, errorMessage, Toast.LENGTH_LONG).show();
                            LoginManager.getInstance().logOut();
                        }
                    });
                });
    }

    /**
     * Save user data to Firestore
     */
    private void saveUserToFirestore(String uid, String name, String email, String phone, String address, String city) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("address", address);
        userData.put("city", city);
        userData.put("profilePicture", ""); // Placeholder for profile picture URL

        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User data saved to Firestore"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save user data to Firestore", e);
                    Toast.makeText(SignUpEmail.this, "Erreur lors de l'enregistrement des données: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Redirect to SignInEmail activity
     */
    private void redirectToSignIn() {
        loadingOverlay.setVisibility(View.GONE);
        Intent intent = new Intent(SignUpEmail.this, SignInEmail.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
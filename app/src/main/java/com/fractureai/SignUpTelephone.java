package com.fractureai;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.hbb20.CountryCodePicker;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class SignUpTelephone extends AppCompatActivity {

    private static final String TAG = "SignUpTelephone";
    private static final int RC_SIGN_IN = 9001;

    private CountryCodePicker countryCodePicker;
    private EditText inputName, inputPhone, inputPassword, inputConfirmPassword;
    private Button btnRegister, btnEmail, btnPhone;
    private ImageView logo_app;
    private FrameLayout loadingOverlay;
    private LinearLayout btnGoogle, btnFacebook;
    private TextView loginText;

    // Firebase Auth
    private FirebaseAuth mAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    // Google Sign-In
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    // Facebook Sign-In
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialisation du SDK Facebook
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_sign_up_telephone);

        // Initialiser Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialiser les callbacks Phone Auth
        setupPhoneAuthCallbacks();

        // Configuration de Google Sign-In
        configureGoogleSignIn();

        // Configuration de Facebook Login
        configureFacebookLogin();

        // Configuration des lanceurs d'activité
        setupActivityLaunchers();

        // Initialisation des vues
        countryCodePicker = findViewById(R.id.country_code_picker);
        inputName = findViewById(R.id.input_name);
        inputPhone = findViewById(R.id.input_phone);
        inputPassword = findViewById(R.id.input_password);
        inputConfirmPassword = findViewById(R.id.input_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        btnEmail = findViewById(R.id.btn_email);
        btnPhone = findViewById(R.id.btn_phone);
        loadingOverlay = findViewById(R.id.loading_overlay);
        btnGoogle = findViewById(R.id.btn_google);
        btnFacebook = findViewById(R.id.btn_facebook);
        logo_app = findViewById(R.id.logo);
        loginText = findViewById(R.id.login_text);

        // Vérifier que les vues existent
        if (countryCodePicker == null || inputName == null || inputPhone == null || inputPassword == null ||
                inputConfirmPassword == null || btnRegister == null || btnEmail == null || btnPhone == null ||
                loadingOverlay == null || btnGoogle == null || btnFacebook == null || logo_app == null ||
                loginText == null) {
            Log.e(TAG, "Une ou plusieurs vues n'existent pas");
            Toast.makeText(this, "Erreur d'initialisation de l'interface", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Lier le CountryCodePicker au champ de saisie
        countryCodePicker.registerCarrierNumberEditText(inputPhone);

        // Setup click listeners
        setupClickListeners();
    }

    /**
     * Configure les options de connexion Google
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
     * Configure la connexion Facebook
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
                            loadingOverlay.setVisibility(View.GONE);
                            Log.d(TAG, "facebook:onCancel");
                            Toast.makeText(SignUpTelephone.this, "Connexion Facebook annulée",
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(FacebookException error) {
                            loadingOverlay.setVisibility(View.GONE);
                            Log.e(TAG, "facebook:onError", error);
                            Toast.makeText(SignUpTelephone.this,
                                    "Erreur de connexion Facebook: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la configuration de Facebook Login : " + e.getMessage(), e);
            Toast.makeText(this, "Configuration Facebook Login échouée. Contactez le support.",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Configure les lanceurs d'activité
     */
    private void setupActivityLaunchers() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        loadingOverlay.setVisibility(View.GONE);
                        Log.d(TAG, "Google Sign-In: L'utilisateur a annulé la connexion ou une erreur s'est produite");
                        Toast.makeText(this, "La connexion avec Google n'a pas pu être complétée. Veuillez réessayer.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Gère le résultat de la connexion Google
     */
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String idToken = account.getIdToken();
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken);
                } else {
                    loadingOverlay.setVisibility(View.GONE);
                    Log.e(TAG, "Google Sign-In: ID token null");
                    Toast.makeText(this, "Erreur d'authentification Google. Veuillez réessayer.",
                            Toast.LENGTH_LONG).show();
                }
            }
        } catch (ApiException e) {
            loadingOverlay.setVisibility(View.GONE);
            Log.e(TAG, "Google Sign-In a échoué avec le code: " + e.getStatusCode(), e);

            String errorMessage;
            switch (e.getStatusCode()) {
                case 12500:
                    errorMessage = "La connexion Google a été annulée";
                    break;
                case 12501:
                    errorMessage = "La connexion Google a échoué. Vérifiez votre connexion internet";
                    break;
                case 12502:
                    errorMessage = "Une connexion Google est déjà en cours";
                    break;
                default:
                    errorMessage = "Erreur Google Sign-In (code " + e.getStatusCode() + ")";
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Configure les callbacks pour l'authentification par téléphone Firebase
     */
    private void setupPhoneAuthCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);
                Toast.makeText(SignUpTelephone.this, "Vérification automatique réussie", Toast.LENGTH_SHORT).show();
                loadingOverlay.setVisibility(View.GONE);
                btnRegister.setVisibility(View.VISIBLE);
                signInWithCredential(credential, inputName.getText().toString().trim(), inputPassword.getText().toString().trim());
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);
                loadingOverlay.setVisibility(View.GONE);
                btnRegister.setVisibility(View.VISIBLE);

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    inputPhone.setError("Numéro de téléphone invalide");
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    Toast.makeText(SignUpTelephone.this, "Trop de tentatives, veuillez réessayer plus tard", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SignUpTelephone.this, "Erreur de vérification : " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent:" + verificationId);
                loadingOverlay.setVisibility(View.GONE);
                btnRegister.setVisibility(View.VISIBLE);
                mVerificationId = verificationId;
                mResendToken = token;
                redirectToVerifyCode(verificationId, countryCodePicker.getFullNumberWithPlus());
            }
        };
    }

    /**
     * Valide les champs et lance le processus d'inscription
     */
    private void validateAndProceed() {
        // Masquer le clavier (si nécessaire)
        hideKeyboard();

        String name = inputName.getText().toString().trim();
        String phoneNumber = countryCodePicker.getFullNumberWithPlus();
        String password = inputPassword.getText().toString();
        String confirmPassword = inputConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(name)) {
            inputName.setError("Le nom est requis");
            return;
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            inputPhone.setError("Le numéro de téléphone est requis");
            return;
        }

        if (password.isEmpty()) {
            inputPassword.setError("Le mot de passe est requis");
            return;
        }

        if (password.length() < 8) {
            inputPassword.setError("Le mot de passe doit contenir au moins 8 caractères");
            return;
        }

        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Les mots de passe ne correspondent pas");
            return;
        }

        btnRegister.setVisibility(View.INVISIBLE);
        loadingOverlay.setVisibility(View.VISIBLE);
        startPhoneNumberVerification(phoneNumber, name, password);
    }

    /**
     * Démarre la vérification du numéro de téléphone avec Firebase
     * @param phoneNumber Numéro de téléphone avec code pays
     * @param name Nom de l'utilisateur
     * @param password Mot de passe
     */
    private void startPhoneNumberVerification(String phoneNumber, String name, String password) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    /**
     * Redirige vers l'écran de vérification du code
     * @param verificationId ID de vérification Firebase
     * @param phoneNumber Numéro de téléphone complet
     */
    private void redirectToVerifyCode(String verificationId, String phoneNumber) {
        Intent intent = new Intent(SignUpTelephone.this, VerifyCode.class);
        intent.putExtra("VERIFICATION_ID", verificationId);
        intent.putExtra("PHONE_NUMBER", phoneNumber);
        intent.putExtra("NAME", inputName.getText().toString().trim());
        intent.putExtra("PASSWORD", inputPassword.getText().toString().trim());
        startActivity(intent);
    }

    /**
     * Connecte l'utilisateur avec les credentials Firebase et met à jour le profil
     * @param credential Credentials d'authentification
     * @param name Nom de l'utilisateur
     * @param password Mot de passe
     */
    private void signInWithCredential(PhoneAuthCredential credential, String name, String password) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        boolean isNewUser = task.getResult().getAdditionalUserInfo() != null && task.getResult().getAdditionalUserInfo().isNewUser();
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d(TAG, "User profile updated.");
                                            user.updatePassword(password)
                                                    .addOnCompleteListener(passwordTask -> {
                                                        loadingOverlay.setVisibility(View.GONE);
                                                        if (passwordTask.isSuccessful()) {
                                                            Log.d(TAG, "Password updated.");
                                                            Toast.makeText(SignUpTelephone.this, "Inscription réussie !", Toast.LENGTH_SHORT).show();
                                                            if (isNewUser) {
                                                                redirectToProfileActivity();
                                                            } else {
                                                                redirectToMainActivity();
                                                            }
                                                        } else {
                                                            btnRegister.setVisibility(View.VISIBLE);
                                                            Log.w(TAG, "Password update failed", passwordTask.getException());
                                                            Toast.makeText(SignUpTelephone.this, "Erreur lors de la mise à jour du mot de passe", Toast.LENGTH_SHORT).show();
                                                            mAuth.signOut();
                                                        }
                                                    });
                                        } else {
                                            btnRegister.setVisibility(View.VISIBLE);
                                            loadingOverlay.setVisibility(View.GONE);
                                            Log.w(TAG, "User profile update failed", profileTask.getException());
                                            Toast.makeText(SignUpTelephone.this, "Erreur lors de la mise à jour du profil", Toast.LENGTH_SHORT).show();
                                            mAuth.signOut();
                                        }
                                    });
                        }
                    } else {
                        btnRegister.setVisibility(View.VISIBLE);
                        loadingOverlay.setVisibility(View.GONE);
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(SignUpTelephone.this, "Erreur d'authentification. Veuillez réessayer.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SignUpTelephone.this, "Erreur : " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Méthode pour la connexion avec Google
     */
    private void signInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            loadingOverlay.setVisibility(View.VISIBLE);

            if (!isNetworkConnected()) {
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(this, "Vérifiez votre connexion internet et réessayez", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            } catch (Exception e) {
                loadingOverlay.setVisibility(View.GONE);
                Log.e(TAG, "Erreur lors du lancement de Google Sign-In: " + e.getMessage(), e);
                Toast.makeText(this, "Impossible de lancer la connexion Google. Veuillez réessayer", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Authentifie l'utilisateur avec Google
     * @param idToken Jeton d'identification Google
     */
    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + idToken.substring(0, Math.min(10, idToken.length())) + "...");

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    loadingOverlay.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            boolean isNewUser = task.getResult().getAdditionalUserInfo() != null && task.getResult().getAdditionalUserInfo().isNewUser();
                            Toast.makeText(this, "Connexion Google réussie !", Toast.LENGTH_SHORT).show();
                            if (isNewUser) {
                                updateProfileAndRedirect(user, inputName.getText().toString().trim(), inputPassword.getText().toString().trim());
                            } else {
                                redirectToMainActivity();
                            }
                        }
                    } else {
                        Log.e(TAG, "signInWithCredential:failure", task.getException());
                        String errorMessage = "Erreur d'authentification Google";
                        if (task.getException() != null) {
                            errorMessage += ": " + task.getException().getMessage();
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Méthode pour la connexion avec Facebook
     */
    private void signInWithFacebook() {
        loadingOverlay.setVisibility(View.VISIBLE);

        if (!isNetworkConnected()) {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Vérifiez votre connexion internet et réessayez", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            LoginManager.getInstance().logOut();
            LoginManager.getInstance().logInWithReadPermissions(
                    this,
                    Arrays.asList("email", "public_profile")
            );
        } catch (Exception e) {
            loadingOverlay.setVisibility(View.GONE);
            Log.e(TAG, "Erreur lors du lancement de Facebook Login: " + e.getMessage(), e);
            Toast.makeText(this, "Impossible de lancer la connexion Facebook. Veuillez réessayer", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Traite le jeton d'accès Facebook et authentifie l'utilisateur avec Firebase
     * @param token Jeton d'accès Facebook
     */
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    loadingOverlay.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            boolean isNewUser = task.getResult().getAdditionalUserInfo() != null && task.getResult().getAdditionalUserInfo().isNewUser();
                            Toast.makeText(this, "Connexion Facebook réussie !", Toast.LENGTH_SHORT).show();
                            if (isNewUser) {
                                updateProfileAndRedirect(user, inputName.getText().toString().trim(), inputPassword.getText().toString().trim());
                            } else {
                                redirectToMainActivity();
                            }
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        String errorMessage = "Échec de l'authentification Facebook";
                        if (task.getException() != null && task.getException().getMessage().contains("email address is already")) {
                            errorMessage = "Un compte existe déjà avec cette adresse e-mail. Veuillez vous connecter avec cette méthode.";
                        } else if (task.getException() != null) {
                            errorMessage += ": " + task.getException().getMessage();
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        LoginManager.getInstance().logOut();
                    }
                });
    }

    /**
     * Met à jour le profil et redirige l'utilisateur
     * @param user Utilisateur Firebase
     * @param name Nom de l'utilisateur
     * @param password Mot de passe
     */
    private void updateProfileAndRedirect(FirebaseUser user, String name, String password) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        user.updateProfile(profileUpdates)
                .addOnCompleteListener(profileTask -> {
                    if (profileTask.isSuccessful()) {
                        Log.d(TAG, "User profile updated.");
                        user.updatePassword(password)
                                .addOnCompleteListener(passwordTask -> {
                                    if (passwordTask.isSuccessful()) {
                                        Log.d(TAG, "Password updated.");
                                        redirectToProfileActivity();
                                    } else {
                                        Log.w(TAG, "Password update failed", passwordTask.getException());
                                        Toast.makeText(this, "Erreur lors de la mise à jour du mot de passe", Toast.LENGTH_SHORT).show();
                                        mAuth.signOut();
                                    }
                                });
                    } else {
                        Log.w(TAG, "User profile update failed", profileTask.getException());
                        Toast.makeText(this, "Erreur lors de la mise à jour du profil", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                });
    }

    /**
     * Redirige vers l'activité principale
     */
    private void redirectToMainActivity() {
        Intent intent = new Intent(SignUpTelephone.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Redirige vers l'activité de profil
     */
    private void redirectToProfileActivity() {
        Intent intent = new Intent(SignUpTelephone.this, Profil.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Vérifie si l'appareil est connecté à internet
     */
    private boolean isNetworkConnected() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    /**
     * Masque le clavier virtuel
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mCallbackManager != null) {
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Setup click listeners for UI elements
     */
    private void setupClickListeners() {
        logo_app.setOnClickListener(v -> {
            try {
                startActivity(new Intent(SignUpTelephone.this, MainActivity.class));
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du démarrage de l'activité MainActivity : " + e.getMessage());
            }
        });

        btnEmail.setOnClickListener(v -> {
            try {
                btnEmail.setBackgroundResource(R.drawable.toggle_background_selected);
                btnEmail.setTextColor(getResources().getColor(android.R.color.white));
                btnPhone.setBackgroundResource(R.drawable.toggle_background_unselected);
                btnPhone.setTextColor(getResources().getColor(R.color.purple));
                Intent intent = new Intent(SignUpTelephone.this, SignUpEmail.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du démarrage de l'activité SignUpEmail : " + e.getMessage());
            }
        });

        btnPhone.setOnClickListener(v -> {
            btnPhone.setBackgroundResource(R.drawable.toggle_background_selected);
            btnPhone.setTextColor(getResources().getColor(android.R.color.white));
            btnEmail.setBackgroundResource(R.drawable.toggle_background_unselected);
            btnEmail.setTextColor(getResources().getColor(R.color.purple));
        });

        loginText.setOnClickListener(v -> {
            try {
                startActivity(new Intent(SignUpTelephone.this, SignIn.class));
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du démarrage de l'activité SignIn : " + e.getMessage());
            }
        });

        btnRegister.setOnClickListener(v -> validateAndProceed());

        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        btnFacebook.setOnClickListener(v -> signInWithFacebook());
    }
}
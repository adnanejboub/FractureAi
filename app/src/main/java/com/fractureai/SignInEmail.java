package com.fractureai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.facebook.FacebookSdk;
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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;

import java.util.Arrays;

public class SignInEmail extends AppCompatActivity {
    private static final String TAG = "SignInEmail";
    private static final int RC_SIGN_IN = 9001;

    private EditText etEmail, etPassword;
    private Button btnLogin, btnTele, btnEmail;
    private TextView forgotPassword, signupText;
    private ImageView logo_app;
    private LinearLayout btnGoogle, btnFacebook;
    private View loading_overlay;
    private String name;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager mCallbackManager;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private ActivityResultLauncher<Intent> facebookSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialisation du SDK Facebook
        FacebookSdk.sdkInitialize(getApplicationContext());

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in_email);

        // Initialisation de Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configuration de Google Sign-In
        configureGoogleSignIn();

        // Configuration de Facebook Login
        configureFacebookLogin();

        // Configuration des lanceurs d'activité
        setupActivityLaunchers();

        // Configuration de l'interface utilisateur
        setupUI();

        // Configuration des écouteurs de clics
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
            // Création du gestionnaire de rappels Facebook
            mCallbackManager = CallbackManager.Factory.create();

            // Configuration des rappels de connexion Facebook
            LoginManager.getInstance().registerCallback(mCallbackManager,
                    new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            Log.d(TAG, "facebook:onSuccess:" + loginResult);
                            handleFacebookAccessToken(loginResult.getAccessToken());
                        }

                        @Override
                        public void onCancel() {
                            loading_overlay.setVisibility(View.GONE);
                            Log.d(TAG, "facebook:onCancel");
                            Toast.makeText(SignInEmail.this, "Connexion Facebook annulée",
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(FacebookException error) {
                            loading_overlay.setVisibility(View.GONE);
                            Log.e(TAG, "facebook:onError", error);
                            Toast.makeText(SignInEmail.this,
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
        // Lanceur pour Google Sign-In
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        loading_overlay.setVisibility(View.GONE);
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
                    // Authentification avec Firebase
                    firebaseAuthWithGoogle(idToken);
                } else {
                    loading_overlay.setVisibility(View.GONE);
                    Log.e(TAG, "Google Sign-In: ID token null");
                    Toast.makeText(this, "Erreur d'authentification Google. Veuillez réessayer.",
                            Toast.LENGTH_LONG).show();
                }
            }
        } catch (ApiException e) {
            loading_overlay.setVisibility(View.GONE);
            Log.e(TAG, "Google Sign-In a échoué avec le code: " + e.getStatusCode(), e);

            String errorMessage;
            switch (e.getStatusCode()) {
                case 12500: // SIGN_IN_CANCELLED
                    errorMessage = "La connexion Google a été annulée";
                    break;
                case 12501: // SIGN_IN_FAILED
                    errorMessage = "La connexion Google a échoué. Vérifiez votre connexion internet";
                    break;
                case 12502: // SIGN_IN_CURRENTLY_IN_PROGRESS
                    errorMessage = "Une connexion Google est déjà en cours";
                    break;
                default:
                    errorMessage = "Erreur Google Sign-In (code " + e.getStatusCode() + ")";
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Initialise les éléments de l'interface utilisateur
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

        // Récupération des références aux vues
        etEmail = findViewById(R.id.input_email);
        etPassword = findViewById(R.id.input_password);
        btnLogin = findViewById(R.id.seConnecter);
        forgotPassword = findViewById(R.id.forgot_password);
        signupText = findViewById(R.id.signup_text);
        logo_app = findViewById(R.id.logo);
        btnTele = findViewById(R.id.btn_phone);
        btnEmail = findViewById(R.id.btn_email);
        btnGoogle = findViewById(R.id.btn_google);
        btnFacebook = findViewById(R.id.btn_facebook);
        loading_overlay = findViewById(R.id.loading_overlay);

        // Vérification des vues
        if (etEmail == null || etPassword == null || btnLogin == null ||
                forgotPassword == null || signupText == null || logo_app == null ||
                btnTele == null || btnEmail == null || btnGoogle == null ||
                btnFacebook == null || loading_overlay == null) {
            Log.e(TAG, "Une ou plusieurs vues n'existent pas");
            Toast.makeText(this, "Erreur d'initialisation de l'interface", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Récupération du nom depuis l'intent
        name = getIntent().getStringExtra("name");
        if (name != null) {
            Toast.makeText(this, "Bienvenue : " + name, Toast.LENGTH_SHORT).show();
        }
    }


    private void setupClickListeners() {
        // Bouton de connexion par email
        btnLogin.setOnClickListener(v -> authenticateUser());

        // Toggle entre email et téléphone
        btnEmail.setOnClickListener(v -> {
            btnEmail.setBackgroundResource(R.drawable.toggle_background_selected);
            btnEmail.setTextColor(getResources().getColor(android.R.color.white));
            btnTele.setBackgroundResource(R.drawable.toggle_background_unselected);
            btnTele.setTextColor(getResources().getColor(R.color.purple));
        });

        btnTele.setOnClickListener(v -> {
            try {
                btnTele.setBackgroundResource(R.drawable.toggle_background_selected);
                btnTele.setTextColor(getResources().getColor(android.R.color.white));
                btnEmail.setBackgroundResource(R.drawable.toggle_background_unselected);
                btnEmail.setTextColor(getResources().getColor(R.color.purple));
                Intent intent = new Intent(SignInEmail.this, SignIn.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Erreur pour la redirection à SignIn : " + e.getMessage(), e);
                Toast.makeText(this, "Erreur de navigation. Veuillez réessayer", Toast.LENGTH_SHORT).show();
            }
        });


        forgotPassword.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(SignInEmail.this, ForgotPassword.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Erreur pour la redirection à ForgotPassword : " + e.getMessage(), e);
                Toast.makeText(this, "Erreur de navigation. Veuillez réessayer", Toast.LENGTH_SHORT).show();
            }
        });


        signupText.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(SignInEmail.this, SignUpTelephone.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Erreur pour la redirection à SignUpTelephone : " + e.getMessage(), e);
                Toast.makeText(this, "Erreur de navigation. Veuillez réessayer", Toast.LENGTH_SHORT).show();
            }
        });


        logo_app.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(SignInEmail.this, MainActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Erreur pour la redirection à MainActivity : " + e.getMessage(), e);
                Toast.makeText(this, "Erreur de navigation. Veuillez réessayer", Toast.LENGTH_SHORT).show();
            }
        });


        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        btnFacebook.setOnClickListener(v -> signInWithFacebook());
    }

    /**
     * Méthode améliorée pour la connexion avec Google
     */
    private void signInWithGoogle() {
        // Déconnexion préalable pour éviter les problèmes de sessions
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            loading_overlay.setVisibility(View.VISIBLE);

            // Vérification de la connectivité réseau
            if (!isNetworkConnected()) {
                loading_overlay.setVisibility(View.GONE);
                Toast.makeText(this, "Vérifiez votre connexion internet et réessayez",
                        Toast.LENGTH_LONG).show();
                return;
            }

            try {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            } catch (Exception e) {
                loading_overlay.setVisibility(View.GONE);
                Log.e(TAG, "Erreur lors du lancement de Google Sign-In: " + e.getMessage(), e);
                Toast.makeText(this, "Impossible de lancer la connexion Google. Veuillez réessayer",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Vérifie si l'appareil est connecté à internet
     * @return true si connecté, false sinon
     */
    private boolean isNetworkConnected() {
        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    /**
     * Méthode améliorée pour la connexion avec Facebook
     */
    private void signInWithFacebook() {
        // Afficher l'overlay de chargement
        loading_overlay.setVisibility(View.VISIBLE);

        // Vérification de la connectivité réseau
        if (!isNetworkConnected()) {
            loading_overlay.setVisibility(View.GONE);
            Toast.makeText(this, "Vérifiez votre connexion internet et réessayez",
                    Toast.LENGTH_LONG).show();
            return;
        }

        try {
            // Déconnexion préalable pour éviter les problèmes de sessions
            LoginManager.getInstance().logOut();

            // Demande des permissions nécessaires
            LoginManager.getInstance().logInWithReadPermissions(
                    this,
                    Arrays.asList("email", "public_profile")
            );
        } catch (Exception e) {
            loading_overlay.setVisibility(View.GONE);
            Log.e(TAG, "Erreur lors du lancement de Facebook Login: " + e.getMessage(), e);
            Toast.makeText(this, "Impossible de lancer la connexion Facebook. Veuillez réessayer",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Authentifie l'utilisateur avec email et mot de passe
     */
    private void authenticateUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation des entrées
        if (email.isEmpty()) {
            etEmail.setError("L'email est requis");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Veuillez entrer un email valide");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Le mot de passe est requis");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 8) {
            etPassword.setError("Le mot de passe doit contenir au moins 8 caractères");
            etPassword.requestFocus();
            return;
        }

        // Vérification de la connectivité réseau
        if (!isNetworkConnected()) {
            Toast.makeText(this, "Vérifiez votre connexion internet et réessayez",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Désactivation du bouton de connexion et affichage du chargement
        btnLogin.setEnabled(false);
        loading_overlay.setVisibility(View.VISIBLE);

        // Connexion avec Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);
                    loading_overlay.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(this, "Connexion réussie !", Toast.LENGTH_SHORT).show();
                            redirectToMainActivity();
                        }
                    } else {
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidUserException e) {
                            etEmail.setError("Cet email n'est pas enregistré");
                            etEmail.requestFocus();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            etPassword.setError("Mot de passe incorrect");
                            etPassword.requestFocus();
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur d'authentification : " + e.getMessage(), e);
                            Toast.makeText(this, "Erreur d'authentification. Veuillez réessayer plus tard.",
                                    Toast.LENGTH_LONG).show();
                        }
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
                    loading_overlay.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(this, "Connexion Google réussie !", Toast.LENGTH_SHORT).show();
                            redirectToMainActivity();
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
     * Traite le jeton d'accès Facebook et authentifie l'utilisateur avec Firebase
     * @param token Jeton d'accès Facebook
     */
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        loading_overlay.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(SignInEmail.this, "Connexion Facebook réussie !",
                                    Toast.LENGTH_SHORT).show();
                            redirectToMainActivity();
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());

                            String errorMessage = "Échec de l'authentification Facebook";
                            if (task.getException() != null) {
                                // Si l'utilisateur a déjà un compte avec la même adresse e-mail
                                if (task.getException().getMessage() != null &&
                                        task.getException().getMessage().contains("adresse email existe déjà")) {
                                    errorMessage = "Un compte existe déjà avec cette adresse e-mail. Veuillez vous connecter avec cette méthode.";
                                } else {
                                    errorMessage += ": " + task.getException().getMessage();
                                }
                            }

                            Toast.makeText(SignInEmail.this, errorMessage,
                                    Toast.LENGTH_LONG).show();

                            // Déconnexion de Facebook pour éviter les problèmes lors des tentatives suivantes
                            LoginManager.getInstance().logOut();
                        }
                    }
                });
    }

    /**
     * Redirige vers l'activité principale
     */
    private void redirectToMainActivity() {
        Intent intent = new Intent(SignInEmail.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
package com.fractureai;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.hbb20.CountryCodePicker;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class SignIn extends AppCompatActivity {

    private static final String TAG = "SignInPhone";
    private static final int RC_SIGN_IN = 9001;

    private CountryCodePicker countryCodePicker;
    private EditText inputPhone;
    private Button btnLogin;
    private ImageView logo_app;
    private Button btnEmail;
    private Button btnPhone;
    private View loading_overlay;
    private LinearLayout btnGoogle;
    private LinearLayout btnFacebook;
    private TextView signupText;

    // Firebase Phone Auth
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

        setContentView(R.layout.activity_sign_in);

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
        countryCodePicker = findViewById(R.id.countryCodePicker);
        inputPhone = findViewById(R.id.input_phone);
        btnLogin = findViewById(R.id.btn_login);
        btnEmail = findViewById(R.id.btn_email);
        btnPhone = findViewById(R.id.btn_phone);
        loading_overlay = findViewById(R.id.loading_overlay);
        btnGoogle = findViewById(R.id.btn_google);
        btnFacebook = findViewById(R.id.btn_facebook);
        logo_app = findViewById(R.id.logo);
        signupText = findViewById(R.id.signup_text);

        if (countryCodePicker == null || inputPhone == null || btnLogin == null ||
                btnEmail == null || btnPhone == null || loading_overlay == null ||
                btnGoogle == null || btnFacebook == null || logo_app == null || signupText == null) {
            Log.e(TAG, "Une ou plusieurs vues n'existent pas");
            return;
        }

        countryCodePicker.registerCarrierNumberEditText(inputPhone);

        logo_app.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(SignIn.this, MainActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du démarrage de l'activité MainActivity : " + e.getMessage());
            }
        });

        // Gestionnaire de clic pour le bouton de connexion
        btnLogin.setOnClickListener(v -> validateAndProceed());

        // Gestionnaire de clic pour le bouton Email
        btnEmail.setOnClickListener(v -> {
            btnEmail.setBackgroundResource(R.drawable.toggle_background_selected);
            btnEmail.setTextColor(getResources().getColor(android.R.color.white));
            btnPhone.setBackgroundResource(R.drawable.toggle_background_unselected);
            btnPhone.setTextColor(getResources().getColor(R.color.purple));

            try {
                Intent intent = new Intent(SignIn.this, SignInEmail.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Erreur pour la redirection à l'activité SignInEmail : " + e.getMessage());
            }
        });

        btnPhone.setOnClickListener(v -> {
            btnPhone.setBackgroundResource(R.drawable.toggle_background_selected);
            btnPhone.setTextColor(getResources().getColor(android.R.color.white));
            btnEmail.setBackgroundResource(R.drawable.toggle_background_unselected);
            btnEmail.setTextColor(getResources().getColor(R.color.purple));
        });

        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        btnFacebook.setOnClickListener(v -> signInWithFacebook());

        signupText.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(SignIn.this, SignUpTelephone.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Erreur pour la redirection à l'activité SignUpTelephone : " + e.getMessage());
            }
        });
    }

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
                            loading_overlay.setVisibility(View.GONE);
                            Log.d(TAG, "facebook:onCancel");
                            Toast.makeText(SignIn.this, "Connexion Facebook annulée",
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(FacebookException error) {
                            loading_overlay.setVisibility(View.GONE);
                            Log.e(TAG, "facebook:onError", error);
                            Toast.makeText(SignIn.this,
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

    private void setupActivityLaunchers() {
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

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String idToken = account.getIdToken();
                if (idToken != null) {
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

    private void setupPhoneAuthCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);
                Toast.makeText(SignIn.this, "Vérification automatique réussie", Toast.LENGTH_SHORT).show();
                loading_overlay.setVisibility(View.GONE);
                btnLogin.setVisibility(View.VISIBLE);
                signInWithCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);
                loading_overlay.setVisibility(View.GONE);
                btnLogin.setVisibility(View.VISIBLE);

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    inputPhone.setError("Numéro de téléphone invalide");
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    Toast.makeText(SignIn.this, "Trop de tentatives, veuillez réessayer plus tard", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SignIn.this, "Erreur de vérification : " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent:" + verificationId);
                loading_overlay.setVisibility(View.GONE);
                btnLogin.setVisibility(View.VISIBLE);
                mVerificationId = verificationId;
                mResendToken = token;
                redirectToVerifyCode(verificationId, null, null); // Pas de nom ni mot de passe pour SignIn
            }
        };
    }

    private void validateAndProceed() {
        hideKeyboard();

        String phoneNumber = countryCodePicker.getFullNumberWithPlus();
        String rawPhoneNumber = inputPhone.getText().toString().trim();

        if (TextUtils.isEmpty(rawPhoneNumber)) {
            inputPhone.setError("Veuillez entrer votre numéro de téléphone");
            return;
        }

        btnLogin.setVisibility(View.INVISIBLE);
        loading_overlay.setVisibility(View.VISIBLE);

        startPhoneNumberVerification(phoneNumber);
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void redirectToVerifyCode(String verificationId, String name, String password) {
        Intent intent = new Intent(SignIn.this, VerifyCode.class);
        intent.putExtra("PHONE_NUMBER", countryCodePicker.getFullNumberWithPlus());
        intent.putExtra("VERIFICATION_ID", verificationId);
        if (name != null) intent.putExtra("NAME", name);
        if (password != null) intent.putExtra("PASSWORD", password);
        startActivity(intent);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                        Intent intent;
                        if (isNewUser) {
                            intent = new Intent(SignIn.this, Profil.class);
                        } else {
                            intent = new Intent(SignIn.this, MainActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        loading_overlay.setVisibility(View.GONE);
                        btnLogin.setVisibility(View.VISIBLE);
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(SignIn.this, "Erreur d'authentification. Veuillez réessayer.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SignIn.this, "Erreur : " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void signInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            loading_overlay.setVisibility(View.VISIBLE);

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
                            boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                            Toast.makeText(this, "Connexion Google réussie !", Toast.LENGTH_SHORT).show();
                            if (isNewUser) {
                                redirectToProfileActivity();
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

    private void signInWithFacebook() {
        loading_overlay.setVisibility(View.VISIBLE);

        if (!isNetworkConnected()) {
            loading_overlay.setVisibility(View.GONE);
            Toast.makeText(this, "Vérifiez votre connexion internet et réessayez",
                    Toast.LENGTH_LONG).show();
            return;
        }

        try {
            LoginManager.getInstance().logOut();

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

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    loading_overlay.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                            Toast.makeText(this, "Connexion Facebook réussie !",
                                    Toast.LENGTH_SHORT).show();
                            if (isNewUser) {
                                redirectToProfileActivity();
                            } else {
                                redirectToMainActivity();
                            }
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());

                        String errorMessage = "Échec de l'authentification Facebook";
                        if (task.getException() != null) {
                            if (task.getException().getMessage() != null &&
                                    task.getException().getMessage().contains("email address is already")) {
                                errorMessage = "Un compte existe déjà avec cette adresse e-mail. Veuillez vous connecter avec cette méthode.";
                            } else {
                                errorMessage += ": " + task.getException().getMessage();
                            }
                        }

                        Toast.makeText(SignIn.this, errorMessage,
                                Toast.LENGTH_LONG).show();

                        LoginManager.getInstance().logOut();
                    }
                });
    }

    private void redirectToMainActivity() {
        Intent intent = new Intent(SignIn.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void redirectToProfileActivity() {
        Intent intent = new Intent(SignIn.this, Profil.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean isNetworkConnected() {
        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

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
}
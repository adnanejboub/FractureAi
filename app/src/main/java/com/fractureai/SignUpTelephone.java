package com.fractureai;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SignUpTelephone extends AppCompatActivity {

    private static final String TAG = "SignUpTelephone";

    private CountryCodePicker countryCodePicker;
    private EditText inputName, inputPhone, inputPassword, inputConfirmPassword;
    private Button btnRegister, btnEmail, btnPhone;
    private ImageView logo_app;
    private FrameLayout loadingOverlay;
    private LinearLayout btnGoogle, btnFacebook;
    private TextView loginText;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
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
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_sign_up_telephone);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Phone Auth Callbacks
        setupPhoneAuthCallbacks();

        // Configure Google Sign-In
        configureGoogleSignIn();

        // Configure Facebook Login
        configureFacebookLogin();

        // Setup Activity Launchers
        setupActivityLaunchers();

        // Initialize UI
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

        // Verify UI elements
        if (countryCodePicker == null || inputName == null || inputPhone == null || inputPassword == null ||
                inputConfirmPassword == null || btnRegister == null || btnEmail == null || btnPhone == null ||
                loadingOverlay == null || btnGoogle == null || btnFacebook == null || logo_app == null ||
                loginText == null) {
            Log.e(TAG, "One or more UI views are null");
            Toast.makeText(this, "Erreur d'initialisation de l'interface", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Link CountryCodePicker
        countryCodePicker.registerCarrierNumberEditText(inputPhone);

        // Setup click listeners
        setupClickListeners();
    }

    private void configureGoogleSignIn() {
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            Log.e(TAG, "Error configuring Google Sign-In: " + e.getMessage(), e);
            Toast.makeText(this, "Échec de la configuration de Google Sign-In", Toast.LENGTH_LONG).show();
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
                            runOnUiThread(() -> {
                                loadingOverlay.setVisibility(View.GONE);
                                Log.d(TAG, "facebook:onCancel");
                                Toast.makeText(SignUpTelephone.this, "Connexion Facebook annulée", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(FacebookException error) {
                            runOnUiThread(() -> {
                                loadingOverlay.setVisibility(View.GONE);
                                Log.e(TAG, "facebook:onError", error);
                                Toast.makeText(SignUpTelephone.this, "Erreur de connexion Facebook : " + error.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error configuring Facebook Login: " + e.getMessage(), e);
            Toast.makeText(this, "Échec de la configuration de la connexion Facebook", Toast.LENGTH_LONG).show();
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
                        runOnUiThread(() -> {
                            loadingOverlay.setVisibility(View.GONE);
                            Log.d(TAG, "Google Sign-In cancelled or failed");
                            Toast.makeText(this, "Échec de la connexion Google. Veuillez réessayer.", Toast.LENGTH_SHORT).show();
                        });
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
                    firebaseAuthWithGoogle(idToken, account.getDisplayName(), account.getEmail());
                } else {
                    runOnUiThread(() -> {
                        loadingOverlay.setVisibility(View.GONE);
                        Log.e(TAG, "Google Sign-In: ID token null");
                        Toast.makeText(this, "Erreur d'authentification Google. Veuillez réessayer.", Toast.LENGTH_LONG).show();
                    });
                }
            }
        } catch (ApiException e) {
            runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.GONE);
                Log.e(TAG, "Google Sign-In failed with code: " + e.getStatusCode(), e);
                String errorMessage;
                switch (e.getStatusCode()) {
                    case 12500:
                        errorMessage = "Connexion Google annulée";
                        break;
                    case 12501:
                        errorMessage = "Échec de la connexion Google. Vérifiez votre connexion Internet.";
                        break;
                    case 12502:
                        errorMessage = "Connexion Google déjà en cours";
                        break;
                    default:
                        errorMessage = "Erreur de connexion Google (code " + e.getStatusCode() + ")";
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            });
        }
    }

    private void setupPhoneAuthCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);
                runOnUiThread(() -> {
                    Toast.makeText(SignUpTelephone.this, "Vérification automatique réussie", Toast.LENGTH_SHORT).show();
                    loadingOverlay.setVisibility(View.GONE);
                    btnRegister.setVisibility(View.VISIBLE);
                });
                signInWithCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                runOnUiThread(() -> {
                    Log.w(TAG, "onVerificationFailed", e);
                    loadingOverlay.setVisibility(View.GONE);
                    btnRegister.setVisibility(View.VISIBLE);
                    if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        inputPhone.setError("Numéro de téléphone invalide");
                    } else if (e instanceof FirebaseTooManyRequestsException) {
                        Toast.makeText(SignUpTelephone.this, "Trop de tentatives, réessayez plus tard", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(SignUpTelephone.this, "Erreur de vérification : " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent:" + verificationId);
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    btnRegister.setVisibility(View.VISIBLE);
                    Toast.makeText(SignUpTelephone.this, "Code de vérification envoyé", Toast.LENGTH_SHORT).show();
                });
                mVerificationId = verificationId;
                mResendToken = token;
                redirectToVerifyCode(verificationId, countryCodePicker.getFullNumberWithPlus());
            }
        };
    }

    private void validateAndProceed() {
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

        if (!isNetworkConnected()) {
            Toast.makeText(this, "Vérifiez votre connexion Internet et réessayez", Toast.LENGTH_LONG).show();
            return;
        }

        btnRegister.setVisibility(View.INVISIBLE);
        loadingOverlay.setVisibility(View.VISIBLE);
        startPhoneNumberVerification(phoneNumber);
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void redirectToVerifyCode(String verificationId, String phoneNumber) {
        Intent intent = new Intent(SignUpTelephone.this, VerifyCode.class);
        intent.putExtra("VERIFICATION_ID", verificationId);
        intent.putExtra("PHONE_NUMBER", phoneNumber);
        intent.putExtra("NAME", inputName.getText().toString().trim());
        intent.putExtra("PASSWORD", inputPassword.getText().toString().trim());
        startActivity(intent);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        boolean isNewUser = task.getResult().getAdditionalUserInfo() != null && task.getResult().getAdditionalUserInfo().isNewUser();
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Data should be handled in VerifyCode activity
                            if (isNewUser) {
                                redirectToProfileActivity();
                            } else {
                                redirectToMainActivity();
                            }
                        }
                    } else {
                        runOnUiThread(() -> {
                            loadingOverlay.setVisibility(View.GONE);
                            btnRegister.setVisibility(View.VISIBLE);
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            String message = task.getException() != null ? task.getException().getMessage() : "Erreur inconnue";
                            Toast.makeText(SignUpTelephone.this, "Échec de l'authentification : " + message, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void signInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            if (!isNetworkConnected()) {
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Vérifiez votre connexion Internet et réessayez", Toast.LENGTH_LONG).show();
                });
                return;
            }
            loadingOverlay.setVisibility(View.VISIBLE);
            try {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Log.e(TAG, "Error launching Google Sign-In: " + e.getMessage(), e);
                    Toast.makeText(this, "Échec du lancement de Google Sign-In", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

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
                                boolean isNewUser = task.getResult().getAdditionalUserInfo() != null && task.getResult().getAdditionalUserInfo().isNewUser();
                                if (isNetworkConnected()) {
                                    saveUserToFirestore(user.getUid(), name != null ? name : inputName.getText().toString().trim(),
                                            email != null ? email : "", "", "", "");
                                    Toast.makeText(this, "Connexion Google réussie !", Toast.LENGTH_SHORT).show();
                                    if (isNewUser) {
                                        updateProfileAndRedirect(user, inputName.getText().toString().trim(), inputPassword.getText().toString().trim());
                                    } else {
                                        redirectToMainActivity();
                                    }
                                } else {
                                    Toast.makeText(this, "Aucune connexion Internet. Données non enregistrées.", Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                }
                            }
                        } else {
                            Log.e(TAG, "signInWithCredential:failure", task.getException());
                            String message = task.getException() != null ? task.getException().getMessage() : "Erreur inconnue";
                            Toast.makeText(this, "Échec de la connexion Google : " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                });
    }

    private void signInWithFacebook() {
        if (!isNetworkConnected()) {
            runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(this, "Vérifiez votre connexion Internet et réessayez", Toast.LENGTH_LONG).show();
            });
            return;
        }
        loadingOverlay.setVisibility(View.VISIBLE);
        try {
            LoginManager.getInstance().logOut();
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        } catch (Exception e) {
            runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.GONE);
                Log.e(TAG, "Error launching Facebook Login: " + e.getMessage(), e);
                Toast.makeText(this, "Échec du lancement de la connexion Facebook", Toast.LENGTH_LONG).show();
            });
        }
    }

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
                                boolean isNewUser = task.getResult().getAdditionalUserInfo() != null && task.getResult().getAdditionalUserInfo().isNewUser();
                                if (isNetworkConnected()) {
                                    saveUserToFirestore(user.getUid(),
                                            user.getDisplayName() != null ? user.getDisplayName() : inputName.getText().toString().trim(),
                                            user.getEmail() != null ? user.getEmail() : "", "", "", "");
                                    Toast.makeText(this, "Connexion Facebook réussie !", Toast.LENGTH_SHORT).show();
                                    if (isNewUser) {
                                        updateProfileAndRedirect(user, inputName.getText().toString().trim(), inputPassword.getText().toString().trim());
                                    } else {
                                        redirectToMainActivity();
                                    }
                                } else {
                                    Toast.makeText(this, "Aucune connexion Internet. Données non enregistrées.", Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                }
                            }
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            String errorMessage = "Échec de la connexion Facebook";
                            if (task.getException() != null) {
                                if (task.getException().getMessage().contains("email address is already")) {
                                    errorMessage = "Un compte existe déjà avec cet email. Veuillez vous connecter avec cette méthode.";
                                } else {
                                    errorMessage += " : " + task.getException().getMessage();
                                }
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                            LoginManager.getInstance().logOut();
                        }
                    });
                });
    }

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
                                        runOnUiThread(() -> {
                                            Log.w(TAG, "Password update failed", passwordTask.getException());
                                            Toast.makeText(this, "Échec de la mise à jour du mot de passe : " + passwordTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        });
                                        mAuth.signOut();
                                    }
                                });
                    } else {
                        runOnUiThread(() -> {
                            Log.w(TAG, "User profile update failed", profileTask.getException());
                            Toast.makeText(this, "Échec de la mise à jour du profil : " + profileTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        });
                        mAuth.signOut();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String email, String phone, String address, String city) {
        Log.d(TAG, "Attempting to save user to Firestore: uid=" + uid + ", name=" + name + ", email=" + email + ", phone=" + phone);
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("address", address);
        userData.put("city", city);
        userData.put("profilePicture", "");
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data successfully saved to Firestore for UID: " + uid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save user data to Firestore for UID: " + uid + ", Error: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(SignUpTelephone.this, "Échec de l'enregistrement des données : " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                });
    }

    private void redirectToMainActivity() {
        Intent intent = new Intent(SignUpTelephone.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void redirectToProfileActivity() {
        Intent intent = new Intent(SignUpTelephone.this, Profil.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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

    private void setupClickListeners() {
        logo_app.setOnClickListener(v -> {
            try {
                startActivity(new Intent(SignUpTelephone.this, MainActivity.class));
            } catch (Exception e) {
                Log.e(TAG, "Error starting MainActivity: " + e.getMessage());
            }
        });

        btnEmail.setOnClickListener(v -> {
            try {
                btnEmail.setBackgroundResource(R.drawable.toggle_background_selected);
                btnEmail.setTextColor(getResources().getColor(android.R.color.white));
                btnPhone.setBackgroundResource(R.drawable.toggle_background_unselected);
                btnPhone.setTextColor(getResources().getColor(R.color.purple));
                startActivity(new Intent(SignUpTelephone.this, SignUpEmail.class));
            } catch (Exception e) {
                Log.e(TAG, "Error starting SignUpEmail: " + e.getMessage());
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
                Log.e(TAG, "Error starting SignIn: " + e.getMessage());
            }
        });

        btnRegister.setOnClickListener(v -> validateAndProceed());

        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        btnFacebook.setOnClickListener(v -> signInWithFacebook());
    }
}
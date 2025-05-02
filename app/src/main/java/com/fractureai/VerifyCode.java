package com.fractureai;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class VerifyCode extends AppCompatActivity {

    private static final String TAG = "VerifyCode";

    private TextView phoneNumberText;
    private TextView resendCodeText;
    private Button btnVerify;

    private EditText[] codeDigits = new EditText[6];

    private String phoneNumber;
    private CountDownTimer resendTimer;
    private boolean canResend = false;

    // Firebase Phone Auth
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_code);

        // Initialiser Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Récupérer le numéro de téléphone depuis l'intent
        Intent intent = getIntent();
        phoneNumber = intent.getStringExtra("PHONE_NUMBER");
        mVerificationId = intent.getStringExtra("VERIFICATION_ID");

        // Initialiser les vues
        phoneNumberText = findViewById(R.id.phone_number);
        resendCodeText = findViewById(R.id.resend_code);
        btnVerify = findViewById(R.id.btn_verify);

        // Initialiser les champs de saisie du code
        codeDigits[0] = findViewById(R.id.code_digit_1);
        codeDigits[1] = findViewById(R.id.code_digit_2);
        codeDigits[2] = findViewById(R.id.code_digit_3);
        codeDigits[3] = findViewById(R.id.code_digit_4);
        codeDigits[4] = findViewById(R.id.code_digit_5);
        codeDigits[5] = findViewById(R.id.code_digit_6);

        // Afficher le numéro de téléphone
        phoneNumberText.setText(phoneNumber);

        // Configurer la navigation automatique entre les champs de saisie
        setupCodeInputs();

        // Initialiser les callbacks Firebase Phone Auth
        setupFirebaseCallbacks();

        // Configurer le bouton de vérification
        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyCodeWithFirebase();
            }
        });

        // Configurer le texte de renvoi de code
        setupResendCode();

        // Donner le focus au premier champ et afficher le clavier
        codeDigits[0].requestFocus();
        showKeyboard(codeDigits[0]);
    }

    /**
     * Initialise les callbacks pour l'authentification par téléphone Firebase
     */
    private void setupFirebaseCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                // Cette méthode est appelée lorsque la vérification est automatiquement complétée
                // par le service de Google Play (par exemple, si l'utilisateur a déjà validé son numéro récemment)
                Log.d(TAG, "onVerificationCompleted:" + credential);

                // Connecter l'utilisateur automatiquement
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                // Cette méthode est appelée si une erreur se produit lors de la demande de vérification
                Log.w(TAG, "onVerificationFailed", e);

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Numéro de téléphone invalide
                    Toast.makeText(VerifyCode.this, "Numéro de téléphone invalide", Toast.LENGTH_SHORT).show();
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // Trop de tentatives de vérification
                    Toast.makeText(VerifyCode.this, "Trop de tentatives, veuillez réessayer plus tard", Toast.LENGTH_SHORT).show();
                } else {
                    // Autre erreur
                    Toast.makeText(VerifyCode.this, "Erreur lors de la vérification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                // Le code SMS a été envoyé au numéro de téléphone fourni
                Log.d(TAG, "onCodeSent:" + verificationId);

                // Enregistrer le ID de vérification et le token de renvoi
                mVerificationId = verificationId;
                mResendToken = token;

                Toast.makeText(VerifyCode.this, "Code envoyé", Toast.LENGTH_SHORT).show();
            }
        };
    }

    /**
     * Configure les champs de saisie du code pour passer automatiquement au champ suivant
     */
    private void setupCodeInputs() {
        for (int i = 0; i < codeDigits.length; i++) {
            final int currentIndex = i;

            codeDigits[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // Si un chiffre est entré, passer au champ suivant
                    if (s.length() == 1) {
                        // Passer au champ suivant s'il existe
                        if (currentIndex < codeDigits.length - 1) {
                            codeDigits[currentIndex + 1].requestFocus();
                        } else {
                            // Si c'est le dernier champ, masquer le clavier
                            hideKeyboard(codeDigits[currentIndex]);
                        }
                    }

                    // Activer le bouton de vérification si tous les champs sont remplis
                    checkAllFieldsFilled();
                }
            });

            // Configurer le comportement de suppression pour revenir au champ précédent
            codeDigits[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL &&
                        codeDigits[currentIndex].getText().toString().isEmpty() &&
                        currentIndex > 0) {
                    // Revenir au champ précédent si le champ actuel est vide
                    codeDigits[currentIndex - 1].requestFocus();
                    codeDigits[currentIndex - 1].setText("");
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Vérifie si tous les champs du code sont remplis
     */
    private void checkAllFieldsFilled() {
        boolean allFilled = true;

        for (EditText digit : codeDigits) {
            if (digit.getText().toString().isEmpty()) {
                allFilled = false;
                break;
            }
        }

        // Activer ou désactiver le bouton de vérification
        btnVerify.setEnabled(allFilled);

        if (allFilled) {
            btnVerify.setAlpha(1.0f);
        } else {
            btnVerify.setAlpha(0.6f);
        }
    }

    /**
     * Configure le texte de renvoi de code avec un compte à rebours
     */
    private void setupResendCode() {
        // Désactiver le renvoi pendant le compte à rebours
        canResend = false;

        // Mettre à jour le texte de renvoi
        resendCodeText.setText("Renvoi possible dans 60 secondes");
        resendCodeText.setTextColor(getResources().getColor(android.R.color.darker_gray));

        // Démarrer le compte à rebours
        resendTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                resendCodeText.setText("Renvoi possible dans " + (millisUntilFinished / 1000) + " secondes");
            }

            @Override
            public void onFinish() {
                // Activer le renvoi
                canResend = true;
                resendCodeText.setText("Vous n'avez pas reçu le code ? Renvoyer");
                resendCodeText.setTextColor(getResources().getColor(R.color.purple));
            }
        }.start();

        // Configurer le clic sur le texte de renvoi
        resendCodeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canResend) {
                    // Renvoyer le code via Firebase
                    resendVerificationCodeWithFirebase();

                    // Redémarrer le compte à rebours
                    setupResendCode();
                }
            }
        });
    }

    /**
     * Renvoie le code de vérification via Firebase
     */
    private void resendVerificationCodeWithFirebase() {
        if (mResendToken != null) {
            PhoneAuthOptions options =
                    PhoneAuthOptions.newBuilder(mAuth)
                            .setPhoneNumber(phoneNumber)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(this)
                            .setCallbacks(mCallbacks)
                            .setForceResendingToken(mResendToken)
                            .build();

            PhoneAuthProvider.verifyPhoneNumber(options);

            // Réinitialiser les champs
            for (EditText digit : codeDigits) {
                digit.setText("");
            }

            // Donner le focus au premier champ
            codeDigits[0].requestFocus();
        } else {
            // Si nous n'avons pas de token, c'est probablement que le code initial n'a pas été envoyé correctement
            Toast.makeText(this, "Erreur: impossible de renvoyer le code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Vérifie le code saisi avec Firebase
     */
    private void verifyCodeWithFirebase() {
        // Construire le code complet
        StringBuilder codeBuilder = new StringBuilder();
        for (EditText digit : codeDigits) {
            codeBuilder.append(digit.getText().toString());
        }
        String code = codeBuilder.toString();

        // Vérifier que le code a 6 chiffres
        if (code.length() != 6) {
            Toast.makeText(this, "Veuillez entrer un code valide à 6 chiffres", Toast.LENGTH_SHORT).show();
            return;
        }

        // Vérifier que nous avons un ID de vérification
        if (mVerificationId == null) {
            Toast.makeText(this, "Erreur: aucun code de vérification n'a été envoyé", Toast.LENGTH_SHORT).show();
            return;
        }

        // Créer les credentials avec le code saisi et l'ID de vérification
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);

        // Connecter l'utilisateur avec ces credentials
        signInWithPhoneAuthCredential(credential);
    }

    /**
     * Connecte l'utilisateur avec les credentials d'authentification par téléphone
     */
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Connexion réussie
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();

                            // Informer l'utilisateur
                            Toast.makeText(VerifyCode.this, "Authentification réussie!", Toast.LENGTH_SHORT).show();

                            // Rediriger vers l'activité principale ou de profil
                            redirectToMainActivity(user);
                        } else {
                            // Échec de la connexion
                            Log.w(TAG, "signInWithCredential:failure", task.getException());

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // Le code saisi est invalide
                                Toast.makeText(VerifyCode.this, "Code incorrect. Veuillez réessayer.", Toast.LENGTH_SHORT).show();
                            } else {
                                // Autre erreur
                                Toast.makeText(VerifyCode.this, "Erreur d'authentification: " + task.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }

                            // Réinitialiser les champs
                            for (EditText digit : codeDigits) {
                                digit.setText("");
                            }

                            // Donner le focus au premier champ
                            codeDigits[0].requestFocus();
                        }
                    }
                });
    }

    /**
     * Redirige vers l'activité principale après une authentification réussie
     */
    private void redirectToMainActivity(FirebaseUser user) {
        // Vérifier si c'est un nouvel utilisateur (première connexion)
        boolean isNewUser = user.getMetadata().getCreationTimestamp() == user.getMetadata().getLastSignInTimestamp();

        // Rediriger vers l'activité appropriée
        Intent intent;
        if (isNewUser) {
            // Rediriger vers l'écran de complétion du profil pour les nouveaux utilisateurs
            intent = new Intent(VerifyCode.this, Profil.class);
        } else {
            // Rediriger vers l'écran principal pour les utilisateurs existants
            intent = new Intent(VerifyCode.this, MainActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Affiche le clavier pour une vue spécifique
     */
    private void showKeyboard(View view) {
        if (view.requestFocus()) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Masque le clavier pour une vue spécifique
     */
    private void hideKeyboard(View view) {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Arrêter le compte à rebours si l'activité est détruite
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }
}
package com.fractureai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class VerifyCode extends AppCompatActivity {

    private static final String TAG = "VerifyCode";

    private TextView phoneNumberText;
    private TextView resendCodeText;
    private Button btnVerify;

    private EditText[] codeDigits = new EditText[6];

    private String phoneNumber;
    private String name;
    private String password;
    private CountDownTimer resendTimer;
    private boolean canResend = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_code);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        phoneNumber = intent.getStringExtra("PHONE_NUMBER");
        mVerificationId = intent.getStringExtra("VERIFICATION_ID");
        name = intent.getStringExtra("NAME"); // Peut être null si pas transmis depuis SignIn
        password = intent.getStringExtra("PASSWORD"); // Peut être null si pas transmis

        phoneNumberText = findViewById(R.id.phone_number);
        resendCodeText = findViewById(R.id.resend_code);
        btnVerify = findViewById(R.id.btn_verify);

        codeDigits[0] = findViewById(R.id.code_digit_1);
        codeDigits[1] = findViewById(R.id.code_digit_2);
        codeDigits[2] = findViewById(R.id.code_digit_3);
        codeDigits[3] = findViewById(R.id.code_digit_4);
        codeDigits[4] = findViewById(R.id.code_digit_5);
        codeDigits[5] = findViewById(R.id.code_digit_6);

        if (phoneNumber == null || mVerificationId == null) {
            Log.e(TAG, "Données manquantes : phoneNumber=" + phoneNumber + ", verificationId=" + mVerificationId);
            Toast.makeText(this, "Erreur : Données de vérification manquantes", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        phoneNumberText.setText(phoneNumber);

        setupCodeInputs();

        setupFirebaseCallbacks();

        btnVerify.setOnClickListener(v -> verifyCodeWithFirebase());

        setupResendCode();

        codeDigits[0].requestFocus();
        showKeyboard(codeDigits[0]);
    }

    private void setupFirebaseCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(VerifyCode.this, "Numéro de téléphone invalide", Toast.LENGTH_SHORT).show();
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    Toast.makeText(VerifyCode.this, "Trop de tentatives, veuillez réessayer plus tard", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(VerifyCode.this, "Erreur lors de la vérification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent:" + verificationId);
                mVerificationId = verificationId;
                mResendToken = token;
                Toast.makeText(VerifyCode.this, "Code envoyé", Toast.LENGTH_SHORT).show();
            }
        };
    }

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
                    if (s.length() == 1) {
                        if (currentIndex < codeDigits.length - 1) {
                            codeDigits[currentIndex + 1].requestFocus();
                        } else {
                            hideKeyboard(codeDigits[currentIndex]);
                        }
                    }
                    checkAllFieldsFilled();
                }
            });

            codeDigits[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL &&
                        codeDigits[currentIndex].getText().toString().isEmpty() &&
                        currentIndex > 0) {
                    codeDigits[currentIndex - 1].requestFocus();
                    codeDigits[currentIndex - 1].setText("");
                    return true;
                }
                return false;
            });
        }
    }

    private void checkAllFieldsFilled() {
        boolean allFilled = true;

        for (EditText digit : codeDigits) {
            if (digit.getText().toString().isEmpty()) {
                allFilled = false;
                break;
            }
        }

        btnVerify.setEnabled(allFilled);
        btnVerify.setAlpha(allFilled ? 1.0f : 0.6f);
    }

    private void setupResendCode() {
        canResend = false;
        resendCodeText.setText("Renvoi possible dans 60 secondes");
        resendCodeText.setTextColor(getResources().getColor(android.R.color.darker_gray));

        resendTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                resendCodeText.setText("Renvoi possible dans " + (millisUntilFinished / 1000) + " secondes");
            }

            @Override
            public void onFinish() {
                canResend = true;
                resendCodeText.setText("Vous n'avez pas reçu le code ? Renvoyer");
                resendCodeText.setTextColor(getResources().getColor(R.color.purple));
            }
        }.start();

        resendCodeText.setOnClickListener(v -> {
            if (canResend) {
                resendVerificationCodeWithFirebase();
                setupResendCode();
            }
        });
    }

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

            for (EditText digit : codeDigits) {
                digit.setText("");
            }
            codeDigits[0].requestFocus();
        } else {
            Toast.makeText(this, "Erreur: impossible de renvoyer le code", Toast.LENGTH_SHORT).show();
        }
    }

    private void verifyCodeWithFirebase() {
        StringBuilder codeBuilder = new StringBuilder();
        for (EditText digit : codeDigits) {
            codeBuilder.append(digit.getText().toString());
        }
        String code = codeBuilder.toString();

        if (code.length() != 6) {
            Toast.makeText(this, "Veuillez entrer un code valide à 6 chiffres", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mVerificationId == null) {
            Toast.makeText(this, "Erreur: aucun code de vérification n'a été envoyé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = task.getResult().getUser();

                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name != null ? name : "")
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d(TAG, "User profile updated.");

                                            // Ne mettez à jour le mot de passe que s'il est fourni
                                            if (password != null && !password.isEmpty()) {
                                                user.updatePassword(password)
                                                        .addOnCompleteListener(passwordTask -> {
                                                            if (passwordTask.isSuccessful()) {
                                                                Log.d(TAG, "Password updated.");
                                                                saveUserToFirestore(user.getUid(), name != null ? name : "",
                                                                        user.getEmail() != null ? user.getEmail() : "",
                                                                        phoneNumber, "", "");
                                                                Toast.makeText(VerifyCode.this, "Authentification et enregistrement réussis !", Toast.LENGTH_SHORT).show();
                                                                redirectToMainActivity(user);
                                                            } else {
                                                                Log.w(TAG, "Password update failed", passwordTask.getException());
                                                                Toast.makeText(VerifyCode.this, "Échec de la mise à jour du mot de passe : " + passwordTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                                mAuth.signOut();
                                                            }
                                                        });
                                            } else {
                                                saveUserToFirestore(user.getUid(), name != null ? name : "",
                                                        user.getEmail() != null ? user.getEmail() : "",
                                                        phoneNumber, "", "");
                                                Toast.makeText(VerifyCode.this, "Authentification réussie !", Toast.LENGTH_SHORT).show();
                                                redirectToMainActivity(user);
                                            }
                                        } else {
                                            Log.w(TAG, "User profile update failed", profileTask.getException());
                                            Toast.makeText(VerifyCode.this, "Échec de la mise à jour du profil : " + profileTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                            mAuth.signOut();
                                        }
                                    });
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(VerifyCode.this, "Code incorrect. Veuillez réessayer.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(VerifyCode.this, "Erreur d'authentification : " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        for (EditText digit : codeDigits) {
                            digit.setText("");
                        }
                        codeDigits[0].requestFocus();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String email, String phone, String address, String city) {
        Log.d(TAG, "Attempting to save user to Firestore: uid=" + uid + ", name=" + name + ", email=" + email + ", phone=" + phone);
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("name", name != null ? name : "");
        userData.put("email", email != null ? email : "");
        userData.put("phone", phone != null ? phone : "");
        userData.put("address", address != null ? address : "");
        userData.put("city", city != null ? city : "");
        userData.put("profilePicture", "");
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data successfully saved to Firestore for UID: " + uid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save user data to Firestore for UID: " + uid + ", Error: " + e.getMessage(), e);
                    Toast.makeText(VerifyCode.this, "Échec de l'enregistrement des données : " + e.getMessage(), Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                });
    }

    private void redirectToMainActivity(FirebaseUser user) {
        boolean isNewUser = user.getMetadata().getCreationTimestamp() == user.getMetadata().getLastSignInTimestamp();
        Intent intent;
        if (isNewUser) {
            intent = new Intent(VerifyCode.this, Profil.class);
        } else {
            intent = new Intent(VerifyCode.this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }
}
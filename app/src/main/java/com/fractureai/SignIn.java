package com.fractureai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SignIn extends AppCompatActivity {
    private Button btnEmail;
    private TextView forgotPassword;
    private TextView signupText;
    private ImageView logo_app ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        } else {
            Log.e("SignIn", "Root view not found");
        }

        btnEmail = findViewById(R.id.btn_email);
        forgotPassword = findViewById(R.id.forgot_password);
        signupText = findViewById(R.id.signup_text);
        logo_app = findViewById(R.id.logo);

        if (logo_app == null) {
            Log.e("SignIn", "Logo de l'app n'existe pas ");
            return;
        }

        if (btnEmail == null) {
            Log.e("SignIn", "la button email n'existe pas");
            return;
        }

        if(forgotPassword == null){
            Log.e("SignIn", "le text forgetPassword n'existe pas");
            return;
        }

        if(signupText == null){
            Log.e("SignIn", "le text signupText n'existe pas");
            return;
        }

        logo_app.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(SignIn.this , MainActivity.class) ;
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("SignIn", "Erreur lors du démarrage de l'activité MainActivity : " + e.getMessage());
                }

            }
        });

        btnEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(SignIn.this, SignInEmail.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("SignIn", "Erreur pour la redirection à l'activité de sign_in_email: " + e.getMessage());
                }
            }
        });

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(SignIn.this, ForgotPassword.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("SignIn", "Erreur pour la redirection à l'activité de forgot_password: " + e.getMessage());
                }
            }
        });

        signupText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(SignIn.this, SignUpTelephone.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("SignIn", "Erreur pour la redirection à l'activité de sign_in_telephone: " + e.getMessage());
                }
            }
        });
    }
}
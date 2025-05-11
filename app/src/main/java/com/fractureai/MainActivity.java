package com.fractureai;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ImageButton userButton;
    private TextView savoirPlus;
    private Button startButton;
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;
    private NavigationView navigationView;
    private FrameLayout loadingOverlay;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialiser Firebase Auth
        try {
            mAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            Log.e("MainActivity", "Erreur lors de l'initialisation de FirebaseAuth : " + e.getMessage());
            Toast.makeText(this, "Erreur d'initialisation Firebase", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialiser le DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout == null) {
            Log.e("MainActivity", "DrawerLayout n'existe pas");
            return;
        }

        // Configurer le bouton de menu pour ouvrir le drawer
        btnMenu = findViewById(R.id.btn_menu);
        if (btnMenu == null) {
            Log.e("MainActivity", "Bouton menu n'existe pas");
            return;
        }
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Configurer le NavigationView
        navigationView = findViewById(R.id.nav_view);
        if (navigationView == null) {
            Log.e("MainActivity", "NavigationView n'existe pas");
            return;
        }
        navigationView.setNavigationItemSelectedListener(this);

        // Configurer le FrameLayout (loader)
        loadingOverlay = findViewById(R.id.loading_overlay);
        if (loadingOverlay == null) {
            Log.e("MainActivity", "FrameLayout loading_overlay n'existe pas");
            return;
        }

        // Configurer le bouton utilisateur
        userButton = findViewById(R.id.btn_user);
        if (userButton == null) {
            Log.e("MainActivity", "Bouton utilisateur n'existe pas");
            return;
        }
        updateUserButton();

        // Configurer le bouton "En savoir plus"
        savoirPlus = findViewById(R.id.savoir_plus);
        if (savoirPlus == null) {
            Log.e("MainActivity", "TextView savoir_plus n'existe pas");
            return;
        }
        savoirPlus.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MainActivity.this, AboutUs.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e("MainActivity", "Erreur lors du démarrage de l'activité AboutUs : " + e.getMessage());
                Toast.makeText(this, "Erreur de navigation", Toast.LENGTH_SHORT).show();
            }
        });

        // Configurer le bouton "Commencer maintenant"
        startButton = findViewById(R.id.btn_start);
        if (startButton == null) {
            Log.e("MainActivity", "Bouton start n'existe pas");
            return;
        }
        startButton.setOnClickListener(v -> {
            try {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                Intent intent;
                if (currentUser != null) {
                    // Utilisateur authentifié : rediriger vers uploadImagesActivity
                    intent = new Intent(MainActivity.this, uploadImagesActivity.class);
                } else {
                    // Utilisateur non authentifié : rediriger vers SignInEmail
                    intent = new Intent(MainActivity.this, SignInEmail.class);
                }
                startActivity(intent);
            } catch (Exception e) {
                Log.e("MainActivity", "Erreur lors du démarrage de l'activité : " + e.getMessage());
                Toast.makeText(this, "Erreur de navigation", Toast.LENGTH_SHORT).show();
            }
        });

        // Mettre à jour le menu en fonction de l'état d'authentification
        updateNavigationMenu();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Mettre à jour le menu et le bouton utilisateur lorsque l'activité démarre
        updateNavigationMenu();
        updateUserButton();
    }

    private void updateNavigationMenu() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Menu menu = navigationView.getMenu();

        // Éléments individuels pour "Connexion" et "Déconnexion"
        MenuItem loginItem = menu.findItem(R.id.nav_login);
        MenuItem logoutItem = menu.findItem(R.id.nav_logout);

        // Éléments réservés aux utilisateurs authentifiés
        MenuItem historyItem = menu.findItem(R.id.nav_history);
        MenuItem profileItem = menu.findItem(R.id.nav_profile);
        MenuItem scanItem = menu.findItem(R.id.nav_scan);
        MenuItem helpItem = menu.findItem(R.id.nav_help);
        MenuItem aboutItem = menu.findItem(R.id.nav_about);

        if (currentUser != null) {
            // Utilisateur authentifié
            if (historyItem != null) historyItem.setVisible(true);
            if (profileItem != null) profileItem.setVisible(true);
            if (scanItem != null) scanItem.setVisible(true);
            if (helpItem != null) helpItem.setVisible(true); // Aide visible pour les connectés
            if (aboutItem != null) aboutItem.setVisible(true);
            if (loginItem != null) loginItem.setVisible(false);
            if (logoutItem != null) logoutItem.setVisible(true);

            // Sélectionner l'élément "Accueil" par défaut
            navigationView.setCheckedItem(R.id.nav_home);
        } else {
            // Utilisateur non authentifié
            if (historyItem != null) historyItem.setVisible(false);
            if (profileItem != null) profileItem.setVisible(false);
            if (scanItem != null) scanItem.setVisible(false);
            if (helpItem != null) helpItem.setVisible(false); // Aide masquée pour les non-connectés
            if (aboutItem != null) aboutItem.setVisible(true);
            if (loginItem != null) loginItem.setVisible(true);
            if (logoutItem != null) logoutItem.setVisible(false);

            // Sélectionner l'élément "Accueil" par défaut
            navigationView.setCheckedItem(R.id.nav_home);
        }

        // Forcer un rafraîchissement du NavigationView
        navigationView.invalidate();
    }

    private void updateUserButton() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Utilisateur authentifié : icône de profil et redirection vers Profil
            userButton.setImageResource(R.drawable.ic_accountp);
            userButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MainActivity.this, Profil.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("MainActivity", "Erreur lors du démarrage de l'activité ProfileActivity : " + e.getMessage());
                }
            });
        } else {
            // Utilisateur non authentifié : icône de connexion et redirection vers SignInEmail
            userButton.setImageResource(R.drawable.ic_accountp);
            userButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MainActivity.this, SignInEmail.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("MainActivity", "Erreur lors du démarrage de l'activité SignInEmail : " + e.getMessage());
                }
            });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_scan) {
            Intent intent = new Intent(MainActivity.this, uploadImagesActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_history) {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_profile) {
            Intent intent = new Intent(MainActivity.this, Profil.class);
            startActivity(intent);
        } else if (id == R.id.nav_help) {
            Intent intent = new Intent(MainActivity.this, ChatBoot.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(MainActivity.this, AboutUs.class);
            startActivity(intent);
        } else if (id == R.id.nav_login) {
            Intent intent = new Intent(MainActivity.this, SignInEmail.class);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {
            loadingOverlay.setVisibility(View.VISIBLE);
            drawerLayout.closeDrawer(GravityCompat.START);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                mAuth.signOut();
                Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
                updateNavigationMenu();
                updateUserButton();
                loadingOverlay.setVisibility(View.GONE);
            }, 500);
            return true;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
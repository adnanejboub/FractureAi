package com.fractureai;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class uploadImages extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Initialiser Firebase Auth
        try {
            mAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            Log.e("uploadImages", "Erreur lors de l'initialisation de FirebaseAuth : " + e.getMessage());
            Toast.makeText(this, "Erreur d'initialisation Firebase", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Vérifier si l'utilisateur est authentifié
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Rediriger vers l'écran de connexion si non authentifié
            Intent intent = new Intent(this, SignInEmail.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_upload_images);

        // Initialiser le DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout == null) {
            Log.e("uploadImages", "DrawerLayout n'existe pas");
            return;
        }

        // Configurer le bouton de menu pour ouvrir le drawer
        btnMenu = findViewById(R.id.btn_menu);
        if (btnMenu == null) {
            Log.e("uploadImages", "Bouton menu n'existe pas");
            return;
        }
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Configurer le NavigationView
        navigationView = findViewById(R.id.nav_view);
        if (navigationView == null) {
            Log.e("uploadImages", "NavigationView n'existe pas");
            return;
        }
        navigationView.setNavigationItemSelectedListener(this);

        // Gestion des insets pour les barres système
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Mettre à jour le menu en fonction de l'état d'authentification
        updateNavigationMenu();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Mettre à jour le menu lorsque l'activité démarre
        updateNavigationMenu();
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
        MenuItem scanItem = menu.findItem(R.id.nav_scan); // Ajout de l'élément scan

        if (currentUser != null) {
            // Utilisateur authentifié
            if (historyItem != null) historyItem.setVisible(true);
            if (profileItem != null) profileItem.setVisible(true);
            if (scanItem != null) scanItem.setVisible(true); // Rendre "Scanner" visible
            if (loginItem != null) loginItem.setVisible(false);
            if (logoutItem != null) logoutItem.setVisible(true);

            // Sélectionner l'élément "Scanner" par défaut (puisque nous sommes dans uploadImages)
            navigationView.setCheckedItem(R.id.nav_scan);
        } else {
            // Utilisateur non authentifié
            if (historyItem != null) historyItem.setVisible(false);
            if (profileItem != null) profileItem.setVisible(false);
            if (scanItem != null) scanItem.setVisible(false); // Masquer "Scanner"
            if (loginItem != null) loginItem.setVisible(true);
            if (logoutItem != null) logoutItem.setVisible(false);

            // Sélectionner l'élément "Accueil" par défaut
            navigationView.setCheckedItem(R.id.nav_home);
        }

        // Forcer un rafraîchissement du NavigationView
        navigationView.invalidate();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            Intent intent = new Intent(uploadImages.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (id == R.id.nav_scan) {
            // Déjà sur cette activité, rien à faire
            Toast.makeText(this, "Vous êtes déjà sur Scanner", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_profile) {
            Intent intent = new Intent(uploadImages.this, Profil.class);
            startActivity(intent);
        } else if (id == R.id.nav_help) {
            Toast.makeText(this, "Aide", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_about) {
            Toast.makeText(this, "À propos", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Paramètres", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_login) {
            Intent intent = new Intent(uploadImages.this, SignInEmail.class);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {
            drawerLayout.closeDrawer(GravityCompat.START);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                mAuth.signOut();
                Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
                updateNavigationMenu();
                Intent intent = new Intent(uploadImages.this, MainActivity.class);
                startActivity(intent);
                finish();
            }, 500);
            return true;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        // Fermer le drawer si ouvert, sinon appeler le comportement par défaut
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
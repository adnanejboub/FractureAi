package com.fractureai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ImageButton userButton;
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView == null) {
            Log.e("MainActivity", "NavigationView n'existe pas");
            return;
        }
        navigationView.setNavigationItemSelectedListener(this);

        // Sélectionner l'item Accueil par défaut
        navigationView.setCheckedItem(R.id.nav_home);

        // Configurer le bouton utilisateur
        userButton = findViewById(R.id.btn_user);
        if (userButton == null) {
            Log.e("MainActivity", "Bouton utilisateur n'existe pas");
            return;
        }
        userButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MainActivity.this, SignInEmail.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e("MainActivity", "Erreur lors du démarrage de l'activité SignInEmail : " + e.getMessage());
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Gérer les clics sur les éléments du menu
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            Toast.makeText(this, "Accueil", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_scan) {
            Toast.makeText(this, "Scanner", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_history) {
            Toast.makeText(this, "Historique", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_profile) {
            Toast.makeText(this, "Profil", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_forum) {
            Toast.makeText(this, "Forum", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_chat) {
            Toast.makeText(this, "Chat", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_help) {
            Toast.makeText(this, "Aide", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_about) {
            Toast.makeText(this, "À propos", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Paramètres", Toast.LENGTH_SHORT).show();
        }

        // Fermer le drawer après avoir cliqué sur un élément
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
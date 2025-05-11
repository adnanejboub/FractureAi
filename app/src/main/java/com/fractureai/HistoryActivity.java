package com.fractureai;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "HistoryActivity";
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private FrameLayout loadingOverlay;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private HistoryAdapter historyAdapter;
    private List<AnalysisHistory> historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        try {
            setContentView(R.layout.activity_history);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du chargement du layout : " + e.getMessage());
            Toast.makeText(this, "Erreur de chargement de l'interface", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, SignInEmail.class);
            startActivity(intent);
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupListeners();
        fetchHistory();
        updateNavigationMenu();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        btnMenu = findViewById(R.id.btn_menu);
        navigationView = findViewById(R.id.nav_view);
        recyclerView = findViewById(R.id.history_recycler_view);
        loadingOverlay = findViewById(R.id.loading_overlay);

        if (drawerLayout == null || btnMenu == null || navigationView == null || recyclerView == null || loadingOverlay == null) {
            Log.e(TAG, "Une ou plusieurs vues sont null");
            Toast.makeText(this, "Erreur d'initialisation de l'interface", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    private void setupRecyclerView() {
        historyList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(historyList, analysis -> {
            Intent intent = new Intent(HistoryActivity.this, AnalysisResultActivity.class);
            intent.putExtra("resultLabel", analysis.getResultLabel());
            intent.putExtra("confidence", analysis.getConfidence());
            intent.putExtra("imageBase64", analysis.getImageBase64());
            intent.putExtra("timestamp", analysis.getTimestamp());
            intent.putExtra("fractureCoordinates", (ArrayList<Map<String, Float>>) analysis.getFractureCoordinates());
            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du démarrage de AnalysisResultActivity : " + e.getMessage());
                Toast.makeText(this, "Erreur de navigation", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(historyAdapter);
    }

    private void setupListeners() {
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void fetchHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Utilisateur non connecté");
            Toast.makeText(this, "Veuillez vous connecter pour voir l'historique", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isNetworkAvailable()) {
            Log.e(TAG, "Aucune connexion réseau disponible");
            Toast.makeText(this, "Vérifiez votre connexion réseau", Toast.LENGTH_LONG).show();
            loadingOverlay.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "Récupération de l'historique pour userId: " + currentUser.getUid());
        loadingOverlay.setVisibility(View.VISIBLE);

        // Option 1: Utiliser une requête simple sans tri
        db.collection("analysis_results")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Nombre de documents récupérés: " + queryDocumentSnapshots.size());
                    historyList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Aucun document trouvé pour userId: " + currentUser.getUid());
                        Toast.makeText(this, "Aucun historique trouvé", Toast.LENGTH_SHORT).show();
                    } else {
                        List<AnalysisHistory> tempList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                AnalysisHistory history = new AnalysisHistory();
                                history.setResultLabel(document.getString("resultLabel"));

                                Number confidence = document.getDouble("confidence");
                                if (confidence == null) {
                                    confidence = document.getLong("confidence");
                                }
                                history.setConfidence(confidence != null ? confidence.floatValue() : 0.0f);

                                history.setImageBase64(document.getString("imageBase64"));
                                history.setTimestamp(document.getString("timestamp"));
                                history.setFractureCoordinates((List<Map<String, Float>>) document.get("fractureCoordinates"));
                                tempList.add(history);
                                Log.d(TAG, "Document ajouté: " + document.getId() + ", resultLabel: " + history.getResultLabel());
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur lors du parsing du document " + document.getId() + ": " + e.getMessage());
                            }
                        }

                        // Tri manuel par timestamp (ordre décroissant)
                        tempList.sort((item1, item2) -> {
                            String timestamp1 = item1.getTimestamp() != null ? item1.getTimestamp() : "";
                            String timestamp2 = item2.getTimestamp() != null ? item2.getTimestamp() : "";
                            return timestamp2.compareTo(timestamp1); // Ordre décroissant
                        });

                        // Ajouter à la liste finale après le tri
                        historyList.addAll(tempList);
                        Log.d(TAG, "Total des analyses chargées: " + historyList.size());
                    }
                    historyAdapter.notifyDataSetChanged();
                    loadingOverlay.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors de la récupération de l'historique: " + e.getMessage(), e);
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Erreur lors du chargement de l'historique: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateNavigationMenu() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (navigationView == null) {
            Log.e(TAG, "NavigationView est null");
            return;
        }
        Menu menu = navigationView.getMenu();

        MenuItem loginItem = menu.findItem(R.id.nav_login);
        MenuItem logoutItem = menu.findItem(R.id.nav_logout);
        MenuItem historyItem = menu.findItem(R.id.nav_history);
        MenuItem profileItem = menu.findItem(R.id.nav_profile);
        MenuItem scanItem = menu.findItem(R.id.nav_scan);
        MenuItem helpItem = menu.findItem(R.id.nav_help);
        MenuItem aboutItem = menu.findItem(R.id.nav_about);

        if (currentUser != null) {
            if (historyItem != null) historyItem.setVisible(true);
            if (profileItem != null) profileItem.setVisible(true);
            if (scanItem != null) scanItem.setVisible(true);
            if (helpItem != null) helpItem.setVisible(true);
            if (aboutItem != null) aboutItem.setVisible(true);
            if (loginItem != null) loginItem.setVisible(false);
            if (logoutItem != null) logoutItem.setVisible(true);
            if (historyItem != null) navigationView.setCheckedItem(R.id.nav_history);
        } else {
            if (historyItem != null) historyItem.setVisible(false);
            if (profileItem != null) profileItem.setVisible(false);
            if (scanItem != null) scanItem.setVisible(false);
            if (helpItem != null) helpItem.setVisible(true);
            if (aboutItem != null) aboutItem.setVisible(true);
            if (loginItem != null) loginItem.setVisible(true);
            if (logoutItem != null) logoutItem.setVisible(false);
            if (menu.findItem(R.id.nav_home) != null) navigationView.setCheckedItem(R.id.nav_home);
        }

        navigationView.invalidate();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        try {
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (id == R.id.nav_scan) {
                startActivity(new Intent(this, uploadImagesActivity.class));
                finish();
            } else if (id == R.id.nav_history) {
                Toast.makeText(this, "Vous êtes déjà sur l'Historique", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, Profil.class));
            } else if (id == R.id.nav_help) {
                startActivity(new Intent(this, ChatBoot.class));
            } else if (id == R.id.nav_about) {
                startActivity(new Intent(this, AboutUs.class));
            } else if (id == R.id.nav_login) {
                startActivity(new Intent(this, SignInEmail.class));
            } else if (id == R.id.nav_logout) {
                drawerLayout.closeDrawer(GravityCompat.START);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    mAuth.signOut();
                    Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
                    updateNavigationMenu();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }, 500);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur dans onNavigationItemSelected : " + e.getMessage());
            Toast.makeText(this, "Erreur lors de la navigation", Toast.LENGTH_SHORT).show();
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // Model class for analysis history
    public static class AnalysisHistory {
        private String resultLabel;
        private float confidence;
        private String imageBase64;
        private String timestamp;
        private List<Map<String, Float>> fractureCoordinates;

        public String getResultLabel() {
            return resultLabel;
        }

        public void setResultLabel(String resultLabel) {
            this.resultLabel = resultLabel;
        }

        public float getConfidence() {
            return confidence;
        }

        public void setConfidence(float confidence) {
            this.confidence = confidence;
        }

        public String getImageBase64() {
            return imageBase64;
        }

        public void setImageBase64(String imageBase64) {
            this.imageBase64 = imageBase64;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public List<Map<String, Float>> getFractureCoordinates() {
            return fractureCoordinates;
        }

        public void setFractureCoordinates(List<Map<String, Float>> fractureCoordinates) {
            this.fractureCoordinates = fractureCoordinates;
        }
    }

    // RecyclerView Adapter
    public static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<AnalysisHistory> historyList;
        private final OnHistoryClickListener clickListener;

        public interface OnHistoryClickListener {
            void onHistoryClick(AnalysisHistory analysis);
        }

        public HistoryAdapter(List<AnalysisHistory> historyList, OnHistoryClickListener clickListener) {
            this.historyList = historyList;
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AnalysisHistory history = historyList.get(position);
            holder.resultLabel.setText(history.getResultLabel() != null ? history.getResultLabel() : "Inconnu");
            holder.confidence.setText(String.format("Confiance: %.2f%%", history.getConfidence() * 100));
            holder.timestamp.setText("Date: " + (history.getTimestamp() != null ? history.getTimestamp() : "Inconnu"));
            holder.itemView.setOnClickListener(v -> clickListener.onHistoryClick(history));
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView resultLabel, confidence, timestamp;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                resultLabel = itemView.findViewById(R.id.history_result_label);
                confidence = itemView.findViewById(R.id.history_confidence);
                timestamp = itemView.findViewById(R.id.history_timestamp);
            }
        }
    }
}
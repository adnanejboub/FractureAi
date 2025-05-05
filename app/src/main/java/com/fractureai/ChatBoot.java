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
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.Timestamp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Activité principale pour le chatbot médical.
 * Gère l'interface utilisateur, les messages et l'interaction avec l'API Gemini.
 */
public class ChatBoot extends AppCompatActivity {
    private static final String TAG = "ChatBoot";
    private RecyclerView chatRecyclerView, suggestionsRecyclerView;
    private EditText messageInput;
    private ImageView sendButton, backButton;
    private ChatAdapter chatAdapter;
    private SuggestionsAdapter suggestionsAdapter;
    private List<Message> messageList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String userId;
    private OkHttpClient client;
    private String editingMessageId = null;
    private static final String GEMINI_API_KEY = "AIzaSyDlKUuqChaM_8BQIvjFBw56-oQzaGIDCqo";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private boolean isSending = false;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private static final long DEBOUNCE_DELAY = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_boot);

        // Initialisation des vues
        try {
            chatRecyclerView = findViewById(R.id.chat_recycler_view);
            suggestionsRecyclerView = findViewById(R.id.suggestions_recycler_view);
            messageInput = findViewById(R.id.message_input);
            sendButton = findViewById(R.id.send_button);
            backButton = findViewById(R.id.back_icon);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation des vues : " + e.getMessage(), e);
            Toast.makeText(this, "Erreur lors de l'initialisation de l'interface", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gestion du bouton Back
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChatBoot.this, MainActivity.class);
            startActivity(intent);
            finish(); // Ferme ChatBoot pour ne pas y revenir avec le bouton retour système
        });

        // Initialisation de Firebase
        try {
            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation de Firebase : " + e.getMessage(), e);
            Toast.makeText(this, "Erreur lors de l'initialisation de Firebase", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialisation de l'adaptateur de chat
        try {
            messageList = new ArrayList<>();
            chatAdapter = new ChatAdapter(this, messageList, (messageId, messageText) -> {
                editingMessageId = messageId;
                messageInput.setText(messageText);
                Toast.makeText(this, "Modifiez le message et envoyez", Toast.LENGTH_SHORT).show();
            });
            chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            chatRecyclerView.setAdapter(chatAdapter);

            // Ajouter un message de bienvenue
            String userName = auth.getCurrentUser() != null && auth.getCurrentUser().getDisplayName() != null
                    ? auth.getCurrentUser().getDisplayName()
                    : "Utilisateur";
            Message welcomeMessage = new Message("Bot", "Bonjour " + userName + ", je suis un médecin virtuel avec 30 ans d'expertise. Posez-moi vos questions sur les maladies, fractures, traitements, nutrition ou entraînements.", false, Timestamp.now());
            messageList.add(welcomeMessage);
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            chatRecyclerView.scrollToPosition(messageList.size() - 1);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation de l'adaptateur de chat ou du message de bienvenue : " + e.getMessage(), e);
            Toast.makeText(this, "Erreur lors de l'initialisation du chat", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialisation de l'adaptateur de suggestions
        try {
            List<String> suggestions = Arrays.asList(
                    "Que faire pour une fracture du poignet ?",
                    "Symptômes d'une grippe saisonnière",
                    "Plan alimentaire pour renforcer les os",
                    "Exercices pour récupérer après une entorse"
            );
            suggestionsAdapter = new SuggestionsAdapter(suggestions, suggestion -> {
                messageInput.setText(suggestion);
                sendMessage();
            });
            suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            suggestionsRecyclerView.setAdapter(suggestionsAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation de l'adaptateur de suggestions : " + e.getMessage(), e);
            Toast.makeText(this, "Erreur lors de l'initialisation des suggestions", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialisation du client OkHttp
        try {
            client = new OkHttpClient();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation d'OkHttp : " + e.getMessage(), e);
            Toast.makeText(this, "Erreur lors de l'initialisation du client réseau", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gestion du bouton d'envoi avec débouncing
        sendButton.setOnClickListener(v -> {
            if (!isSending) {
                isSending = true;
                debounceHandler.postDelayed(() -> {
                    sendMessage();
                    isSending = false;
                }, DEBOUNCE_DELAY);
            }
        });

        // Écoute des messages Firestore
        listenToMessages();
    }

    /**
     * Vérifie si la connexion réseau est disponible.
     */
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification de la connectivité : " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envoie un message saisi par l'utilisateur ou met à jour un message existant, et affiche un indicateur de saisie.
     */
    private void sendMessage() {
        if (userId == null) {
            Toast.makeText(this, "Vous devez être connecté pour envoyer un message.", Toast.LENGTH_SHORT).show();
            sendButton.setEnabled(true);
            return;
        }

        String text;
        try {
            text = messageInput.getText().toString().trim();
        } catch (NullPointerException e) {
            Log.e(TAG, "Erreur : Champ de message invalide : " + e.getMessage(), e);
            Toast.makeText(this, "Erreur : Champ de message invalide.", Toast.LENGTH_SHORT).show();
            sendButton.setEnabled(true);
            return;
        }

        if (text.isEmpty()) {
            sendButton.setEnabled(true);
            return;
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Pas de connexion réseau. Vérifiez votre connexion.", Toast.LENGTH_SHORT).show();
            sendButton.setEnabled(true);
            return;
        }

        try {
            sendButton.setEnabled(false);
            if (editingMessageId != null) {
                // Mettre à jour un message existant
                db.collection("conversations").document(userId).collection("messages")
                        .document(editingMessageId)
                        .update("text", text, "timestamp", Timestamp.now())
                        .addOnSuccessListener(aVoid -> {
                            runOnUiThread(() -> {
                                messageInput.setText("");
                                editingMessageId = null;
                                Log.d(TAG, "Message modifié avec succès : " + text);
                                // Mettre à jour messageList localement
                                for (Message msg : messageList) {
                                    if (msg.getId() != null && msg.getId().equals(editingMessageId)) {
                                        msg.setText(text);
                                        msg.setTimestamp(Timestamp.now());
                                        chatAdapter.notifyDataSetChanged();
                                        break;
                                    }
                                }
                                sendButton.setEnabled(true);
                            });
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> {
                                String errorMsg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
                                Toast.makeText(this, "Erreur lors de la modification du message : " + errorMsg, Toast.LENGTH_SHORT).show();
                                sendButton.setEnabled(true);
                            });
                        });
            } else {
                // Ajouter un nouveau message
                Message message = new Message(userId, text, true, Timestamp.now());
                db.collection("conversations").document(userId).collection("messages")
                        .add(message)
                        .addOnSuccessListener(documentReference -> {
                            runOnUiThread(() -> {
                                messageInput.setText("");
                                Log.d(TAG, "Message envoyé avec succès : " + text);
                                // Ajouter l'indicateur de saisie
                                Message typingMessage = new Message("Bot", "en train d'écrire ...", false, Timestamp.now());
                                messageList.add(typingMessage);
                                chatAdapter.notifyItemInserted(messageList.size() - 1);
                                chatRecyclerView.scrollToPosition(messageList.size() - 1);
                            });
                            documentReference.get().addOnSuccessListener(documentSnapshot -> {
                                Message savedMessage = documentSnapshot.toObject(Message.class);
                                if (savedMessage != null) {
                                    savedMessage.setId(documentSnapshot.getId());
                                }
                            });
                            callGeminiApi(text);
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> {
                                String errorMsg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
                                Toast.makeText(this, "Erreur lors de l'envoi du message : " + errorMsg, Toast.LENGTH_SHORT).show();
                                sendButton.setEnabled(true);
                            });
                        });
            }
        } catch (Exception e) {
            runOnUiThread(() -> {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
                Toast.makeText(this, "Erreur inattendue lors de l'envoi du message : " + errorMsg, Toast.LENGTH_SHORT).show();
                sendButton.setEnabled(true);
            });
        }
    }

    /**
     * Appelle l'API Gemini pour obtenir une réponse à une question médicale.
     */
    private void callGeminiApi(String text) {
        if (!isNetworkAvailable()) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Pas de connexion réseau. Vérifiez votre connexion.", Toast.LENGTH_SHORT).show();
                sendButton.setEnabled(true);
                removeTypingIndicator();
            });
            return;
        }

        try {
            // Préparer la charge utile JSON pour une entrée textuelle
            JSONObject json = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject textPart = new JSONObject();
            textPart.put("text", "Vous êtes un médecin avec 30 ans d'expérience dans toutes les spécialités médicales (orthopédie, médecine générale, nutrition, rééducation, etc.). Répondez à la question suivante de manière concise, claire et professionnelle, comme un expert médical. Évitez les mentions de résumé ou de manipulation. Fournissez des conseils précis sur les maladies, fractures, traitements, médicaments, nutrition ou entraînements, selon la question : " + text);
            partsArray.put(textPart);
            content.put("parts", partsArray);
            contentsArray.put(content);
            json.put("contents", contentsArray);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    json.toString()
            );

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL + "?key=" + GEMINI_API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        String errorMsg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
                        Toast.makeText(ChatBoot.this, "Erreur de communication avec l'API Gemini : " + errorMsg, Toast.LENGTH_SHORT).show();
                        sendButton.setEnabled(true);
                        removeTypingIndicator();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody;
                        try {
                            responseBody = response.body().string();
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                String errorMsg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
                                Toast.makeText(ChatBoot.this, "Erreur de lecture de la réponse API : " + errorMsg, Toast.LENGTH_SHORT).show();
                                sendButton.setEnabled(true);
                                removeTypingIndicator();
                            });
                            return;
                        }

                        JSONObject jsonResponse;
                        try {
                            jsonResponse = new JSONObject(responseBody);
                            String botResponse = jsonResponse.getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");

                            // Enregistrer la réponse du bot dans Firestore
                            Message botMessage = new Message("Bot", botResponse, false, Timestamp.now());
                            db.collection("conversations").document(userId).collection("messages")
                                    .add(botMessage)
                                    .addOnSuccessListener(doc -> {
                                        runOnUiThread(() -> {
                                            sendButton.setEnabled(true);
                                            removeTypingIndicator();
                                            Log.d(TAG, "Réponse du bot enregistrée avec succès");
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        runOnUiThread(() -> {
                                            String errorMsg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
                                            Toast.makeText(ChatBoot.this, "Erreur lors de l'enregistrement de la réponse du bot : " + errorMsg, Toast.LENGTH_SHORT).show();
                                            sendButton.setEnabled(true);
                                            removeTypingIndicator();
                                        });
                                    });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                String errorMsg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
                                Toast.makeText(ChatBoot.this, "Erreur d'analyse de la réponse API : " + errorMsg, Toast.LENGTH_SHORT).show();
                                sendButton.setEnabled(true);
                                removeTypingIndicator();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            String errorMsg = response.message() != null ? response.message() : "Erreur inconnue";
                            Toast.makeText(ChatBoot.this, "Erreur API Gemini : " + errorMsg, Toast.LENGTH_SHORT).show();
                            sendButton.setEnabled(true);
                            removeTypingIndicator();
                        });
                    }
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
                Toast.makeText(ChatBoot.this, "Erreur inattendue lors de l'appel API : " + errorMsg, Toast.LENGTH_SHORT).show();
                sendButton.setEnabled(true);
                removeTypingIndicator();
            });
        }
    }

    /**
     * Supprime l'indicateur de saisie de la liste des messages.
     */
    private void removeTypingIndicator() {
        for (int i = messageList.size() - 1; i >= 0; i--) {
            if ("en train d'écrire ...".equals(messageList.get(i).getText())) {
                messageList.remove(i);
                chatAdapter.notifyItemRemoved(i);
                break;
            }
        }
    }

    /**
     * Écoute les messages dans Firestore et met à jour l'interface.
     */
    private void listenToMessages() {
        if (userId == null) {
            Toast.makeText(this, "Utilisateur non connecté. Impossible de charger les messages.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Utilisateur non connecté pour l'écoute des messages");
            return;
        }
        try {
            db.collection("conversations").document(userId).collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            runOnUiThread(() -> {
                                String errorMsg = error.getMessage() != null ? error.getMessage() : "Erreur inconnue";
                                Toast.makeText(this, "Erreur de chargement des messages : " + errorMsg, Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Erreur d'écoute Firestore : " + errorMsg, error);
                            });
                            return;
                        }
                        if (value != null) {
                            for (DocumentChange dc : value.getDocumentChanges()) {
                                if (dc.getType() == DocumentChange.Type.ADDED) {
                                    try {
                                        Message message = dc.getDocument().toObject(Message.class);
                                        message.setId(dc.getDocument().getId());
                                        // Ignorer l'indicateur de saisie lors de l'ajout
                                        if (!"en train d'écrire ...".equals(message.getText())) {
                                            messageList.add(message);
                                            chatAdapter.notifyItemInserted(messageList.size() - 1);
                                            chatRecyclerView.scrollToPosition(messageList.size() - 1);
                                            Log.d(TAG, "Nouveau message ajouté : " + message.getText());
                                        }
                                    } catch (Exception e) {
                                        runOnUiThread(() -> {
                                            String errorMsg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
                                            Toast.makeText(this, "Erreur lors de l'ajout du message : " + errorMsg, Toast.LENGTH_SHORT).show();
                                            Log.e(TAG, "Erreur lors de l'ajout du message : " + errorMsg, e);
                                        });
                                    }
                                } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                    try {
                                        Message updatedMessage = dc.getDocument().toObject(Message.class);
                                        updatedMessage.setId(dc.getDocument().getId());
                                        for (int i = 0; i < messageList.size(); i++) {
                                            if (messageList.get(i).getId() != null && messageList.get(i).getId().equals(updatedMessage.getId())) {
                                                messageList.set(i, updatedMessage);
                                                chatAdapter.notifyItemChanged(i);
                                                break;
                                            }
                                        }
                                        Log.d(TAG, "Message modifié : " + updatedMessage.getText());
                                    } catch (Exception e) {
                                        runOnUiThread(() -> {
                                            String errorMsg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
                                            Toast.makeText(this, "Erreur lors de la mise à jour du message : " + errorMsg, Toast.LENGTH_SHORT).show();
                                            Log.e(TAG, "Erreur lors de la mise à jour du message : " + errorMsg, e);
                                        });
                                    }
                                }
                            }
                        }
                    });
        } catch (Exception e) {
            runOnUiThread(() -> {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
                Toast.makeText(this, "Erreur inattendue lors de l'écoute des messages : " + errorMsg, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Erreur inattendue d'écoute : " + errorMsg, e);
            });
        }
    }

    /**
     * Adaptateur pour les suggestions de questions.
     */
    private static class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.ViewHolder> {
        private final List<String> suggestions;
        private final OnSuggestionClickListener listener;

        interface OnSuggestionClickListener {
            void onSuggestionClick(String suggestion);
        }

        SuggestionsAdapter(List<String> suggestions, OnSuggestionClickListener listener) {
            this.suggestions = suggestions;
            this.listener = listener;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String suggestion = suggestions.get(position);
            holder.textView.setText(suggestion);
            holder.textView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.purple));
            holder.textView.setBackgroundResource(R.drawable.light_grey_background);
            holder.textView.setPadding(32, 16, 32, 16);
            holder.textView.setTextSize(14);
            holder.itemView.setOnClickListener(v -> listener.onSuggestionClick(suggestion));
        }

        @Override
        public int getItemCount() {
            return suggestions.size();
        }
    }
}
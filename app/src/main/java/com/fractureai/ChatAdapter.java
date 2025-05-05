package com.fractureai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.util.List;

/**
 * Adaptateur pour afficher les messages dans le RecyclerView du chat.
 * Gère les messages de l'utilisateur et du bot avec des mises en page différentes.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;
    private final Context context;
    private final List<Message> messageList;
    private final OnEditMessageListener editMessageListener;

    public interface OnEditMessageListener {
        void onEditMessage(String messageId, String messageText);
    }

    public ChatAdapter(Context context, List<Message> messageList, OnEditMessageListener editMessageListener) {
        this.context = context;
        this.messageList = messageList;
        this.editMessageListener = editMessageListener;
    }

    /**
     * Détermine le type de vue (utilisateur ou bot) pour chaque message.
     */
    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    /**
     * Crée le ViewHolder approprié selon le type de vue.
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_user_message, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_bot_message, parent, false);
            return new BotMessageViewHolder(view);
        }
    }

    /**
     * Lie les données du message au ViewHolder.
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else {
            ((BotMessageViewHolder) holder).bind(message);
        }
    }

    /**
     * Retourne le nombre total de messages.
     */
    @Override
    public int getItemCount() {
        return messageList.size();
    }

    /**
     * ViewHolder pour les messages de l'utilisateur.
     */
    class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timestampText;
        ImageView copyButton, editButton;
        LinearLayout actionButtons;

        UserMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timestampText = itemView.findViewById(R.id.timestamp_text);
            copyButton = itemView.findViewById(R.id.copy_button);
            editButton = itemView.findViewById(R.id.edit_button);
            actionButtons = itemView.findViewById(R.id.action_buttons);
        }

        void bind(Message message) {
            messageText.setText(message.getText());
            timestampText.setText(formatTimestamp(message.getTimestamp()));

            // Afficher les boutons d'action au clic long
            itemView.setOnLongClickListener(v -> {
                actionButtons.setVisibility(View.VISIBLE);
                return true;
            });

            // Action de copie
            copyButton.setOnClickListener(v -> {
                copyToClipboard(message.getText());
                actionButtons.setVisibility(View.GONE);
            });

            // Action de modification
            editButton.setOnClickListener(v -> {
                editMessageListener.onEditMessage(message.getId(), message.getText());
                actionButtons.setVisibility(View.GONE);
            });
        }
    }

    /**
     * ViewHolder pour les messages du bot.
     * Gère l'affichage spécial pour l'indicateur de saisie.
     */
    class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timestampText;
        ImageView copyButton;
        LinearLayout actionButtons;

        BotMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timestampText = itemView.findViewById(R.id.timestamp_text);
            copyButton = itemView.findViewById(R.id.copy_button);
            actionButtons = itemView.findViewById(R.id.action_buttons);
        }

        void bind(Message message) {
            messageText.setText(message.getText());
            timestampText.setText(formatTimestamp(message.getTimestamp()));

            // Afficher les boutons d'action au clic long
            itemView.setOnLongClickListener(v -> {
                actionButtons.setVisibility(View.VISIBLE);
                return true;
            });

            // Cacher le bouton de copie pour l'indicateur de saisie
            if ("en train d'écrire ...".equals(message.getText())) {
                copyButton.setVisibility(View.GONE);
                actionButtons.setVisibility(View.GONE);
                messageText.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                messageText.setTypeface(null, android.graphics.Typeface.ITALIC);
            } else {
                copyButton.setVisibility(View.VISIBLE);
                messageText.setTextColor(context.getResources().getColor(android.R.color.black));
                messageText.setTypeface(null, android.graphics.Typeface.NORMAL);
                copyButton.setOnClickListener(v -> {
                    copyToClipboard(message.getText());
                    actionButtons.setVisibility(View.GONE);
                });
            }
        }
    }

    /**
     * Formate le timestamp en une chaîne lisible (format : jj/mm/aaaa HH:mm).
     */
    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "Date inconnue";
        }
        try {
            return DateFormat.format("dd/MM/yyyy HH:mm", timestamp.toDate()).toString();
        } catch (Exception e) {
            return "Erreur de formatage";
        }
    }

    /**
     * Copie le texte du message dans le presse-papiers.
     */
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Message", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Message copié dans le presse-papiers", Toast.LENGTH_SHORT).show();
    }
}
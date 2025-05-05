package com.fractureai;

import com.google.firebase.Timestamp;

public class Message {
    private String id; // ID du document Firestore
    private String userId;
    private String text;
    private boolean sentByUser;
    private Timestamp timestamp;

    // Constructeur par défaut requis pour Firestore
    public Message() {
    }

    public Message(String userId, String text, boolean sentByUser, Timestamp timestamp) {
        this.userId = userId;
        this.text = text;
        this.sentByUser = sentByUser;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isSentByUser() {
        return sentByUser;
    }

    public boolean isUser() {
        return sentByUser;
    }

    public void setSentByUser(boolean sentByUser) {
        this.sentByUser = sentByUser;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
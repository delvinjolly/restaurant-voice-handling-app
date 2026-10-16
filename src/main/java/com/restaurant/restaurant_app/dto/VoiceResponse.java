package com.restaurant.restaurant_app.dto;

public class VoiceResponse {

    private String intent;
    private String message;

    public VoiceResponse(String intent, String message) {
        this.intent = intent;
        this.message = message;
    }

    public String getIntent() {
        return intent;
    }

    public String getMessage() {
        return message;
    }
}
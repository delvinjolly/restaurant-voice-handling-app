package com.restaurant.restaurant_app.ai;

public class VoiceResponse {

    private String intent;
    private String message;

    public VoiceResponse() {
    }

    public VoiceResponse(String intent, String message) {
        this.intent = intent;
        this.message = message;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
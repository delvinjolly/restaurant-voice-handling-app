package com.restaurant.restaurant_app.controller;

import com.restaurant.restaurant_app.dto.VoiceRequest;
import com.restaurant.restaurant_app.dto.VoiceResponse;
import com.restaurant.restaurant_app.service.VoiceService;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class VoiceController {

    private final VoiceService voiceService;

    public VoiceController(VoiceService voiceService) {
        this.voiceService = voiceService;
    }

    @PostMapping("/voice")
    public VoiceResponse handleVoice(
            @Valid @RequestBody VoiceRequest request) {

        return voiceService.handleVoice(request);
    }
}
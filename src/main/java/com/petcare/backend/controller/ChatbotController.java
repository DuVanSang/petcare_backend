package com.petcare.backend.controller;

import com.petcare.backend.dto.chatbot.ChatRequest;
import com.petcare.backend.dto.chatbot.ChatResponse;
import com.petcare.backend.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatbotService.processChat(request));
    }
}

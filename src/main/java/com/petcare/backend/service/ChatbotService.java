package com.petcare.backend.service;

import com.petcare.backend.dto.chatbot.ChatRequest;
import com.petcare.backend.dto.chatbot.ChatResponse;

public interface ChatbotService {
    ChatResponse processChat(ChatRequest request);
}

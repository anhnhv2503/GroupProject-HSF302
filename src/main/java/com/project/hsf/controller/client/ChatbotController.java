package com.project.hsf.controller.client;

import com.project.hsf.dto.ChatbotRequest;
import com.project.hsf.dto.ChatbotResponse;
import com.project.hsf.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/send")
    @ResponseBody
    public ChatbotResponse handleChat(@RequestBody ChatbotRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return new ChatbotResponse("Vui lòng nhập tin nhắn!");
        }
        String reply = chatbotService.getReply(request.getMessage());
        return new ChatbotResponse(reply);
    }
}

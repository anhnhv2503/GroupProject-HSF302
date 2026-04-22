package com.project.hsf.service.impl;

import com.project.hsf.entity.KnowledgeDocument;
import com.project.hsf.repository.KnowledgeDocumentRepository;
import com.project.hsf.service.ChatbotService;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatbotServiceImpl implements ChatbotService {

    private final KnowledgeDocumentRepository repository;
    private final GoogleAiGeminiChatModel model;

    public ChatbotServiceImpl(KnowledgeDocumentRepository repository, 
                              @Value("${gemini.api.key}") String apiKey) {
        this.repository = repository;
        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.3)
                .build();
    }

    @Override
    public String getReply(String userMessage) {
        try {
            // RAG: Search context from database
            List<KnowledgeDocument> docs = repository.searchByKeywords(userMessage);
            String context = docs.stream()
                    .map(d -> d.getTitle() + ": " + d.getContent())
                    .collect(Collectors.joining("\n\n"));

            String systemPrompt = "Bạn là trợ lý ảo 'Oceanic AI' của cửa hàng hải sản Culinary Curator. " +
                    "Hãy trả lời khách hàng một cách lịch sự, chuyên nghiệp bằng tiếng Việt. " +
                    "Sử dụng thông tin dưới đây để trả lời nếu có liên quan:\n\n" + context +
                    "\n\nNếu không có thông tin trong ngữ cảnh, hãy trả lời dựa trên kiến thức chung về hải sản của bạn. " +
                    "Nếu người dùng hỏi về giá, hãy nhắc họ xem bảng giá trên website.";

            ChatResponse response = model.chat(new UserMessage(systemPrompt + "\n\nUser: " + userMessage));
            return response.aiMessage().text();
        } catch (Exception e) {
            e.printStackTrace();
            return "Xin lỗi, hệ thống AI đang gặp sự cố. Vui lòng thử lại sau hoặc liên hệ hotline!";
        }
    }
}

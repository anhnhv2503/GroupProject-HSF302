package com.project.hsf.service.impl;

import com.project.hsf.entity.KnowledgeDocument;
import com.project.hsf.repository.KnowledgeDocumentRepository;
import com.project.hsf.service.ChatbotService;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    /**
     * Reply used when Gemini cannot be reached. A third-party failure must not break the purchase
     * flow: customers still need to browse products and place orders normally.
     */
    private static final String FALLBACK_REPLY =
            "Xin lỗi, trợ lý ảo đang tạm thời không phản hồi. "
                    + "Bạn có thể xem thông tin sản phẩm trực tiếp trên website "
                    + "hoặc liên hệ hotline để được tư vấn ngay.";

    /** Max documents stuffed into the prompt - more only dilutes context and burns tokens. */
    private static final int MAX_CONTEXT_DOCS = 3;

    /** Tokens shorter than this are meaningless to search on. */
    private static final int MIN_KEYWORD_LENGTH = 3;

    /** Words common in questions but useless for finding documents. */
    private static final Set<String> STOP_WORDS = Set.of(
            "cho", "toi", "tôi", "minh", "mình", "ban", "bạn", "shop", "cua", "của",
            "gia", "giá", "nhe", "nhé", "khong", "không", "duoc", "được", "the", "thế",
            "nao", "nào", "gi", "gì", "co", "có", "va", "và", "voi", "với", "hoi", "hỏi",
            "muon", "muốn", "can", "cần", "xin", "vui", "long", "lòng", "lam", "làm");

    private final KnowledgeDocumentRepository repository;
    private final GoogleAiGeminiChatModel model;

    public ChatbotServiceImpl(KnowledgeDocumentRepository repository,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.model-name:gemini-2.5-flash}") String modelName,
            @Value("${gemini.timeout-seconds:15}") long timeoutSeconds,
            @Value("${gemini.max-retries:1}") int maxRetries) {
        this.repository = repository;
        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.3)
                // Without a timeout, one hanging request holds a Tomcat thread indefinitely.
                .timeout(Duration.ofSeconds(timeoutSeconds))
                // Retry once for transient network errors. More would make the customer wait longer
                // and burn quota faster exactly when Gemini is already overloaded.
                .maxRetries(maxRetries)
                .maxOutputTokens(1024)
                .build();
    }

    @Override
    public String getReply(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "Bạn cần tư vấn gì về hải sản ạ?";
        }

        String context = buildContext(userMessage);

        try {
            ChatResponse response = model.chat(new UserMessage(buildPrompt(context, userMessage)));
            return response.aiMessage().text();
        } catch (Exception e) {
            // Log the message instead of printStackTrace so it lands in the log file once deployed.
            log.warn("Gemini call failed, returning fallback reply: {}", e.getMessage());
            return FALLBACK_REPLY;
        }
    }

    /**
     * Pulls relevant documents out of the knowledge base.
     *
     * The whole user sentence used to be passed straight into LIKE %...%, which meant
     * "do you still have king crab" had to match verbatim in the database to find anything -
     * so it almost always returned nothing. The sentence is now split into keywords first.
     */
    private String buildContext(String userMessage) {
        // LinkedHashMap keeps discovery order and drops duplicates when several keywords hit the
        // same document.
        Map<Long, KnowledgeDocument> found = new LinkedHashMap<>();

        for (String keyword : extractKeywords(userMessage)) {
            if (found.size() >= MAX_CONTEXT_DOCS) {
                break;
            }
            try {
                for (KnowledgeDocument doc : repository.searchByKeywords(keyword)) {
                    found.putIfAbsent(doc.getId(), doc);
                }
            } catch (Exception e) {
                log.warn("Knowledge document lookup failed for keyword '{}': {}", keyword, e.getMessage());
            }
        }

        StringBuilder context = new StringBuilder();
        found.values().stream().limit(MAX_CONTEXT_DOCS).forEach(
                doc -> context.append(doc.getTitle()).append(": ").append(doc.getContent()).append("\n\n"));
        return context.toString();
    }

    private List<String> extractKeywords(String userMessage) {
        List<String> keywords = new ArrayList<>();
        // Split on anything that is not a letter or digit, which preserves accented Vietnamese.
        for (String token : userMessage.toLowerCase().split("[^\\p{L}\\p{N}]+")) {
            if (token.length() >= MIN_KEYWORD_LENGTH && !STOP_WORDS.contains(token)
                    && !keywords.contains(token)) {
                keywords.add(token);
            }
        }
        // Longer tokens tend to be the distinctive nouns (product names), so search those first.
        keywords.sort((a, b) -> Integer.compare(b.length(), a.length()));
        return keywords;
    }

    /**
     * Constrains the bot to the knowledge base.
     *
     * Prices and stock levels change constantly and are not in the knowledge base, so the bot is
     * forbidden from stating concrete numbers for either - quoting a wrong price to a customer is a
     * real business error, not just a display bug.
     */
    private String buildPrompt(String context, String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là trợ lý ảo 'Oceanic AI' của cửa hàng hải sản Culinary Curator. ")
                .append("Trả lời khách hàng lịch sự, ngắn gọn, bằng tiếng Việt.\n\n");

        if (context.isBlank()) {
            prompt.append("Không tìm thấy thông tin nội bộ nào liên quan đến câu hỏi này.\n");
        } else {
            prompt.append("Thông tin nội bộ của cửa hàng:\n").append(context);
        }

        prompt.append("\nQuy tắc bắt buộc:\n")
                .append("- Chỉ dựa vào thông tin nội bộ ở trên để nói về sản phẩm của cửa hàng.\n")
                .append("- TUYỆT ĐỐI không tự nêu giá tiền hay số lượng tồn kho cụ thể. ")
                .append("Nếu khách hỏi giá hoặc còn hàng không, hãy hướng khách xem trên trang sản phẩm.\n")
                .append("- Nếu không có thông tin để trả lời, nói thẳng là chưa có thông tin ")
                .append("và đề nghị khách liên hệ hotline, không được suy đoán.\n\n")
                .append("Câu hỏi của khách: ").append(userMessage);

        return prompt.toString();
    }
}

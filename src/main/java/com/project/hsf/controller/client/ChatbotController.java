package com.project.hsf.controller.client;

import com.project.hsf.dto.ChatbotRequest;
import com.project.hsf.dto.ChatbotResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    @PostMapping("/send")
    public ChatbotResponse handleChat(@RequestBody ChatbotRequest request) {
        String userMessage = request.getMessage().toLowerCase();
        String reply;

        if (userMessage.contains("giá") || userMessage.contains("chi phí")) {
            reply = "Giá của các loại hải sản cao cấp luôn được cập nhật hàng ngày trên trang sản phẩm. Bạn có thể xem chi tiết tại mục Sản phẩm nhé!";
        } else if (userMessage.contains("vận chuyển") || userMessage.contains("ship")) {
            reply = "Chúng tôi hỗ trợ giao hàng hỏa tốc trong nội thành và vận chuyển chuyên dụng cho các tỉnh lân cận để đảm bảo độ tươi ngon nhất.";
        } else if (userMessage.contains("chào") || userMessage.contains("hi") || userMessage.contains("hello")) {
            reply = "Xin chào! Tôi là trợ lý AI của Culinary Curator. Rất vui được hỗ trợ bạn tìm kiếm những loại hải sản tươi ngon nhất!";
        } else if (userMessage.contains("tươi") || userMessage.contains("sống")) {
            reply = "Tất cả hải sản tại Culinary Curator đều được tuyển chọn kỹ lưỡng và bảo quản trong môi trường tiêu chuẩn, cam kết tươi sống 100% khi đến tay khách hàng.";
        } else {
            reply = "Cảm ơn bạn đã quan tâm! Hiện tại tôi đang được huấn luyện thêm nhiều kiến thức. Bạn có thể để lại số điện thoại hoặc câu hỏi chi tiết hơn để nhân viên tư vấn gọi lại hỗ trợ nhé.";
        }

        return new ChatbotResponse(reply);
    }
}

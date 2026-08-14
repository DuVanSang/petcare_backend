package com.petcare.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcare.backend.dto.chatbot.ChatMessageDto;
import com.petcare.backend.dto.chatbot.ChatRequest;
import com.petcare.backend.dto.chatbot.ChatResponse;
import com.petcare.backend.service.ChatbotService;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String geminiApiKey;

    private String systemInstructionText = "";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("petcare_system_instruction.txt");
            try (InputStream inputStream = resource.getInputStream()) {
                systemInstructionText = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
                log.info("Successfully loaded PetCare System Instruction ({} chars)", systemInstructionText.length());
            }
        } catch (Exception e) {
            log.error("Failed to load petcare_system_instruction.txt from classpath", e);
            systemInstructionText = "Your name is PetCare AI. Expert Veterinary & App Support Assistant for PetCare Application.";
        }
        log.info("Gemini API Key configured: {}", StringUtils.hasText(geminiApiKey) ? ("YES (prefix: " + geminiApiKey.substring(0, Math.min(6, geminiApiKey.length())) + "...)") : "NO (EMPTY)");
    }

    @Override
    public ChatResponse processChat(ChatRequest request) {
        String userMessage = request.getMessage();
        if (!StringUtils.hasText(userMessage)) {
            return ChatResponse.builder().reply("Xin chào! Bạn cần PetCare AI hỗ trợ gì cho bé cưng hôm nay?").build();
        }

        // 1. Check boundary rules (Non-pet / non-app questions)
        if (isOutOfScopeQuestion(userMessage)) {
            return ChatResponse.builder()
                    .reply("Tôi chỉ có thể hỗ trợ các vấn đề về chăm sóc thú cưng và ứng dụng PetCare thôi nha!")
                    .build();
        }

        // 2. Call Gemini API if Key is present
        if (StringUtils.hasText(geminiApiKey)) {
            try {
                String aiReply = callGeminiApi(request);
                if (StringUtils.hasText(aiReply)) {
                    return ChatResponse.builder().reply(aiReply).build();
                }
            } catch (Exception ex) {
                log.error("Gemini API call failed, falling back to Knowledge Engine: {}", ex.getMessage(), ex);
            }
        } else {
            log.warn("Gemini API Key is empty! Using fallback knowledge engine.");
        }

        // 3. Fallback Knowledge Engine (Responds directly from Knowledge Base)
        String fallbackReply = generateFallbackKnowledgeResponse(userMessage);
        return ChatResponse.builder().reply(fallbackReply).build();
    }

    private boolean isOutOfScopeQuestion(String text) {
        String lower = text.toLowerCase();
        String[] outOfScopeKeywords = {
            "viết code", "lập trình", "java spring", "react native", "lịch sử", "chính trị",
            "thời tiết hôm nay", "giá vàng", "bóng đá", "xem phim", "đánh giá phim", "giải toán"
        };
        for (String kw : outOfScopeKeywords) {
            if (lower.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static final String[] GEMINI_MODELS = {
        "gemini-3-flash-preview",
        "gemini-3.7-flash",
        "gemini-flash-latest"
    };

    private String callGeminiApi(ChatRequest request) {
        Map<String, Object> requestBody = new HashMap<>();

        // System Instruction
        Map<String, Object> sysInstruction = new HashMap<>();
        List<Map<String, String>> sysParts = new ArrayList<>();
        Map<String, String> sysPart = new HashMap<>();
        sysPart.put("text", systemInstructionText);
        sysParts.add(sysPart);
        sysInstruction.put("parts", sysParts);
        requestBody.put("system_instruction", sysInstruction);

        // Contents (History + User Message)
        List<Map<String, Object>> contents = new ArrayList<>();
        if (request.getHistory() != null) {
            for (ChatMessageDto msg : request.getHistory()) {
                Map<String, Object> contentItem = new HashMap<>();
                contentItem.put("role", "user".equalsIgnoreCase(msg.getRole()) ? "user" : "model");
                List<Map<String, String>> parts = new ArrayList<>();
                Map<String, String> p = new HashMap<>();
                p.put("text", msg.getContent());
                parts.add(p);
                contentItem.put("parts", parts);
                contents.add(contentItem);
            }
        }

        // Current User Message
        Map<String, Object> userContentItem = new HashMap<>();
        userContentItem.put("role", "user");
        List<Map<String, String>> userParts = new ArrayList<>();
        Map<String, String> p = new HashMap<>();
        p.put("text", request.getMessage());
        userParts.add(p);
        userContentItem.put("parts", userParts);
        contents.add(userContentItem);

        requestBody.put("contents", contents);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-goog-api-key", geminiApiKey.trim());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        for (String model : GEMINI_MODELS) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
                log.info("Calling Gemini model: {}", model);

                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode textNode = root.path("candidates").get(0).path("content").path("parts").get(0).path("text");
                    if (!textNode.isMissingNode() && StringUtils.hasText(textNode.asText())) {
                        log.info("Gemini model {} responded successfully!", model);
                        return textNode.asText();
                    }
                }
            } catch (Exception ex) {
                log.warn("Gemini model {} failed ({}), trying next candidate...", model, ex.getMessage());
            }
        }

        return null;
    }

    private String generateFallbackKnowledgeResponse(String query) {
        String q = query.toLowerCase();

        // === APP USAGE / HƯỚNG DẪN SỬ DỤNG ===
        if (q.contains("sử dụng") || q.contains("hướng dẫn") || q.contains("cách dùng") || q.contains("dùng app") || q.contains("tính năng") || q.contains("chức năng")) {
            return "📱 **Hướng dẫn Sử dụng Ứng dụng PetCare:**\n\n" +
                    "**1. Quản lý Hồ sơ Thú cưng:**\n" +
                    "• Vào tab **Thú cưng** ➔ Bấm nút **+** để thêm bé cưng mới.\n" +
                    "• Điền đầy đủ thông tin: Tên, Loài, Giống, Ngày sinh, Cân nặng, Giới tính.\n" +
                    "• Bấm vào hồ sơ bé để xem chi tiết: Vắc-xin, Cân nặng, Timeline.\n\n" +
                    "**2. Lịch tiêm phòng (2 chế độ):**\n" +
                    "• **Tự động:** App tự tính lịch tiêm chuẩn thú y theo tuổi bé.\n" +
                    "• **Thủ công:** Bạn tự thêm/sửa/xóa mũi tiêm theo ý muốn.\n" +
                    "• Chuyển chế độ bằng thanh gạt trong tab Vắc-xin của bé.\n\n" +
                    "**3. Nhắc nhở Chăm sóc:**\n" +
                    "• Vào tab **Nhắc nhở** ➔ Tạo lịch nhắc: Tẩy giun, Tắm rửa, Khám thú y, Uống thuốc.\n" +
                    "• App sẽ tự gửi thông báo đẩy đến điện thoại khi đến ngày.\n\n" +
                    "**4. Cộng đồng PetCare:**\n" +
                    "• Vào tab **Cộng đồng** ➔ Đăng ảnh, chia sẻ khoảnh khắc, hỏi đáp cùng cộng đồng.\n\n" +
                    "**5. Tìm Phòng khám Thú y:**\n" +
                    "• Vào **Bản đồ** ➔ App hiện các phòng khám gần bạn, bấm để gọi điện hoặc chỉ đường.\n\n" +
                    "**6. Trợ lý AI (Bạn đang dùng!):**\n" +
                    "• Hỏi bất kỳ câu hỏi nào về lịch tiêm, dinh dưỡng, bệnh lý, hoặc cách dùng App.\n\n" +
                    "✨ *Bạn cần hướng dẫn chi tiết hơn về tính năng nào không?*";
        }

        // === THÊM THÚ CƯNG / PET ===
        if (q.contains("thêm pet") || q.contains("thêm thú cưng") || q.contains("tạo hồ sơ") || q.contains("thêm bé") || q.contains("đăng ký thú cưng")) {
            return "🐾 **Hướng dẫn Thêm Thú cưng mới trên PetCare:**\n\n" +
                    "1. Mở App ➔ Vào tab **Thú cưng** (biểu tượng chân thú cưng).\n" +
                    "2. Bấm nút **+ Thêm thú cưng** ở góc dưới màn hình.\n" +
                    "3. Điền thông tin của bé:\n" +
                    "   • **Tên, Loài** (Chó/Mèo), **Giống**, **Giới tính**.\n" +
                    "   • **Ngày sinh** (để App tự tính lịch tiêm chuẩn).\n" +
                    "   • **Cân nặng hiện tại**, trạng thái triệt sản.\n" +
                    "4. Chọn ảnh đại diện cho bé ➔ Bấm **Lưu**.\n\n" +
                    "✨ *Sau khi tạo xong, App sẽ tự động đề xuất lịch tiêm phòng chuẩn cho bé!*";
        }

        // === NHẮC NHỞ ===
        if (q.contains("nhắc nhở") || q.contains("đặt lịch") || q.contains("reminder") || q.contains("thông báo")) {
            return "🔔 **Hướng dẫn Tạo Nhắc nhở trên PetCare:**\n\n" +
                    "1. Vào tab **Nhắc nhở** (biểu tượng chuông).\n" +
                    "2. Bấm **+ Tạo nhắc nhở mới**.\n" +
                    "3. Chọn loại nhắc nhở: Tẩy giun, Tắm rửa, Khám thú y, Uống thuốc, hoặc Tùy chỉnh.\n" +
                    "4. Chọn ngày giờ và tần suất lặp lại (Một lần / Hàng tuần / Hàng tháng).\n" +
                    "5. Bấm **Lưu** ➔ App sẽ tự động gửi thông báo đẩy đến điện thoại khi đến hạn!\n\n" +
                    "💡 *Bạn có thể bật/tắt Nhắc nhở trong phần Cài đặt ➔ Thông báo.*";
        }

        // === CÂN NẶNG ===
        if (q.contains("cân nặng") || q.contains("theo dõi cân") || q.contains("biểu đồ") || q.contains("tăng cân") || q.contains("giảm cân")) {
            return "⚖️ **Hướng dẫn Theo dõi Cân nặng trên PetCare:**\n\n" +
                    "1. Vào **Hồ sơ Thú cưng** ➔ Chọn bé cần theo dõi.\n" +
                    "2. Vào tab **Cân nặng** ➔ Bấm **+ Thêm bản ghi cân nặng**.\n" +
                    "3. Nhập số cân và ngày cân ➔ App tự vẽ biểu đồ tăng trưởng.\n" +
                    "4. Biểu đồ giúp bạn phát hiện sớm tình trạng béo phì hoặc sụt cân bất thường.\n\n" +
                    "💡 *Cân bé định kỳ 2-4 tuần/lần để có dữ liệu chính xác nhất!*";
        }

        // === VACCINE ===
        if (q.contains("lịch tiêm") || q.contains("tiêm phòng") || q.contains("vắc xin") || q.contains("vaccine")) {
            if (q.contains("mèo") || q.contains("cat")) {
                return "🐱 **Lịch tiêm phòng đề xuất cho Mèo (PetCare AI):**\n\n" +
                        "• **8 tuần tuổi (Mũi 1):** Mũi 3 trong 1 (Phòng Giảm bạch cầu, Viêm mũi khí quản, Calicivirus).\n" +
                        "• **12 tuần tuổi (Mũi 2):** Nhắc lại mũi 3 trong 1 + Tiêm phòng bệnh Dại (Rabies).\n" +
                        "• **16 tuần tuổi (Mũi 3):** Mũi 3 trong 1 củng cố miễn dịch.\n" +
                        "• **Hàng năm:** Nhắc lại 1 mũi 3 trong 1 + 1 mũi Dại định kỳ.\n\n" +
                        "💡 *Lưu ý: Thông tin trên mang tính chất tham khảo. Trong trường hợp cấp cứu, bạn hãy mang bé đến phòng khám thú y gần nhất nhé!*";
            }
            return "🐶 **Lịch tiêm phòng đề xuất cho Chó (PetCare AI):**\n\n" +
                    "• **6 - 8 tuần tuổi (Mũi 1):** Mũi 5 trong 1 (Phòng Care, Parvo, Viêm gan truyền nhiễm, Cúm, Ho cũi chó).\n" +
                    "• **9 - 11 tuần tuổi (Mũi 2):** Mũi 7 trong 1 (Thêm phòng Leptospira & Corona).\n" +
                    "• **12 - 14 tuần tuổi (Mũi 3):** Mũi 7 trong 1 củng cố + Tiêm phòng bệnh Dại (Rabies).\n" +
                    "• **Hàng năm:** Nhắc lại 1 mũi 7 trong 1 + 1 mũi Dại định kỳ.\n\n" +
                    "💡 *Lưu ý: Thông tin trên mang tính chất tham khảo. Trong trường hợp cấp cứu, bạn hãy mang bé đến phòng khám thú y gần nhất nhé!*";
        }

        // === TẨY GIUN / VE RẬN ===
        if (q.contains("tẩy giun") || q.contains("sổ giun") || q.contains("ve rận")) {
            return "💊 **Lịch Tẩy giun & Trị ve rận chuẩn Thú y:**\n\n" +
                    "• **Chó/Mèo con (dưới 2 tháng):** Tẩy giun mỗi 2 tuần một lần từ 2 tuần tuổi.\n" +
                    "• **Chó/Mèo từ 2 đến 6 tháng:** Tẩy giun mỗi tháng 1 lần.\n" +
                    "• **Thú cưng trưởng thành (trên 6 tháng):** Tẩy giun định kỳ 3 - 6 tháng/lần.\n" +
                    "• **Trị ve rận:** Nhỏ gáy hoặc dùng viên uống (NexGard, Bravecto) định kỳ hàng tháng.\n\n" +
                    "💡 *Lưu ý: Bạn có thể bật tính năng Nhắc nhở Tẩy giun ngay trong App PetCare để không bị quên lịch nhé!*";
        }

        // === THỰC PHẨM ĐỘC HẠI ===
        if (q.contains("độc") || q.contains("cấm ăn") || q.contains("sô cô la") || q.contains("chocolate") || q.contains("hành") || q.contains("tỏi") || q.contains("nho")) {
            return "⚠️ **Danh sách Thực phẩm Độc hại Tuyệt đối CẤM Thú cưng ăn:**\n\n" +
                    "• **Chocolate (Sô-cô-la) & Cà phê:** Chứa Theobromine gây ngộ độc, co giật, suy tim.\n" +
                    "• **Hành, Tỏi, Hành tăm:** Phá hủy hồng cầu, gây thiếu máu cấp tính nguy hiểm.\n" +
                    "• **Nho tươi & Nho khô:** Gây suy thận cấp chỉ với lượng nhỏ.\n" +
                    "• **Kẹo cao su / Xylitol:** Gây hạ đường huyết đột ngột và suy gan.\n" +
                    "• **Xương gà/vịt đã nấu chín:** Dễ vỡ vụn gây đâm thủng dạ dày & ruột.\n\n" +
                    "💡 *Lưu ý: Nếu bé nhỡ ăn phải các thực phẩm trên, bạn hãy đưa bé đến phòng khám thú y ngay lập tức!*";
        }

        // === CHẾ ĐỘ LỊCH TIÊM ===
        if (q.contains("thủ công") || q.contains("tự động") || q.contains("chuyển chế độ") || q.contains("chế độ lịch")) {
            return "📱 **Hướng dẫn Đổi Chế độ Lịch tiêm trên App PetCare:**\n\n" +
                    "1. Vào mục **Hồ sơ Thú cưng** ➔ Chọn **Vắc-xin**.\n" +
                    "2. Ở trên cùng có thanh gạt **\"Sử dụng lịch trình đề xuất của PetCare\"**.\n" +
                    "3. Gạt sang **TẮT** để chuyển sang **Chế độ Thủ công** (Cho phép bạn tự do thêm/sửa/xóa mũi tiêm).\n" +
                    "4. Gạt sang **BẬT** để chuyển sang **Chế độ Tự động** (PetCare sẽ tự tính toán lịch tiêm chuẩn thú y theo tuổi của bé).\n\n" +
                    "✨ *Mọi lịch tiêm đã đánh dấu hoàn thành sẽ luôn được giữ nguyên khi đổi chế độ!*";
        }

        // === BỆNH / TRIỆU CHỨNG ===
        if (q.contains("parvo") || q.contains("care") || q.contains("nôn") || q.contains("tiêu chảy") || q.contains("bệnh") || q.contains("ốm") || q.contains("sốt")) {
            return "🚨 **Dấu hiệu Cảnh báo Bệnh nguy hiểm ở Thú cưng:**\n\n" +
                    "• **Bệnh Parvo / Care ở Chó:** Nôn mửa liên tục, tiêu chảy ra máu mùi hôi nồng, sốt cao, bỏ ăn, nằm bẹp một chỗ.\n" +
                    "• **Bệnh Giảm bạch cầu ở Mèo:** Nôn ra dịch vàng, sốt cao đột ngột, bỏ ăn, ngồi ủ rủ bên bát nước nhưng không uống.\n" +
                    "• **Bệnh Dại:** Sợ nước, hốt hoảng, dữ tợn đột ngột hoặc liệt cơ.\n\n" +
                    "🏥 *Lưu ý: Đây là các bệnh truyền nhiễm rất nguy hiểm. Bạn cần cách ly bé và đưa đến Bác sĩ Thú y ngay lập tức!*";
        }

        // === DINH DƯỠNG / ĂN UỐNG ===
        if (q.contains("ăn gì") || q.contains("cho ăn") || q.contains("dinh dưỡng") || q.contains("thức ăn") || q.contains("hạt") || q.contains("pate")) {
            return "🍖 **Hướng dẫn Dinh dưỡng cho Thú cưng:**\n\n" +
                    "**Chó (Động vật ăn tạp):**\n" +
                    "• Protein chất lượng cao (thịt gà, bò, cá), chất béo lành mạnh, rau củ.\n" +
                    "• Hạt khô kết hợp pate ướt để đa dạng dinh dưỡng.\n\n" +
                    "**Mèo (Động vật ăn thịt bắt buộc):**\n" +
                    "• Cần protein động vật cao, Taurine (rất quan trọng cho tim & mắt mèo).\n" +
                    "• Nên cho ăn thêm pate ướt để bổ sung nước, phòng sỏi thận.\n\n" +
                    "• **Nước sạch 24/7** luôn phải có sẵn cho bé.\n\n" +
                    "💡 *Lưu ý: Tránh hoàn toàn Chocolate, Hành, Tỏi, Nho, Xylitol — rất nguy hiểm cho thú cưng!*";
        }

        // === TẮM RỬA / VỆ SINH ===
        if (q.contains("tắm") || q.contains("vệ sinh") || q.contains("chải lông") || q.contains("cắt tỉa") || q.contains("grooming")) {
            return "🛁 **Hướng dẫn Vệ sinh & Chăm sóc Thú cưng:**\n\n" +
                    "• **Tắm Chó:** 2 - 4 tuần/lần bằng sữa tắm chuyên dụng cho chó.\n" +
                    "• **Tắm Mèo:** Mèo tự vệ sinh, chỉ tắm khi bẩn nặng hoặc theo hướng dẫn bác sĩ.\n" +
                    "• **Chải lông:** 2-3 lần/tuần để loại bỏ lông rụng, ngừa búi lông (mèo).\n" +
                    "• **Răng miệng:** Đánh răng 2-3 lần/tuần bằng kem đánh răng dành cho thú cưng.\n" +
                    "• **Vệ sinh tai:** Lau tai hàng tuần bằng dung dịch chuyên dụng.\n\n" +
                    "💡 *Bạn có thể tạo Nhắc nhở Tắm rửa định kỳ ngay trên App PetCare!*";
        }

        // === DEFAULT ===
        return "🐾 **Xin chào! Tôi là PetCare AI - Trợ lý Thú y & Hỗ trợ ứng dụng PetCare.**\n\n" +
                "Tôi có thể hỗ trợ bạn:\n" +
                "• 📱 **Hướng dẫn sử dụng** App PetCare (thêm pet, lịch tiêm, nhắc nhở, cân nặng).\n" +
                "• 💉 **Lịch tiêm phòng** chuẩn cho Chó & Mèo.\n" +
                "• 💊 **Tẩy giun**, phòng trị ve rận định kỳ.\n" +
                "• ⚠️ **Thực phẩm độc hại** cần tránh.\n" +
                "• 🍖 **Dinh dưỡng** & chế độ ăn hợp lý.\n" +
                "• 🚨 **Dấu hiệu bệnh** nguy hiểm cần đi thú y.\n" +
                "• 🛁 **Vệ sinh**, tắm rửa, chăm sóc lông.\n\n" +
                "Bạn cần tôi hỗ trợ thông tin gì cho bé hôm nay?";
    }
}


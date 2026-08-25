-- ============================================================
-- PETCARE FULL SEED SCRIPT: SPECIES, BREEDS & VACCINE TEMPLATES
-- Chạy script này trên MySQL VPS để nạp toàn bộ dữ liệu chuẩn
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. XÓA VÀ NẠP LẠI DANH MỤC LOÀI (SPECIES)
DELETE FROM categories_species;
ALTER TABLE categories_species AUTO_INCREMENT = 1;

INSERT INTO categories_species (id, name, icon_url) VALUES
(1, 'Chó', NULL),
(2, 'Mèo', NULL),
(3, 'Chim', NULL),
(4, 'Thỏ', NULL),
(5, 'Hamster', NULL),
(6, 'Cá', NULL),
(7, 'Bò sát', NULL),
(8, 'Khác', NULL);

-- 2. XÓA VÀ NẠP LẠI DANH MỤC GIỐNG (BREEDS)
DELETE FROM categories_breeds;
ALTER TABLE categories_breeds AUTO_INCREMENT = 1;

INSERT INTO categories_breeds (species_id, name) VALUES
(1, 'Labrador Retriever'),
(1, 'Golden Retriever'),
(1, 'Poodle'),
(1, 'Corgi'),
(1, 'Husky'),
(1, 'Beagle'),
(1, 'Bulldog'),
(1, 'Chihuahua'),
(1, 'Shih Tzu'),
(1, 'Pomeranian'),
(1, 'German Shepherd'),
(1, 'Rottweiler'),
(1, 'Dachshund'),
(1, 'Boxer'),
(1, 'Border Collie'),
(1, 'Pug'),
(1, 'Maltese'),
(1, 'Yorkshire Terrier'),
(1, 'Phú Quốc Ridgeback'),
(1, 'Chó ta (Nội địa)'),
(1, 'Hỗn hợp / Không rõ'),
(2, 'Anh lông ngắn (British Shorthair)'),
(2, 'Ba Tư (Persian)'),
(2, 'Scottish Fold'),
(2, 'Maine Coon'),
(2, 'Ragdoll'),
(2, 'Siamese'),
(2, 'Bengal'),
(2, 'Sphinx'),
(2, 'Russian Blue'),
(2, 'Mèo ta (Nội địa)'),
(2, 'Hỗn hợp / Không rõ'),
(3, 'Vẹt đuôi dài (Budgerigar)'),
(3, 'Vẹt Cockatiel'),
(3, 'Vẹt Cockatoo'),
(3, 'Vẹt African Grey'),
(3, 'Chim sẻ'),
(3, 'Chim yến'),
(3, 'Chim cú'),
(3, 'Khác'),
(4, 'Holland Lop'),
(4, 'Netherland Dwarf'),
(4, 'Mini Rex'),
(4, 'Lionhead'),
(4, 'Flemish Giant'),
(4, 'Thỏ nội địa'),
(4, 'Hỗn hợp / Không rõ'),
(5, 'Syrian (Gấu vàng)'),
(5, 'Roborovski'),
(5, 'Campbell'),
(5, 'Winter White'),
(5, 'Chinese'),
(5, 'Hỗn hợp / Không rõ'),
(6, 'Cá betta'),
(6, 'Cá vàng (Goldfish)'),
(6, 'Cá Koi'),
(6, 'Cá La Hán'),
(6, 'Cá Neon Tetra'),
(6, 'Cá thần tiên (Angelfish)'),
(6, 'Khác'),
(7, 'Khác'),
(8, 'Khác');

-- 3. XÓA VÀ NẠP LẠI PHÁC ĐỒ VẮC-XIN CHUẨN (VACCINE TEMPLATES)
DELETE FROM vaccine_templates;
ALTER TABLE vaccine_templates AUTO_INCREMENT = 1;

-- Vắc-xin cho CHÓ (species_id = 1)
INSERT INTO vaccine_templates (
  species_id, series_code, vaccine_name, target_stage, dose_number,
  recommended_age_weeks, minimum_age_weeks, interval_from_previous_days, booster_interval_months,
  is_optional, is_active, description
) VALUES
(1, 'CANINE_CORE_DHPP', 'DHPP core - mũi 1', 'PUPPY', 1, 8, 8, 0, NULL, 0, 1, 'Lịch đề xuất tham khảo; cần bác sĩ xác nhận và tuân theo nhãn vaccine.'),
(1, 'CANINE_CORE_DHPP', 'DHPP core - mũi 2', 'PUPPY', 2, 12, 12, 28, NULL, 0, 1, 'Khoảng cách tham khảo 4 tuần.'),
(1, 'CANINE_CORE_DHPP', 'DHPP core - mũi cuối puppy', 'PUPPY', 3, 16, 16, 28, NULL, 0, 1, 'Mũi puppy core cuối không sớm hơn 16 tuần tuổi.'),
(1, 'CANINE_CORE_DHPP', 'DHPP core - mũi 26+ tuần', 'PUPPY', 4, 26, 26, 70, NULL, 0, 1, 'Mũi theo dõi ở hoặc ngay sau 26 tuần tuổi.'),
(1, 'CANINE_CORE_DHPP', 'DHPP core catch-up', 'CATCH_UP', 1, 26, 26, 0, NULL, 0, 1, 'Một liều MLV core thường đủ cho chó trên 26 tuần.'),
(1, 'CANINE_CORE_DHPP', 'DHPP core catch-up nguy cơ cao', 'CATCH_UP', 2, 26, 26, 28, NULL, 1, 1, 'Liều tùy chọn khi bác sĩ đánh giá nguy cơ cao.'),
(1, 'CANINE_CORE_DHPP', 'DHPP core nhắc lại', 'ADULT', 1, 26, 26, 0, 36, 0, 1, 'Chu kỳ tham khảo 36 tháng cho core MLV sau đáp ứng ban đầu.'),
(1, 'CANINE_RABIES', 'Vaccine dại puppy', 'PUPPY', 1, 12, 12, 0, NULL, 0, 1, 'Thời điểm phải tuân theo nhãn sản phẩm và quy định địa phương.'),
(1, 'CANINE_RABIES', 'Vaccine dại catch-up', 'CATCH_UP', 1, 26, 26, 0, NULL, 0, 1, 'Lịch catch-up cần bác sĩ và quy định địa phương xác nhận.'),
(1, 'CANINE_RABIES', 'Vaccine dại nhắc lại', 'ADULT', 1, 26, 26, 0, 12, 0, 1, 'Chu kỳ chỉ là cấu hình mặc định; ưu tiên luật và nhãn sản phẩm.');

-- Vắc-xin cho MÈO (species_id = 2)
INSERT INTO vaccine_templates (
  species_id, series_code, vaccine_name, target_stage, dose_number,
  recommended_age_weeks, minimum_age_weeks, interval_from_previous_days, booster_interval_months,
  is_optional, is_active, description
) VALUES
(2, 'FELINE_CORE_FVRCP', 'FVRCP core - mũi 1', 'PUPPY', 1, 8, 8, 0, NULL, 0, 1, 'Lịch kitten tham khảo; cần bác sĩ xác nhận và tuân theo nhãn vaccine.'),
(2, 'FELINE_CORE_FVRCP', 'FVRCP core - mũi 2', 'PUPPY', 2, 12, 12, 28, NULL, 0, 1, 'Khoảng cách tham khảo 4 tuần.'),
(2, 'FELINE_CORE_FVRCP', 'FVRCP core - mũi cuối kitten', 'PUPPY', 3, 16, 16, 28, NULL, 0, 1, 'Mũi core cuối không sớm hơn 16 tuần tuổi.'),
(2, 'FELINE_CORE_FVRCP', 'FVRCP core - mũi 26+ tuần', 'PUPPY', 4, 26, 26, 70, NULL, 0, 1, 'Mũi theo dõi ở hoặc ngay sau 26 tuần tuổi.'),
(2, 'FELINE_CORE_FVRCP', 'FVRCP core catch-up - mũi 1', 'CATCH_UP', 1, 26, 26, 0, NULL, 0, 1, 'Mũi đầu của lịch catch-up cho mèo trưởng thành.'),
(2, 'FELINE_CORE_FVRCP', 'FVRCP core catch-up - mũi 2', 'CATCH_UP', 2, 26, 26, 28, NULL, 0, 1, 'Mũi thứ hai cách mũi đầu 4 tuần.'),
(2, 'FELINE_CORE_FVRCP', 'FVRCP core nhắc lại', 'ADULT', 1, 26, 26, 0, 36, 0, 1, 'Chu kỳ tham khảo 36 tháng cho mèo nguy cơ thấp sau phác đồ nền.'),
(2, 'FELINE_RABIES', 'Vaccine dại kitten', 'PUPPY', 1, 12, 12, 0, NULL, 0, 1, 'Thời điểm phải tuân theo nhãn sản phẩm và quy định địa phương.'),
(2, 'FELINE_RABIES', 'Vaccine dại catch-up', 'CATCH_UP', 1, 26, 26, 0, NULL, 0, 1, 'Lịch catch-up cần bác sĩ và quy định địa phương xác nhận.'),
(2, 'FELINE_RABIES', 'Vaccine dại nhắc lại', 'ADULT', 1, 26, 26, 0, 12, 0, 1, 'Chu kỳ chỉ là cấu hình mặc định; ưu tiên luật và nhãn sản phẩm.');

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- SEED DATA: Loài thú cưng (categories_species)
-- ============================================================
INSERT IGNORE INTO categories_species (id, name, icon_url) VALUES
(1, 'Chó', NULL),
(2, 'Mèo', NULL),
(3, 'Chim', NULL),
(4, 'Thỏ', NULL),
(5, 'Hamster', NULL),
(6, 'Cá', NULL),
(7, 'Bò sát', NULL),
(8, 'Khác', NULL);

-- ============================================================
-- SEED DATA: Giống chó (categories_breeds) - species_id = 1
-- ============================================================
INSERT IGNORE INTO categories_breeds (species_id, name) VALUES
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
(1, 'Hỗn hợp / Không rõ');

-- ============================================================
-- SEED DATA: Giống mèo (categories_breeds) - species_id = 2
-- ============================================================
INSERT IGNORE INTO categories_breeds (species_id, name) VALUES
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
(2, 'Hỗn hợp / Không rõ');

-- ============================================================
-- SEED DATA: Giống chim (categories_breeds) - species_id = 3
-- ============================================================
INSERT IGNORE INTO categories_breeds (species_id, name) VALUES
(3, 'Vẹt đuôi dài (Budgerigar)'),
(3, 'Vẹt Cockatiel'),
(3, 'Vẹt Cockatoo'),
(3, 'Vẹt African Grey'),
(3, 'Chim sẻ'),
(3, 'Chim yến'),
(3, 'Chim cú'),
(3, 'Khác');

-- ============================================================
-- SEED DATA: Giống thỏ (categories_breeds) - species_id = 4
-- ============================================================
INSERT IGNORE INTO categories_breeds (species_id, name) VALUES
(4, 'Holland Lop'),
(4, 'Netherland Dwarf'),
(4, 'Mini Rex'),
(4, 'Lionhead'),
(4, 'Flemish Giant'),
(4, 'Thỏ nội địa'),
(4, 'Hỗn hợp / Không rõ');

-- ============================================================
-- SEED DATA: Giống hamster (categories_breeds) - species_id = 5
-- ============================================================
INSERT IGNORE INTO categories_breeds (species_id, name) VALUES
(5, 'Syrian (Gấu vàng)'),
(5, 'Roborovski'),
(5, 'Campbell'),
(5, 'Winter White'),
(5, 'Chinese');

-- ============================================================
-- SEED DATA: Giống cá (categories_breeds) - species_id = 6
-- ============================================================
INSERT IGNORE INTO categories_breeds (species_id, name) VALUES
(6, 'Cá betta'),
(6, 'Cá vàng (Goldfish)'),
(6, 'Cá Koi'),
(6, 'Cá La Hán'),
(6, 'Cá Neon Tetra'),
(6, 'Cá thần tiên (Angelfish)'),
(6, 'Khác');

-- ============================================================
-- Giống cho loài còn thiếu mục "Khác"
-- ============================================================
INSERT IGNORE INTO categories_breeds (species_id, name) VALUES
(5, 'Hỗn hợp / Không rõ'),
(7, 'Khác'),
(8, 'Khác');

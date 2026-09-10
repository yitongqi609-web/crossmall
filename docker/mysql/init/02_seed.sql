-- =============================================================
-- CrossMall 种子数据:演示账号 / 双语商品 / 汇率 / 关税税则 / 物流线路
-- 演示账号:买家 demo@crossmall.com / demo123;管理员 admin / admin123
-- ({raw}前缀的明文密码由应用启动时 PasswordInitRunner 自动升级为 BCrypt)
-- =============================================================
USE crossmall;
SET NAMES utf8mb4;

-- ---------- 账号 ----------
INSERT INTO admin_user (username, password, nickname) VALUES
('admin', '{raw}admin123', '潮汐管理员');

INSERT INTO `user` (email, password, nickname) VALUES
('demo@crossmall.com', '{raw}demo123', 'TideDemo');

INSERT INTO address (user_id, receiver_name, phone, country_code, country_name, state_province, city, street, postcode, is_default) VALUES
(1, 'John Carter', '+1 202-555-0147', 'US', 'United States', 'California', 'Los Angeles', '8420 Sunset Blvd, Suite 210', '90069', 1),
(1, 'Emma Wilson', '+44 20 7946 0958', 'GB', 'United Kingdom', 'London', 'London', '221B Baker Street', 'NW1 6XE', 0);

-- ---------- 分类 ----------
INSERT INTO category (id, name, name_en, icon, sort) VALUES
(1, '数码电子', 'Electronics', '📱', 1),
(2, '服饰鞋包', 'Apparel',    '👕', 2),
(3, '家居生活', 'Home & Living', '🏠', 3),
(4, '美妆个护', 'Beauty',     '💄', 4),
(5, '运动户外', 'Outdoor',    '🏕️', 5);

-- ---------- 汇率(1 USD = rate) ----------
INSERT INTO fx_rate (currency, currency_en, symbol, rate, source) VALUES
('USD', 'US Dollar',      '$',   1.000000,  'MANUAL'),
('EUR', 'Euro',           '€',   0.920000,  'MANUAL'),
('GBP', 'British Pound',  '£',   0.790000,  'MANUAL'),
('JPY', 'Japanese Yen',   '¥',   155.300000,'MANUAL'),
('AUD', 'Australian Dollar', 'A$', 1.520000, 'MANUAL'),
('CNY', 'Chinese Yuan',   'CN¥', 7.120000,  'MANUAL');

-- ---------- 关税税则(查找顺序:国家+HS → 国家默认* → 全球默认*) ----------
INSERT INTO hs_tax (country_code, country_name, hs_code, hs_name, tax_rate, de_minimis_cents) VALUES
('*',  'Global Default', '*',       '通用税率',       0.0800, 0),
('US', 'United States',  '*',       '美国通用',       0.0500, 80000),
('US', 'United States',  '8517.13', '智能手机',       0.0000, 80000),
('US', 'United States',  '9102.12', '智能手表',       0.0000, 80000),
('GB', 'United Kingdom', '*',       '英国通用',       0.1200, 0),
('GB', 'United Kingdom', '6109.10', '针织T恤',        0.1200, 0),
('DE', 'Germany',        '*',       '德国通用',       0.1400, 0),
('DE', 'Germany',        '6109.10', '针织T恤',        0.1200, 0),
('FR', 'France',         '*',       '法国通用',       0.1400, 0),
('JP', 'Japan',          '*',       '日本通用',       0.0800, 10000),
('JP', 'Japan',          '8517.13', '智能手机',       0.0000, 10000),
('AU', 'Australia',      '*',       '澳大利亚通用',   0.1000, 66000),
('CA', 'Canada',         '*',       '加拿大通用',     0.0900, 15000),
('SG', 'Singapore',      '*',       '新加坡通用',     0.0700, 30000);

-- ---------- 物流线路 ----------
INSERT INTO logistics_line (name, name_en, carrier, supported_countries, first_weight_grams, first_fee_cents, continue_weight_grams, continue_fee_cents, eta_min_days, eta_max_days) VALUES
('经济标准线', 'Economic Standard', '4PX 递四方', '["*"]',                       500, 499, 500, 250, 10, 18),
('快速直邮线', 'Express Direct',    'YunExpress 云途', '["*"]',                  500, 999, 500, 400, 6, 10),
('中欧铁路线', 'China-EU Rail',     'CDEK 中欧班列', '["GB","DE","FR","SE","PL","NL"]', 800, 799, 1000, 180, 16, 22),
('美线特惠',   'US Special Line',   'Yanwen 燕文',   '["US","CA"]',             200, 399, 100, 120, 8, 12);

-- ---------- 商品(20 SPU / 43 SKU) ----------
INSERT INTO spu (id, category_id, title, title_en, description, description_en, brand, hs_code, weight_grams, sales) VALUES
(1,  1, '星尘 X5 Pro 5G 智能手机', 'Stardust X5 Pro 5G Smartphone', '6.7 英寸 AMOLED 全面屏,骁龙旗舰芯片,6400 万影像系统,双模 5G。', '6.7" AMOLED display, flagship chipset, 64MP camera system, dual-mode 5G.', 'Stardust', '8517.13', 380, 2861),
(2,  1, '云鹿 UltraBook 14 轻薄本', 'CloudDeer UltraBook 14 Laptop', '1.19kg 镁铝合金机身,2.8K 90Hz 好屏,全功能 Type-C。', '1.19kg magnesium-alloy body, 2.8K 90Hz display, full-function USB-C.', 'CloudDeer', '8471.30', 1650, 1204),
(3,  1, '声浪 Air 主动降噪耳机', 'SoundWave Air ANC Earbuds', '45dB 深度降噪,蓝牙 5.4 双设备连接,单次 9 小时续航。', '45dB deep ANC, Bluetooth 5.4 dual-device, 9h single charge.', 'SoundWave', '8518.30', 120, 3522),
(4,  1, '长风 20000mAh 移动电源', 'LongWind 20000mAh Power Bank', '22.5W 超级快充,双向 Type-C,可上飞机。', '22.5W fast charge, dual USB-C, flight-safe.', 'LongWind', '8507.60', 420, 2010),
(5,  1, '潮汐智能手表 T2', 'Tide Smartwatch T2', '1.85 英寸高清大屏,血氧/心率监测,14 天续航,100+ 运动模式。', '1.85" display, SpO2 & heart rate, 14-day battery, 100+ sport modes.', 'TideMall', '9102.12', 160, 1780),
(6,  2, '山雾纯棉基础T恤', 'Mountain Mist Cotton Tee', '280g 重磅新疆棉,螺纹领口不变形,基础百搭。', '280g heavyweight cotton, ribbed collar, versatile basics.', 'Mountain Mist', '6109.10', 220, 4310),
(7,  2, '极夜三防冲锋衣', 'PolarNight Shell Jacket', '三合一抓绒内胆,防风防水透湿,适合 -10℃ 环境通勤徒步。', '3-in-1 fleece liner, windproof & waterproof, for -10°C commutes and hikes.', 'PolarNight', '6201.40', 950, 1544),
(8,  2, '逐云轻量跑鞋', 'CloudChaser Running Shoes', '轻至 218g,高回弹中底,透气网面,日常慢跑必备。', '218g per shoe, high-rebound midsole, breathable mesh.', 'CloudChaser', '6403.99', 340, 2678),
(9,  2, '漫游者帆布双肩包', 'Wanderer Canvas Backpack', '20L 城市通勤,防泼水帆布,16 英寸电脑仓。', '20L urban commuter, water-repellent canvas, 16" laptop sleeve.', 'Wanderer', '4202.22', 680, 980),
(10, 2, '极简羊毛围巾', 'Minimalist Wool Scarf', '澳大利亚美丽诺羊毛,200×35cm,双面针织。', 'Australian merino wool, 200x35cm, double-face knit.', 'Minimalist', '6214.10', 180, 655),
(11, 3, '栖木实木边几', 'Perch Solid Wood Side Table', 'FAS 级白橡木,榫卯工艺,可作床头几/沙发边几。', 'FAS-grade white oak, mortise & tenon joinery, nightstand or side table.', 'Perch', '9403.60', 5200, 421),
(12, 3, '青瓷对杯礼盒', 'Celadon Cup Gift Set', '龙泉青瓷,梅子青釉,一壶两杯礼盒装。', 'Longquan celadon, plum-green glaze, gift box with pot and two cups.', 'CeladonCraft', '6912.00', 900, 760),
(13, 3, '云感记忆棉枕头', 'CloudFeel Memory Foam Pillow', '4 秒慢回弹记忆棉,人体工学曲线,可拆洗针织外套。', '4s slow-rebound memory foam, ergonomic curve, washable knit cover.', 'CloudFeel', '9404.90', 1100, 1560),
(14, 3, '暖阳香薰蜡烛套装', 'WarmSun Candle Set', '天然大豆蜡,雪松/海盐/白茶三味,45 小时燃烧。', 'Natural soy wax, cedar/sea salt/white tea, 45h burn time each.', 'WarmSun', '3406.00', 750, 1122),
(15, 4, '雪润玻尿酸精华', 'SnowGlow Hyaluronic Serum', '5 重玻尿酸复配,320kDa 小分子渗透,敏感肌可用。', '5-weight hyaluronic complex, 320kDa low molecules, sensitive-skin safe.', 'SnowGlow', '3304.99', 130, 2340),
(16, 4, '山茶花滋润唇膏', 'Camellia Lip Balm', '山茶花油 + 乳木果,8 小时保湿,食品级原料。', 'Camellia oil & shea butter, 8h moisture, food-grade ingredients.', 'Camellia', '3304.10', 45, 3120),
(17, 4, '海盐控油洗发水', 'SeaSalt Oil-Control Shampoo', '海盐颗粒按摩头皮,氨基酸表活,控油 72 小时。', 'Sea salt scalp scrub, amino-acid surfactants, 72h oil control.', 'SeaSalt', '3305.10', 560, 1450),
(18, 5, '露宿星空双人帐篷', 'Starry Night 2P Tent', '210T 防水面料,加压 3000mm,铝合金杆,2.1kg。', '210T PU3000mm fabric, aluminum poles, 2.1kg.', 'StarryNight', '6306.22', 2300, 890),
(19, 5, '御风铝合金折叠椅', 'WindGuard Folding Chair', '7075 铝合金,承重 150kg,仅 950g,便携收纳袋。', '7075 aluminum, 150kg load, 950g, carry pouch included.', 'WindGuard', '9401.61', 1050, 1670),
(20, 5, '曜阳偏光太阳镜', 'SunVeil Polarized Sunglasses', 'TAC 偏光镜片,UV400,TR90 超轻镜框。', 'TAC polarized lenses, UV400, TR90 ultralight frame.', 'SunVeil', '9004.10', 95, 2260);

INSERT INTO sku (spu_id, attrs, price_cents, stock, weight_grams) VALUES
-- 1 星尘 X5 Pro
(1, '[{"k":"颜色","kEn":"Color","v":"曜石黑","vEn":"Obsidian Black"},{"k":"存储","kEn":"Storage","v":"8GB+128GB","vEn":"8GB+128GB"}]', 29900, 120, 380),
(1, '[{"k":"颜色","kEn":"Color","v":"冰川蓝","vEn":"Glacier Blue"},{"k":"存储","kEn":"Storage","v":"8GB+128GB","vEn":"8GB+128GB"}]', 29900, 80, 380),
(1, '[{"k":"颜色","kEn":"Color","v":"曜石黑","vEn":"Obsidian Black"},{"k":"存储","kEn":"Storage","v":"12GB+256GB","vEn":"12GB+256GB"}]', 39900, 60, 380),
-- 2 云鹿 UltraBook
(2, '[{"k":"颜色","kEn":"Color","v":"月光银","vEn":"Moonlight Silver"}]', 69900, 45, 1650),
(2, '[{"k":"颜色","kEn":"Color","v":"深空灰","vEn":"Space Gray"}]', 69900, 45, 1650),
-- 3 声浪 Air
(3, '[{"k":"颜色","kEn":"Color","v":"陶瓷白","vEn":"Ceramic White"}]', 5999, 200, 120),
(3, '[{"k":"颜色","kEn":"Color","v":"夜幕黑","vEn":"Midnight Black"}]', 5999, 200, 120),
-- 4 长风移动电源
(4, '[{"k":"颜色","kEn":"Color","v":"钛灰","vEn":"Titanium Gray"}]', 3990, 300, 420),
-- 5 潮汐手表
(5, '[{"k":"表带","kEn":"Strap","v":"曜石黑氟橡胶","vEn":"Obsidian Fluoroelastomer"}]', 12900, 150, 160),
(5, '[{"k":"表带","kEn":"Strap","v":"星空米兰尼斯","vEn":"Starry Milanese"}]', 13900, 120, 160),
-- 6 基础T恤
(6, '[{"k":"颜色","kEn":"Color","v":"云雾白","vEn":"Cloud White"},{"k":"尺码","kEn":"Size","v":"M","vEn":"M"}]', 1990, 400, 220),
(6, '[{"k":"颜色","kEn":"Color","v":"云雾白","vEn":"Cloud White"},{"k":"尺码","kEn":"Size","v":"L","vEn":"L"}]', 1990, 400, 230),
(6, '[{"k":"颜色","kEn":"Color","v":"岩灰","vEn":"Rock Gray"},{"k":"尺码","kEn":"Size","v":"L","vEn":"L"}]', 1990, 300, 230),
-- 7 冲锋衣
(7, '[{"k":"颜色","kEn":"Color","v":"极夜黑","vEn":"Polar Night Black"},{"k":"尺码","kEn":"Size","v":"M","vEn":"M"}]', 8900, 120, 950),
(7, '[{"k":"颜色","kEn":"Color","v":"极夜黑","vEn":"Polar Night Black"},{"k":"尺码","kEn":"Size","v":"L","vEn":"L"}]', 8900, 120, 980),
-- 8 跑鞋
(8, '[{"k":"配色","kEn":"Colorway","v":"月岩灰","vEn":"Moonrock"},{"k":"尺码","kEn":"Size","v":"EU 42","vEn":"EU 42"}]', 7900, 150, 340),
(8, '[{"k":"配色","kEn":"Colorway","v":"月岩灰","vEn":"Moonrock"},{"k":"尺码","kEn":"Size","v":"EU 43","vEn":"EU 43"}]', 7900, 150, 350),
(8, '[{"k":"配色","kEn":"Colorway","v":"薄暮蓝","vEn":"Dusk Blue"},{"k":"尺码","kEn":"Size","v":"EU 44","vEn":"EU 44"}]', 7900, 120, 350),
-- 9 双肩包
(9, '[{"k":"颜色","kEn":"Color","v":"军绿","vEn":"Army Green"}]', 4590, 180, 680),
-- 10 围巾
(10, '[{"k":"颜色","kEn":"Color","v":"雾灰","vEn":"Fog Gray"}]', 2900, 200, 180),
(10, '[{"k":"颜色","kEn":"Color","v":"驼色","vEn":"Camel"}]', 2900, 200, 180),
-- 11 边几
(11, '[{"k":"材质","kEn":"Material","v":"白橡木原色","vEn":"White Oak Natural"}]', 11900, 30, 5200),
-- 12 青瓷对杯
(12, '[{"k":"规格","kEn":"Spec","v":"一壶两杯礼盒","vEn":"1 Pot + 2 Cups Gift Box"}]', 3490, 90, 900),
-- 13 枕头
(13, '[{"k":"规格","kEn":"Spec","v":"标准单只 48*74cm","vEn":"Standard 48*74cm"}]', 4200, 260, 1100),
-- 14 蜡烛
(14, '[{"k":"香型","kEn":"Scent","v":"雪松·海盐·白茶 三只装","vEn":"Cedar · Sea Salt · White Tea, 3-pack"}]', 2790, 220, 750),
-- 15 精华
(15, '[{"k":"规格","kEn":"Spec","v":"30ml","vEn":"30ml"}]', 3600, 350, 130),
-- 16 唇膏
(16, '[{"k":"色号","kEn":"Shade","v":"蜜桃乌龙","vEn":"Peach Oolong"}]', 1290, 500, 45),
(16, '[{"k":"色号","kEn":"Shade","v":"莓果红酒","vEn":"Berry Wine"}]', 1290, 500, 45),
-- 17 洗发水
(17, '[{"k":"规格","kEn":"Spec","v":"500ml","vEn":"500ml"}]', 1890, 280, 560),
-- 18 帐篷
(18, '[{"k":"规格","kEn":"Spec","v":"双人标准版","vEn":"2-Person Standard"}]', 9900, 70, 2300),
-- 19 折叠椅
(19, '[{"k":"颜色","kEn":"Color","v":"森林绿","vEn":"Forest Green"}]', 3990, 240, 1050),
-- 20 太阳镜
(20, '[{"k":"颜色","kEn":"Color","v":"曜石黑","vEn":"Obsidian Black"}]', 4900, 260, 95),
(20, '[{"k":"颜色","kEn":"Color","v":"玳瑁棕","vEn":"Tortoise Brown"}]', 4900, 200, 95);

/*
 Navicat Premium Data Transfer

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 50734
 Source Host           : localhost:3306
 Source Schema         : campus

 Target Server Type    : MySQL
 Target Server Version : 50734
 File Encoding         : 65001

 Date: 22/06/2026 11:14:36
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for appeals
-- ----------------------------
DROP TABLE IF EXISTS `appeals`;
CREATE TABLE `appeals`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_id` bigint(20) NOT NULL,
  `appealer_id` bigint(20) NOT NULL,
  `defendant_id` bigint(20) NOT NULL,
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `evidence_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `status` int(11) NOT NULL DEFAULT 0,
  `admin_remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `admin_id` bigint(20) NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '申诉仲裁表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of appeals
-- ----------------------------
INSERT INTO `appeals` VALUES (1, 11, 2, 9, '跑腿员长时间未送达，已超过预期时间2小时，且不接电话', '/avatars/1_1777279107885.jpg', 0, NULL, NULL, '2026-03-24 11:00:04', '2026-04-27 16:54:46', 0);
INSERT INTO `appeals` VALUES (2, 16, 7, 13, '跑腿员接单后一直不来取货，已经超过2小时了', '/avatars/1_1777279107885.jpg', 0, NULL, NULL, '2026-04-26 20:53:35', '2026-04-27 16:54:46', 0);
INSERT INTO `appeals` VALUES (3, 11, 4, 13, '物品损坏，箱子有破损', '/avatars/1_1777279107885.jpg', 0, NULL, NULL, '2026-04-26 20:53:35', '2026-04-27 16:54:46', 0);
INSERT INTO `appeals` VALUES (4, 2, 2, 6, '超时送达，而且送错地方了', '/avatars/1_1777279107885.jpg', 1, '核实后确实送错，已罚款跑腿员', 1, '2026-04-26 20:53:35', '2026-04-27 16:54:46', 0);
INSERT INTO `appeals` VALUES (5, 22, 3, 4, '图书没有归还成功', '', 0, NULL, NULL, '2026-05-21 12:00:27', '2026-05-21 12:00:27', 0);
INSERT INTO `appeals` VALUES (6, 31, 3, 4, '水果坏了', 'http://localhost:8080/api/files/orders/2026/05/21/48d517cb97b84fd99e9deb43af3120e8.png,http://localhost:8080/api/files/orders/2026/05/21/1a593a5c8dae42eba690919b2d22ec31.png', 0, NULL, NULL, '2026-05-21 14:53:33', '2026-05-21 14:53:33', 0);

-- ----------------------------
-- Table structure for credit_history
-- ----------------------------
DROP TABLE IF EXISTS `credit_history`;
CREATE TABLE `credit_history`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `delta` int(11) NOT NULL COMMENT '变动值（正数加分，负数扣分）',
  `before_score` int(11) NOT NULL COMMENT '变动前分数',
  `after_score` int(11) NOT NULL COMMENT '变动后分数',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '变动原因',
  `related_order_id` bigint(20) NULL DEFAULT NULL COMMENT '关联订单ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '信用分历史记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of credit_history
-- ----------------------------
INSERT INTO `credit_history` VALUES (1, 6, 3, 99, 102, '订单评价: 5星 (发布者评价)', 2, '2026-05-28 22:24:40');

-- ----------------------------
-- Table structure for menu_permissions
-- ----------------------------
DROP TABLE IF EXISTS `menu_permissions`;
CREATE TABLE `menu_permissions`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  `permission_id` bigint(20) NOT NULL COMMENT '权限ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_menu_permission`(`menu_id`, `permission_id`) USING BTREE,
  INDEX `FK920eibr125089uc8shiwiqhth`(`permission_id`) USING BTREE,
  CONSTRAINT `FK920eibr125089uc8shiwiqhth` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKgqhw0tj26d3bkn8ol6jc02mre` FOREIGN KEY (`menu_id`) REFERENCES `menus` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单权限关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of menu_permissions
-- ----------------------------
INSERT INTO `menu_permissions` VALUES (1, 1, 1, '2026-04-26 22:23:17');
INSERT INTO `menu_permissions` VALUES (2, 2, 3, '2026-04-26 22:23:17');
INSERT INTO `menu_permissions` VALUES (3, 3, 6, '2026-04-26 22:23:17');
INSERT INTO `menu_permissions` VALUES (4, 4, 10, '2026-04-26 22:23:17');
INSERT INTO `menu_permissions` VALUES (5, 5, 14, '2026-04-26 22:23:17');
INSERT INTO `menu_permissions` VALUES (6, 6, 16, '2026-04-26 22:23:17');
INSERT INTO `menu_permissions` VALUES (7, 7, 18, '2026-04-26 22:23:17');

-- ----------------------------
-- Table structure for menus
-- ----------------------------
DROP TABLE IF EXISTS `menus`;
CREATE TABLE `menus`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `menu_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `menu_icon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `menu_path` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '前端路由路径',
  `parent_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '父菜单ID，0表示顶级菜单',
  `menu_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '菜单类型：1目录 2菜单 3按钮',
  `permission_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限标识',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '排序',
  `visible` tinyint(4) NOT NULL DEFAULT 1 COMMENT '是否显示：1显示 0隐藏',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of menus
-- ----------------------------
INSERT INTO `menus` VALUES (1, '数据大屏', 'chart-bar', '/dashboard', 0, 2, 'dashboard', 1, 1, '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `menus` VALUES (2, '订单管理', 'shopping-cart', '/orders', 0, 2, 'order', 2, 1, '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `menus` VALUES (3, '用户管理', 'user', '/users', 0, 2, 'user', 3, 1, '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `menus` VALUES (4, '认证审核', 'check-circle', '/verify', 0, 2, 'verify', 4, 1, '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `menus` VALUES (5, '申诉仲裁', 'gavel', '/appeals', 0, 2, 'appeal', 5, 1, '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `menus` VALUES (6, '提现审核', 'money', '/withdraws', 0, 2, 'withdraw', 6, 1, '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `menus` VALUES (7, '系统配置', 'setting', '/config', 0, 2, 'config', 7, 1, '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `menus` VALUES (8, '测试菜单', 'test', '/test', 0, 2, 'test', 10, 1, '2026-04-27 09:28:29', '2026-04-27 09:28:29', 0);

-- ----------------------------
-- Table structure for messages
-- ----------------------------
DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_id` bigint(20) NOT NULL,
  `sender_id` bigint(20) NOT NULL,
  `receiver_id` bigint(20) NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'text',
  `is_read` tinyint(4) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id`) USING BTREE,
  INDEX `idx_receiver_read`(`receiver_id`, `is_read`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '聊天消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of messages
-- ----------------------------
INSERT INTO `messages` VALUES (1, 9, 2, 6, '师傅你好，请问大概什么时候能到？', 'text', 1, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `messages` VALUES (2, 9, 6, 2, '快了快了，还有10分钟', 'text', 1, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `messages` VALUES (3, 9, 2, 6, '好的谢谢，我在图书馆门口等你', 'text', 1, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `messages` VALUES (4, 9, 6, 2, '到了，你在哪个门？', 'text', 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `messages` VALUES (5, 11, 2, 9, '你好，帮我多带一包辣条好吗？', 'text', 1, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `messages` VALUES (6, 11, 9, 2, '好的，辣条放在哪里买？', 'text', 1, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `messages` VALUES (7, 11, 2, 9, '校外那个小卖部就有卖的', 'text', 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);

-- ----------------------------
-- Table structure for notifications
-- ----------------------------
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `related_id` bigint(20) NULL DEFAULT NULL,
  `is_read` tinyint(4) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_read`(`user_id`, `is_read`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '消息通知表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of notifications
-- ----------------------------
INSERT INTO `notifications` VALUES (1, 2, '订单已被接取', '您的订单CH20240317140001已被跑腿员接取，正在配送中', 'order_status', 9, 1, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `notifications` VALUES (2, 6, '新订单提醒', '附近有新订单，任务类型：送物，报酬：¥9.00', 'order_status', 9, 1, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `notifications` VALUES (3, 2, '申诉已提交', '您的申诉已提交，等待管理员处理', 'system', 11, 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `notifications` VALUES (4, 9, '订单申诉通知', '订单CH20240319150001 发布者提起申诉，请配合处理', 'system', 11, 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `notifications` VALUES (5, 3, '订单已取消', '您的订单CH20240318100001已被取消', 'order_status', 10, 1, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `notifications` VALUES (6, 2, '支付成功', '订单CH20240315110001支付成功，等待跑腿员接单', 'payment', 7, 1, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `notifications` VALUES (7, 6, '收益到账', '订单CH20240310130001已完成，¥14.25已到账', 'payment', 6, 1, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `notifications` VALUES (8, 3, '系统公告', '欢迎使用校园帮互助跑腿平台！新用户首单立减2元', 'system', NULL, 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `notifications` VALUES (9, 13, '订单已被接取', '您的订单已被跑腿员接取，正在配送中', 'order_status', 15, 0, '2026-04-29 15:17:27', '2026-04-29 15:17:27', 0);
INSERT INTO `notifications` VALUES (11, 1, '订单已被接取', '您的订单已被跑腿员接取，正在配送中', 'order_status', 76, 0, '2026-05-16 21:10:03', '2026-05-16 21:10:03', 0);
INSERT INTO `notifications` VALUES (12, 1, '跑腿员已送达', '跑腿员已确认送达，请确认收货', 'order_status', 76, 0, '2026-05-16 21:10:53', '2026-05-16 21:10:53', 0);
INSERT INTO `notifications` VALUES (13, 51, '订单已完成', '发布者已确认收货，收益已到账', 'order_status', 76, 0, '2026-05-16 21:11:06', '2026-05-16 21:11:06', 0);
INSERT INTO `notifications` VALUES (14, 51, '订单已被接取', '您的订单已被跑腿员接取，正在配送中', 'order_status', 89, 0, '2026-05-16 21:19:46', '2026-05-16 21:19:46', 0);
INSERT INTO `notifications` VALUES (15, 62, '订单已被接取', '您的订单已被跑腿员接取，正在配送中', 'order_status', 90, 0, '2026-05-16 21:22:48', '2026-05-16 21:22:48', 0);
INSERT INTO `notifications` VALUES (16, 62, '订单已被接取', '您的订单已被跑腿员接取，正在配送中', 'order_status', 91, 0, '2026-05-16 21:22:48', '2026-05-16 21:22:48', 0);
INSERT INTO `notifications` VALUES (17, 62, '跑腿员已送达', '跑腿员已确认送达，请确认收货', 'order_status', 91, 0, '2026-05-16 21:22:48', '2026-05-16 21:22:48', 0);
INSERT INTO `notifications` VALUES (18, 63, '订单已完成', '发布者已确认收货，收益已到账', 'order_status', 91, 0, '2026-05-16 21:22:48', '2026-05-16 21:22:48', 0);
INSERT INTO `notifications` VALUES (19, 64, '订单已被接取', '您的订单已被跑腿员接取，正在配送中', 'order_status', 92, 0, '2026-05-16 21:23:34', '2026-05-16 21:23:34', 0);
INSERT INTO `notifications` VALUES (20, 5, '订单已被接取', '您的订单已被跑腿员接取，正在配送中', 'order_status', 80, 0, '2026-05-17 08:41:46', '2026-05-17 08:41:46', 0);
INSERT INTO `notifications` VALUES (21, 6, '订单已被接取', '您的订单已被跑腿员接取，正在配送中', 'order_status', 81, 0, '2026-05-20 09:15:33', '2026-05-20 09:15:33', 0);
INSERT INTO `notifications` VALUES (22, 3, '申诉已提交', '您的申诉已提交，等待管理员处理', 'system', 22, 0, '2026-05-21 12:00:27', '2026-05-21 12:00:27', 0);
INSERT INTO `notifications` VALUES (23, 3, '申诉已提交', '您的申诉已提交，等待管理员处理', 'system', 31, 0, '2026-05-21 14:53:33', '2026-05-21 14:53:33', 0);
INSERT INTO `notifications` VALUES (24, 3, '订单已取消', '您的订单已取消：用户主动取消', 'order_status', 105, 0, '2026-05-21 15:21:05', '2026-05-21 15:21:05', 0);
INSERT INTO `notifications` VALUES (25, 6, '跑腿员已送达', '跑腿员已确认送达，请确认收货', 'order_status', 81, 0, '2026-05-21 15:34:00', '2026-05-21 15:34:00', 0);

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `publisher_id` bigint(20) NOT NULL,
  `runner_id` bigint(20) NULL DEFAULT NULL,
  `task_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `pickup_location` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `delivery_location` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `pickup_latitude` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `pickup_longitude` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `delivery_latitude` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `delivery_longitude` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `expected_time` datetime NOT NULL,
  `reward` decimal(10, 2) NOT NULL,
  `status` int(11) NOT NULL DEFAULT 0 COMMENT '0: 待支付, 1: 待接单, 2: 进行中, 3: 待确认, 4: 已完成, 5: 已取消, 6: 申诉中',
  `payment_status` tinyint(4) NOT NULL DEFAULT 0,
  `alipay_trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `pay_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付时间',
  `platform_fee` decimal(10, 2) NOT NULL DEFAULT 0.00,
  `runner_earnings` decimal(10, 2) NOT NULL DEFAULT 0.00,
  `accepted_at` datetime NULL DEFAULT NULL,
  `completed_at` datetime NULL DEFAULT NULL,
  `cancelled_at` datetime NULL DEFAULT NULL,
  `publisher_rated` tinyint(4) NOT NULL DEFAULT 0,
  `runner_rated` tinyint(4) NOT NULL DEFAULT 0,
  `publisher_rating` int(11) NOT NULL DEFAULT 0,
  `runner_rating` int(11) NOT NULL DEFAULT 0,
  `publisher_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `runner_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `appeal_status` tinyint(4) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT 0,
  `proof_image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '跑腿员上传的凭证图片URL，多张用逗号分隔',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no`) USING BTREE,
  INDEX `idx_publisher`(`publisher_id`) USING BTREE,
  INDEX `idx_runner`(`runner_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 115 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of orders
-- ----------------------------
INSERT INTO `orders` VALUES (1, 'CH20240301120001', 2, 6, '代取快递', '菜鸟驿站A栋一楼', '宿舍3号楼401室', '30.5728', '104.0668', NULL, NULL, '帮取顺丰快递SF7654321，谢谢', NULL, '2024-03-01 14:00:00', 8.00, 4, 1, NULL, NULL, 0.40, 7.60, '2024-03-01 12:10:00', '2024-03-01 13:50:00', NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-06-07 11:00:04', '2026-06-08 17:17:31', 0, NULL);
INSERT INTO `orders` VALUES (2, 'CH20240302090001', 3, 6, '代购', '校内超市', '宿舍5号楼202室', '30.5720', '104.0660', NULL, NULL, '买一瓶农夫山泉500ml和一包薯片', NULL, '2024-03-02 11:00:00', 6.00, 4, 1, NULL, NULL, 0.30, 5.70, '2024-03-02 09:15:00', '2024-03-02 10:30:00', NULL, 1, 0, 5, 0, '完成的速度快！！', NULL, 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0, NULL);
INSERT INTO `orders` VALUES (3, 'CH20240303150001', 2, 7, '代办', '教务处一楼', '宿舍2号楼305室', '30.5735', '104.0675', NULL, NULL, '帮我领一下成绩单，需要盖章', NULL, '2024-03-03 17:00:00', 12.00, 4, 1, NULL, NULL, 0.60, 11.40, '2024-03-03 15:20:00', '2024-03-03 16:45:00', NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0, NULL);
INSERT INTO `orders` VALUES (4, 'CH20240304100001', 3, 7, '送物', '图书馆正门', '教学楼B201', '30.5740', '104.0670', NULL, NULL, '把我忘在图书馆的充电宝送到教学楼', NULL, '2024-03-04 12:00:00', 10.00, 4, 1, NULL, NULL, 0.50, 9.50, '2024-03-04 10:30:00', '2024-03-04 11:40:00', NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0, NULL);
INSERT INTO `orders` VALUES (5, 'CH20240305140001', 2, 8, '代取快递', '快递柜C区', '宿舍6号楼108室', '30.5722', '104.0662', NULL, NULL, '取京东快递JD123456789', NULL, '2024-03-05 16:00:00', 7.00, 4, 1, NULL, NULL, 0.35, 6.65, '2024-03-05 14:20:00', '2024-03-05 15:50:00', NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0, NULL);
INSERT INTO `orders` VALUES (6, 'CH20240310130001', 3, 6, '代购', '校外麦当劳', '宿舍4号楼512室', '30.5715', '104.0655', NULL, NULL, '一个巨无霸套餐+可乐，不要冰', NULL, '2024-03-10 14:30:00', 15.00, 4, 1, NULL, NULL, 0.75, 14.25, '2024-03-10 13:10:00', '2024-03-10 14:20:00', NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0, NULL);
INSERT INTO `orders` VALUES (7, 'CH20240315110001', 2, NULL, '代取快递', '菜鸟驿站B栋', '宿舍1号楼212室', '30.5728', '104.0668', NULL, NULL, '取一个菜鸟包裹', NULL, '2024-03-15 13:00:00', 8.00, 1, 1, NULL, NULL, 0.40, 7.60, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0, NULL);
INSERT INTO `orders` VALUES (8, 'CH20240316090001', 3, NULL, '代办', '行政楼二楼', '宿舍7号楼601室', '30.5735', '104.0675', NULL, NULL, '帮我办理校园卡补办手续', NULL, '2024-03-16 12:00:00', 20.00, 1, 1, NULL, NULL, 1.00, 19.00, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0, NULL);
INSERT INTO `orders` VALUES (9, 'CH20240317140001', 2, 6, '送物', '实验楼308', '图书馆三楼', '30.5740', '104.0670', NULL, NULL, '送实验报告到图书馆打印', NULL, '2024-03-17 16:00:00', 9.00, 2, 1, NULL, NULL, 0.45, 8.55, '2024-03-17 14:30:00', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0, NULL);
INSERT INTO `orders` VALUES (10, 'CH20240318100001', 3, NULL, '代取快递', '菜鸟驿站A栋', '宿舍3号楼301室', '30.5728', '104.0668', NULL, NULL, '取申通快递', NULL, '2024-03-18 12:00:00', 7.00, 5, 0, NULL, NULL, 0.35, 6.65, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0, NULL);
INSERT INTO `orders` VALUES (11, 'CH20240319150001', 2, 9, '代购', '校外便利店', '宿舍8号楼402室', '30.5715', '104.0655', NULL, NULL, '买两瓶红牛和三包方便面', NULL, '2024-03-19 17:00:00', 10.00, 6, 1, NULL, NULL, 0.50, 9.50, '2024-03-19 15:30:00', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0, NULL);
INSERT INTO `orders` VALUES (12, 'CH20240320080001', 3, NULL, '代办', '医务室', '宿舍2号楼201室', '30.5730', '104.0665', NULL, NULL, '帮我取一下体检报告', NULL, '2024-03-20 10:00:00', 11.00, 0, 0, NULL, NULL, 0.55, 10.45, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0, NULL);
INSERT INTO `orders` VALUES (13, 'CH20240321080001', 11, 19, '代取快递', '菜鸟驿站B区', '宿舍4号楼105室', '30.5725', '104.0665', NULL, NULL, '取中通快递，盒子有点大', NULL, '2024-03-21 10:00:00', 8.00, 4, 1, NULL, NULL, 0.40, 7.60, '2024-03-21 08:15:00', '2024-03-21 09:30:00', NULL, 1, 1, 5, 5, '很快很方便', '谢谢老板', 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (14, 'CH20240321090002', 12, 19, '代购', '校门口奶茶店', '实验楼302', '30.5718', '104.0658', NULL, NULL, '一杯珍珠奶茶，半糖去冰', NULL, '2024-03-21 11:00:00', 6.00, 4, 1, NULL, NULL, 0.30, 5.70, '2024-03-21 09:05:00', '2024-03-21 10:20:00', NULL, 1, 1, 4, 5, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (15, 'CH20240321100003', 13, 3, '代办', '校医院', '宿舍5号楼308', '30.5732', '104.0672', NULL, NULL, '帮我拿一下体检报告', NULL, '2024-03-21 14:00:00', 10.00, 2, 1, NULL, NULL, 0.50, 9.50, '2026-04-29 15:17:27', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (16, 'CH20240321110004', 14, 4, '送物', '东门快递柜', '图书馆二楼', '30.5742', '104.0660', NULL, NULL, '送一本书给同学', NULL, '2024-03-21 12:00:00', 7.00, 2, 1, NULL, NULL, 0.35, 6.65, '2024-03-21 11:10:00', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (17, 'CH20240321120005', 15, 3, '代取快递', '京东快递柜', '宿舍2号楼205', '30.5720', '104.0662', NULL, NULL, '取大件行李', NULL, '2024-03-21 16:00:00', 12.00, 4, 1, NULL, NULL, 0.60, 11.40, '2024-03-21 12:20:00', '2024-03-21 13:40:00', NULL, 1, 1, 5, 4, '师傅帮送上楼，非常赞', '不客气', 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (18, 'CH20240321130006', 16, 3, '代购', '西门超市', '计算机楼508', '30.5712', '104.0652', NULL, NULL, '两瓶矿泉水，一包纸巾', NULL, '2024-03-21 14:30:00', 8.00, 4, 1, NULL, NULL, 0.40, 7.60, '2024-03-21 13:10:00', '2024-03-21 14:20:00', NULL, 1, 0, 5, 0, '准时送到，非常好', NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (19, 'CH20240322080007', 17, 13, '代取快递', '菜鸟驿站C区', '宿舍6号楼203', '30.5726', '104.0666', NULL, NULL, '取顺丰包裹，重量大概5kg', NULL, '2024-03-22 10:00:00', 10.00, 1, 1, NULL, NULL, 0.50, 9.50, '2024-03-22 08:15:00', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (20, 'CH20240322090008', 18, 13, '送物', '行政楼门口', '数学楼203', '30.5738', '104.0678', NULL, NULL, '送一份材料给老师', NULL, '2024-03-22 11:00:00', 9.00, 2, 1, NULL, NULL, 0.45, 8.55, '2024-03-22 09:05:00', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (21, 'CH20240322100009', 2, 9, '代办', '教务处', '宿舍3号楼405', '30.5730', '104.0670', NULL, NULL, '帮我打印成绩单盖章', NULL, '2024-03-22 14:00:00', 15.00, 1, 1, NULL, NULL, 0.75, 14.25, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (22, 'CH20240322110010', 3, 4, '代购', '小吃街', '图书馆一楼', '30.5710', '104.0650', NULL, NULL, '两份炒饭，打包带走', NULL, '2024-03-22 12:30:00', 12.00, 6, 1, NULL, NULL, 0.60, 11.40, '2024-03-22 11:10:00', '2024-03-22 12:15:00', NULL, 1, 1, 4, 4, '味道不错，谢谢', '好评', 1, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (23, 'CH20240323080011', 4, 13, '代取快递', '圆通快递网点', '宿舍7号楼102', '30.5724', '104.0664', NULL, NULL, '取一个大箱子，可能需要帮忙抬', NULL, '2024-03-23 10:00:00', 14.00, 4, 1, NULL, NULL, 0.70, 13.30, '2024-03-23 08:20:00', '2024-03-23 09:30:00', NULL, 1, 1, 5, 5, '帮抬上楼太感谢了', '应该的', 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (24, 'CH20240323090012', 5, 4, '代购', '奶茶店', '计算机学院会议室', '30.5716', '104.0656', NULL, NULL, '五杯珍珠奶茶，三分糖', NULL, '2024-03-23 11:00:00', 20.00, 4, 1, NULL, NULL, 1.00, 19.00, '2024-03-23 09:05:00', '2024-03-23 10:20:00', NULL, 1, 1, 5, 5, '准时送到会议室，很棒', '谢谢', 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (25, 'CH20240323100013', 6, NULL, '代取快递', '菜鸟驿站A区', '宿舍8号楼406', '30.5729', '104.0669', NULL, NULL, '取两个包裹', NULL, '2024-03-23 15:00:00', 9.00, 1, 1, NULL, NULL, 0.45, 8.55, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (26, 'CH20240323110014', 7, 9, '送物', '宿舍1号楼', '实验室B305', '30.5745', '104.0675', NULL, NULL, '送实验器材', NULL, '2024-03-23 12:00:00', 11.00, 2, 1, NULL, NULL, 0.55, 10.45, '2024-03-23 11:05:00', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (27, 'CH20240323120015', 8, NULL, '代办', '学生处', '宿舍5号楼208', '30.5736', '104.0676', NULL, NULL, '帮我交一份材料', NULL, '2024-03-23 16:00:00', 8.00, 0, 0, NULL, NULL, 0.40, 7.60, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (28, 'CH20240324080016', 9, 13, '代购', '食堂三楼', '图书馆五楼', '30.5715', '104.0655', NULL, NULL, '两份套餐带走', NULL, '2024-03-24 11:00:00', 15.00, 6, 1, NULL, NULL, 0.75, 14.25, '2024-03-24 08:15:00', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 1, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (29, 'CH20240324090017', 10, 3, '代取快递', '顺丰网点', '宿舍1号楼502', '30.5721', '104.0661', NULL, NULL, '取文件快递', NULL, '2024-03-24 10:00:00', 6.00, 4, 1, NULL, NULL, 0.30, 5.70, '2024-03-24 09:05:00', '2024-03-24 09:50:00', NULL, 1, 1, 5, 5, '很快拿到了', NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (30, 'CH20240324100018', 2, 9, '送物', '北门', '东门', '30.5748', '104.0648', NULL, NULL, '把书包送到东门给朋友', NULL, '2024-03-24 12:00:00', 5.00, 4, 1, NULL, NULL, 0.25, 4.75, '2024-03-24 10:10:00', '2024-03-24 10:45:00', NULL, 1, 1, 5, 4, '准时送到', '不客气', 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (31, 'CH20240324110019', 3, 4, '代购', '校门口水果店', '宿舍4号楼301', '30.5714', '104.0654', NULL, NULL, '三斤苹果，一斤香蕉', NULL, '2024-03-24 14:30:00', 10.00, 6, 1, NULL, NULL, 0.50, 9.50, '2024-03-24 11:40:00', '2024-03-24 12:30:00', NULL, 1, 1, 4, 5, '水果很新鲜，帮忙送上楼，点赞', '谢谢', 1, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (32, 'CH20240325080020', 4, 13, '代取快递', '菜鸟驿站D区', '宿舍2号楼306', '30.5723', '104.0663', NULL, NULL, '取三个小包裹', NULL, '2024-03-25 10:00:00', 10.00, 1, 1, NULL, NULL, 0.50, 9.50, '2024-03-25 08:20:00', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (33, 'CH20240325090021', 5, 3, '代办', '财务处', '办公楼203', '30.5734', '104.0674', NULL, NULL, '帮我交学费发票', NULL, '2024-03-25 11:00:00', 8.00, 4, 1, NULL, NULL, 0.40, 7.60, '2024-03-25 09:05:00', '2024-03-25 10:15:00', NULL, 1, 0, 4, 0, '办好啦，谢谢', NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (34, 'CH20240325100022', 6, 9, '代购', '校外便利店', '体育馆', '30.5719', '104.0659', NULL, NULL, '两瓶功能饮料，一包纸巾', NULL, '2024-03-25 14:00:00', 9.00, 2, 1, NULL, NULL, 0.45, 8.55, '2024-03-25 10:30:00', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (35, 'CH20240325110023', 7, 13, '送物', '正门', '实验楼405', '30.5746', '104.0669', NULL, NULL, '送钥匙给同学', NULL, '2024-03-25 12:30:00', 6.00, 4, 1, NULL, NULL, 0.30, 5.70, '2024-03-25 11:10:00', '2024-03-25 12:00:00', NULL, 1, 1, 5, 5, '非常及时，救了命了', '哈哈没事', 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (36, 'CH20240325120024', 8, NULL, '代取快递', '邮政支局', '宿舍9号楼101', '30.5727', '104.0667', NULL, NULL, '取包裹', NULL, '2024-03-25 16:00:00', 7.00, 0, 1, NULL, NULL, 0.35, 6.65, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (37, 'CH20240326080025', 11, 3, '代购', '面包店', '宿舍6号楼302', '30.5717', '104.0657', NULL, NULL, '四个面包当早餐', NULL, '2024-03-26 09:00:00', 10.00, 4, 1, NULL, NULL, 0.50, 9.50, '2024-03-26 08:05:00', '2024-03-26 08:45:00', NULL, 1, 1, 5, 4, '面包新鲜，很好', '谢谢', 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (38, 'CH20240326090026', 12, 4, '代办', '学院办公室', '宿舍5号楼103', '30.5731', '104.0671', NULL, NULL, '帮我领一下教材', NULL, '2024-03-26 10:00:00', 12.00, 4, 1, NULL, NULL, 0.60, 11.40, '2024-03-26 08:15:00', '2024-03-26 09:20:00', NULL, 1, 1, 4, 5, '很快领到了，谢谢', '不客气', 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (39, 'CH20240326100027', 9, 13, '送物', '校门', '研究生公寓3号楼', '30.5741', '104.0671', NULL, NULL, '送电脑给同学', NULL, '2024-03-26 11:30:00', 8.00, 4, 1, NULL, NULL, 0.40, 7.60, '2024-03-26 09:40:00', '2024-03-26 10:30:00', NULL, 1, 1, 5, 5, '安全送到，完美', '满意', 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (40, 'CH20240326110028', 10, 9, '代购', '超市', '图书馆自习室', '30.5713', '104.0653', NULL, NULL, '一瓶水，一块巧克力', NULL, '2024-03-26 14:00:00', 7.00, 4, 1, NULL, NULL, 0.35, 6.65, '2024-03-26 11:45:00', '2024-03-26 12:30:00', NULL, 1, 1, 5, 5, '很快送到，很棒', '谢谢', 0, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0, NULL);
INSERT INTO `orders` VALUES (41, 'CH202605091726208808', 3, NULL, '代取快递', '三元里广州市白云区人民政府(广园中路南) 广东省广州市白云区广园中路238号', '广州卫生职业技术学院(广园南校区) 广东省广州市白云区广园中路248号', '23.15787561040057', '113.27348676322934', '23.156491', '113.275069', '归还图书202房', '', '2026-05-11 19:30:00', 10.00, 1, 1, NULL, NULL, 0.50, 9.50, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-09 17:26:21', '2026-05-09 17:26:21', 0, NULL);
INSERT INTO `orders` VALUES (42, 'CH1778914892', 1, NULL, '代取快递', '菜鸟驿站A区', '宿舍3号楼502', '30.5721', '113.2735', '30.5728', '113.2742', '顺丰快递，大件包裹，凭取件码123456取件', NULL, '2026-05-16 17:01:32', 8.00, 1, 1, NULL, NULL, 0.40, 7.60, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:01:32', '2026-05-16 15:04:17', 1, NULL);
INSERT INTO `orders` VALUES (44, 'CH1778915057', 1, NULL, '代取快递', '菜鸟驿站A区', '宿舍3号楼502', '30.5721', '113.2735', '30.5728', '113.2742', '顺丰快递，大件包裹，凭取件码123456取件', NULL, '2026-05-16 17:04:18', 8.00, 1, 1, NULL, NULL, 0.40, 7.60, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:04:18', '2026-05-16 15:07:01', 1, NULL);
INSERT INTO `orders` VALUES (46, 'TEST17789152212871', 1, NULL, '代取快递', '菜鸟驿站A区', '宿舍3号楼502', '30.5721', '113.2735', '30.5728', '113.2742', '顺丰快递，大件包裹，凭取件码123456取件', NULL, '2026-05-16 17:07:01', 8.00, 1, 1, NULL, NULL, 0.40, 7.60, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:07:01', '2026-05-16 15:09:51', 1, NULL);
INSERT INTO `orders` VALUES (47, 'TEST17789152213062', 1, 2, '代购', '学校超市', '图书馆3楼阅览室', '30.5721', '113.2735', '30.5728', '113.2742', '两瓶矿泉水、一包纸巾', NULL, '2026-05-16 17:07:01', 6.00, 2, 1, NULL, NULL, 0.30, 5.70, '2026-05-16 14:37:01', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:07:01', '2026-05-16 15:09:51', 1, NULL);
INSERT INTO `orders` VALUES (48, 'TEST17789153912081', 1, NULL, '代取快递', '菜鸟驿站A区', '宿舍3号楼502', '30.5721', '113.2735', '30.5728', '113.2742', '顺丰快递，大件包裹，凭取件码123456取件', NULL, '2026-05-16 17:09:51', 8.00, 1, 1, NULL, NULL, 0.40, 7.60, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:09:51', '2026-05-16 15:13:34', 1, NULL);
INSERT INTO `orders` VALUES (49, 'TEST17789153912302', 1, 2, '代购', '学校超市', '图书馆3楼阅览室', '30.5721', '113.2735', '30.5728', '113.2742', '两瓶矿泉水、一包纸巾', NULL, '2026-05-16 17:09:51', 6.00, 2, 1, NULL, NULL, 0.30, 5.70, '2026-05-16 14:39:51', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:09:51', '2026-05-16 15:13:34', 1, NULL);
INSERT INTO `orders` VALUES (50, 'TEST17789153912353', 1, 3, '代办', '教务处', '宿舍3号楼502', '30.5721', '113.2735', '30.5728', '113.2742', '帮忙打印成绩单并盖章', NULL, '2026-05-16 17:09:51', 12.00, 4, 1, NULL, NULL, 0.60, 11.40, '2026-05-16 14:39:51', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:09:51', '2026-05-16 15:13:34', 1, NULL);
INSERT INTO `orders` VALUES (51, 'TEST17789153912434', 2, 1, '代取快递', '京东自提点', '宿舍5号楼201', '30.5721', '113.2735', '30.5728', '113.2742', '京东快递，取件码654321，送到宿舍', NULL, '2026-05-16 17:09:51', 7.00, 2, 1, NULL, NULL, 0.35, 6.65, '2026-05-16 14:39:51', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:09:51', '2026-05-16 15:13:34', 1, NULL);
INSERT INTO `orders` VALUES (52, 'TEST17789153912485', 5, NULL, '代取快递', '顺丰快递站', '宿舍2号楼305', '30.5721', '113.2735', '30.5728', '113.2742', '顺丰快递，凭取件码789012取件，送到宿舍', NULL, '2026-05-16 17:09:51', 9.00, 1, 1, NULL, NULL, 0.45, 8.55, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:09:51', '2026-05-16 15:09:51', 0, NULL);
INSERT INTO `orders` VALUES (53, 'TEST17789153912526', 6, NULL, '代购', '食堂三楼', '研究生公寓1号楼', '30.5721', '113.2735', '30.5728', '113.2742', '一份糖醋排骨饭，微辣，送到宿舍门口', NULL, '2026-05-16 17:09:51', 15.00, 1, 1, NULL, NULL, 0.75, 14.25, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:09:51', '2026-05-16 15:09:51', 0, NULL);
INSERT INTO `orders` VALUES (54, 'TEST17789153912557', 7, NULL, '代办', '图书馆', '宿舍4号楼102', '30.5721', '113.2735', '30.5728', '113.2742', '帮忙还3本书到图书馆', NULL, '2026-05-16 17:09:51', 6.00, 1, 1, NULL, NULL, 0.30, 5.70, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:09:51', '2026-05-16 15:09:51', 0, NULL);
INSERT INTO `orders` VALUES (55, 'TEST17789156144211', 1, NULL, '代取快递', '菜鸟驿站A区', '宿舍3号楼502', '30.5721', '113.2735', '30.5728', '113.2742', '顺丰快递，大件包裹，凭取件码123456取件', NULL, '2026-05-16 17:13:34', 8.00, 1, 1, NULL, NULL, 0.40, 7.60, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:13:34', '2026-05-16 15:16:09', 1, NULL);
INSERT INTO `orders` VALUES (56, 'TEST17789156144522', 1, 2, '代购', '学校超市', '图书馆3楼阅览室', '30.5721', '113.2735', '30.5728', '113.2742', '两瓶矿泉水、一包纸巾', NULL, '2026-05-16 17:13:34', 6.00, 2, 1, NULL, NULL, 0.30, 5.70, '2026-05-16 14:43:34', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:13:34', '2026-05-16 15:16:09', 1, NULL);
INSERT INTO `orders` VALUES (57, 'TEST17789156144623', 1, 3, '代办', '教务处', '宿舍3号楼502', '30.5721', '113.2735', '30.5728', '113.2742', '帮忙打印成绩单并盖章', NULL, '2026-05-16 17:13:34', 12.00, 4, 1, NULL, NULL, 0.60, 11.40, '2026-05-16 14:43:34', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:13:34', '2026-05-16 15:16:09', 1, NULL);
INSERT INTO `orders` VALUES (58, 'TEST17789156144684', 2, 1, '代取快递', '京东自提点', '宿舍5号楼201', '30.5721', '113.2735', '30.5728', '113.2742', '京东快递，取件码654321，送到宿舍', NULL, '2026-05-16 17:13:34', 7.00, 2, 1, NULL, NULL, 0.35, 6.65, '2026-05-16 14:43:34', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:13:34', '2026-05-16 15:16:09', 1, NULL);
INSERT INTO `orders` VALUES (59, 'TEST17789156144785', 5, NULL, '代取快递', '顺丰快递站', '宿舍2号楼305', '30.5721', '113.2735', '30.5728', '113.2742', '顺丰快递，凭取件码789012取件，送到宿舍', NULL, '2026-05-16 17:13:34', 9.00, 1, 1, NULL, NULL, 0.45, 8.55, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:13:34', '2026-05-16 15:13:34', 0, NULL);
INSERT INTO `orders` VALUES (60, 'TEST17789156144826', 6, NULL, '代购', '食堂三楼', '研究生公寓1号楼', '30.5721', '113.2735', '30.5728', '113.2742', '一份糖醋排骨饭，微辣，送到宿舍门口', NULL, '2026-05-16 17:13:34', 15.00, 1, 1, NULL, NULL, 0.75, 14.25, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:13:34', '2026-05-16 15:13:34', 0, NULL);
INSERT INTO `orders` VALUES (61, 'TEST17789156144907', 7, NULL, '代办', '图书馆', '宿舍4号楼102', '30.5721', '113.2735', '30.5728', '113.2742', '帮忙还3本书到图书馆', NULL, '2026-05-16 17:13:34', 6.00, 1, 1, NULL, NULL, 0.30, 5.70, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:13:34', '2026-05-16 15:13:34', 0, NULL);
INSERT INTO `orders` VALUES (62, 'TEST17789157697961', 1, NULL, '代取快递', '菜鸟驿站A区', '宿舍3号楼502', '30.5721', '113.2735', '30.5728', '113.2742', '顺丰快递，大件包裹，凭取件码123456取件', NULL, '2026-05-16 17:16:10', 8.00, 1, 1, NULL, NULL, 0.40, 7.60, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:16:10', '2026-05-16 15:20:21', 1, NULL);
INSERT INTO `orders` VALUES (63, 'TEST17789157698222', 1, 2, '代购', '学校超市', '图书馆3楼阅览室', '30.5721', '113.2735', '30.5728', '113.2742', '两瓶矿泉水、一包纸巾', NULL, '2026-05-16 17:16:10', 6.00, 2, 1, NULL, NULL, 0.30, 5.70, '2026-05-16 14:46:10', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:16:10', '2026-05-16 15:20:21', 1, NULL);
INSERT INTO `orders` VALUES (64, 'TEST17789157698273', 1, 3, '代办', '教务处', '宿舍3号楼502', '30.5721', '113.2735', '30.5728', '113.2742', '帮忙打印成绩单并盖章', NULL, '2026-05-16 17:16:10', 12.00, 4, 1, NULL, NULL, 0.60, 11.40, '2026-05-16 14:46:10', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:16:10', '2026-05-16 15:20:21', 1, NULL);
INSERT INTO `orders` VALUES (65, 'TEST17789157698314', 2, 1, '代取快递', '京东自提点', '宿舍5号楼201', '30.5721', '113.2735', '30.5728', '113.2742', '京东快递，取件码654321，送到宿舍', NULL, '2026-05-16 17:16:10', 7.00, 2, 1, NULL, NULL, 0.35, 6.65, '2026-05-16 14:46:10', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:16:10', '2026-05-16 15:20:21', 1, NULL);
INSERT INTO `orders` VALUES (66, 'TEST17789157698435', 5, NULL, '代取快递', '顺丰快递站', '宿舍2号楼305', '30.5721', '113.2735', '30.5728', '113.2742', '顺丰快递，凭取件码789012取件，送到宿舍', NULL, '2026-05-16 17:16:10', 9.00, 1, 1, NULL, NULL, 0.45, 8.55, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:16:10', '2026-05-16 15:16:10', 0, NULL);
INSERT INTO `orders` VALUES (67, 'TEST17789157698486', 6, NULL, '代购', '食堂三楼', '研究生公寓1号楼', '30.5721', '113.2735', '30.5728', '113.2742', '一份糖醋排骨饭，微辣，送到宿舍门口', NULL, '2026-05-16 17:16:10', 15.00, 1, 1, NULL, NULL, 0.75, 14.25, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:16:10', '2026-05-16 15:16:10', 0, NULL);
INSERT INTO `orders` VALUES (68, 'TEST17789157698577', 7, NULL, '代办', '图书馆', '宿舍4号楼102', '30.5721', '113.2735', '30.5728', '113.2742', '帮忙还3本书到图书馆', NULL, '2026-05-16 17:16:10', 6.00, 1, 1, NULL, NULL, 0.30, 5.70, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:16:10', '2026-05-16 15:16:10', 0, NULL);
INSERT INTO `orders` VALUES (69, 'TEST17789160210261', 1, NULL, '代取快递', '菜鸟驿站A区', '宿舍3号楼502', '30.5721', '113.2735', '30.5728', '113.2742', '顺丰快递，大件包裹，凭取件码123456取件', NULL, '2026-05-16 17:20:21', 8.00, 1, 1, NULL, NULL, 0.40, 7.60, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:20:21', '2026-05-16 15:21:31', 1, NULL);
INSERT INTO `orders` VALUES (70, 'TEST17789160210472', 1, 2, '代购', '学校超市', '图书馆3楼阅览室', '30.5721', '113.2735', '30.5728', '113.2742', '两瓶矿泉水、一包纸巾', NULL, '2026-05-16 17:20:21', 6.00, 2, 1, NULL, NULL, 0.30, 5.70, '2026-05-16 14:50:21', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:20:21', '2026-05-16 15:21:31', 1, NULL);
INSERT INTO `orders` VALUES (71, 'TEST17789160210523', 1, 3, '代办', '教务处', '宿舍3号楼502', '30.5721', '113.2735', '30.5728', '113.2742', '帮忙打印成绩单并盖章', NULL, '2026-05-16 17:20:21', 12.00, 4, 1, NULL, NULL, 0.60, 11.40, '2026-05-16 14:50:21', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:20:21', '2026-05-16 15:21:31', 1, NULL);
INSERT INTO `orders` VALUES (72, 'TEST17789160210554', 2, 1, '代取快递', '京东自提点', '宿舍5号楼201', '30.5721', '113.2735', '30.5728', '113.2742', '京东快递，取件码654321，送到宿舍', NULL, '2026-05-16 17:20:21', 7.00, 2, 1, NULL, NULL, 0.35, 6.65, '2026-05-16 14:50:21', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:20:21', '2026-05-16 15:21:31', 1, NULL);
INSERT INTO `orders` VALUES (73, 'TEST17789160210555', 5, NULL, '代取快递', '顺丰快递站', '宿舍2号楼305', '30.5721', '113.2735', '30.5728', '113.2742', '顺丰快递，凭取件码789012取件，送到宿舍', NULL, '2026-05-16 17:20:21', 9.00, 1, 1, NULL, NULL, 0.45, 8.55, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:20:21', '2026-05-16 15:20:21', 0, NULL);
INSERT INTO `orders` VALUES (74, 'TEST17789160210556', 6, NULL, '代购', '食堂三楼', '研究生公寓1号楼', '30.5721', '113.2735', '30.5728', '113.2742', '一份糖醋排骨饭，微辣，送到宿舍门口', NULL, '2026-05-16 17:20:21', 15.00, 1, 1, NULL, NULL, 0.75, 14.25, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:20:21', '2026-05-16 15:20:21', 0, NULL);
INSERT INTO `orders` VALUES (75, 'TEST17789160210797', 7, NULL, '代办', '图书馆', '宿舍4号楼102', '30.5721', '113.2735', '30.5728', '113.2742', '帮忙还3本书到图书馆', NULL, '2026-05-16 17:20:21', 6.00, 1, 1, NULL, NULL, 0.30, 5.70, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:20:21', '2026-05-16 15:20:21', 0, NULL);
INSERT INTO `orders` VALUES (76, 'TEST17789160911281', 1, 51, '代取快递', '菜鸟驿站A区', '宿舍3号楼502', '30.5721', '113.2735', '30.5728', '113.2742', '顺丰快递，大件包裹，凭取件码123456取件', NULL, '2026-05-16 17:21:31', 8.00, 4, 1, NULL, NULL, 0.40, 7.60, '2026-05-16 21:10:03', '2026-05-16 21:11:06', NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:21:31', '2026-05-16 15:21:31', 0, NULL);
INSERT INTO `orders` VALUES (77, 'TEST17789160911382', 1, 2, '代购', '学校超市', '图书馆3楼阅览室', '30.5721', '113.2735', '30.5728', '113.2742', '两瓶矿泉水、一包纸巾', NULL, '2026-05-16 17:21:31', 6.00, 2, 1, NULL, NULL, 0.30, 5.70, '2026-05-16 14:51:31', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:21:31', '2026-05-16 15:21:31', 0, NULL);
INSERT INTO `orders` VALUES (78, 'TEST17789160911413', 1, 3, '代办', '教务处', '宿舍3号楼502', '30.5721', '113.2735', '30.5728', '113.2742', '帮忙打印成绩单并盖章', NULL, '2026-05-16 17:21:31', 12.00, 4, 1, NULL, NULL, 0.60, 11.40, '2026-05-16 14:51:31', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:21:31', '2026-05-16 15:21:31', 0, NULL);
INSERT INTO `orders` VALUES (79, 'TEST17789160911444', 2, 1, '代取快递', '京东自提点', '宿舍5号楼201', '30.5721', '113.2735', '30.5728', '113.2742', '京东快递，取件码654321，送到宿舍', NULL, '2026-05-16 17:21:31', 7.00, 2, 1, NULL, NULL, 0.35, 6.65, '2026-05-16 14:51:31', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:21:31', '2026-05-16 15:21:31', 0, NULL);
INSERT INTO `orders` VALUES (80, 'TEST17789160911485', 5, 74, '代取快递', '顺丰快递站', '宿舍2号楼305', '30.5721', '113.2735', '30.5728', '113.2742', '顺丰快递，凭取件码789012取件，送到宿舍', NULL, '2026-05-16 17:21:31', 9.00, 2, 1, NULL, NULL, 0.45, 8.55, '2026-05-17 08:41:46', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:21:31', '2026-05-17 08:41:46', 0, NULL);
INSERT INTO `orders` VALUES (81, 'TEST17789160911516', 6, 3, '代购', '食堂三楼', '研究生公寓1号楼', '30.5721', '113.2735', '30.5728', '113.2742', '一份糖醋排骨饭，微辣，送到宿舍门口', NULL, '2026-05-16 17:21:31', 15.00, 4, 1, NULL, NULL, 0.75, 14.25, '2026-05-20 09:15:33', '2026-05-27 09:02:12', NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:21:31', '2026-05-20 09:15:32', 0, 'http://localhost:8080/api/files/orders/2026/05/21/19ce6ad400a04ca3938a482d30de6502.png');
INSERT INTO `orders` VALUES (82, 'TEST17789160911537', 7, NULL, '代办', '图书馆', '宿舍4号楼102', '30.5721', '113.2735', '30.5728', '113.2742', '帮忙还3本书到图书馆', NULL, '2026-05-16 17:21:31', 6.00, 1, 1, NULL, NULL, 0.30, 5.70, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-16 14:21:31', '2026-05-16 15:21:31', 0, NULL);
INSERT INTO `orders` VALUES (103, 'CH202605181528385050', 3, NULL, '送物', '广州科技职业技术大学(广州校区)10座公寓 广东省广州市黄埔区广从九路1038号广州科技职业技术大学(广州校区)', '广州科技职业技术大学(广州校区)3号饭堂 广东省广州市白云区钟落潭镇马沥村广从九路1038号广州科技职业技术大学(广州校区)', '23.400825', '113.469116', '23.400976', '113.466336', '送饭到3号饭堂腊味店', '', '2026-05-18 20:30:00', 5.00, 5, 0, NULL, NULL, 0.25, 4.75, NULL, NULL, '2026-05-18 16:12:58', 0, 0, 0, 0, NULL, NULL, 0, '2026-05-18 15:28:39', '2026-05-18 15:28:39', 0, NULL);
INSERT INTO `orders` VALUES (104, 'CH202605181619364684', 3, NULL, '代购', '广州科技职业技术大学(广州校区)6号饭堂 广东省广州市白云区钟落潭镇科技职业大学宇通充电站东北侧70米', '广州科技职业技术大学(广州校区)10座公寓 广东省广州市黄埔区广从九路1038号广州科技职业技术大学(广州校区)', '23.401038', '113.467586', '23.400825', '113.469116', '买饭6号饭堂', '', '2026-05-18 19:10:00', 5.00, 5, 0, NULL, NULL, 0.25, 4.75, NULL, NULL, '2026-05-18 16:49:38', 0, 0, 0, 0, NULL, NULL, 0, '2026-05-18 16:19:37', '2026-05-18 16:19:37', 0, NULL);
INSERT INTO `orders` VALUES (105, 'CH202605182308464263', 3, NULL, '代购', '广州科技职业技术大学(广州校区)6号饭堂 广东省广州市白云区钟落潭镇科技职业大学宇通充电站东北侧70米', '广州科技职业技术大学(广州校区)10座公寓 广东省广州市黄埔区广从九路1038号广州科技职业技术大学(广州校区)', '23.401038', '113.467586', '23.400825', '113.469116', '买饭6号饭堂', '', '2026-05-19 19:00:00', 5.00, 5, 2, '2026051822001447410508727663', '2026-05-18 23:09:46', 0.25, 4.75, NULL, NULL, '2026-05-21 15:21:05', 0, 0, 0, 0, NULL, NULL, 0, '2026-05-18 23:08:47', '2026-05-18 23:08:47', 0, NULL);
INSERT INTO `orders` VALUES (106, 'CH202605211521288764', 3, NULL, '代购', '广州科技职业技术大学(广州校区)6号饭堂 广东省广州市白云区钟落潭镇科技职业大学宇通充电站东北侧70米', '广州科技职业技术大学(广州校区)10座公寓 广东省广州市黄埔区广从九路1038号广州科技职业技术大学(广州校区)', '23.401038', '113.467586', '23.400825', '113.469116', '买饭6号饭堂', '', '2026-05-21 19:00:00', 5.00, 1, 1, NULL, NULL, 0.25, 4.75, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-21 15:21:29', '2026-05-21 15:21:29', 0, NULL);
INSERT INTO `orders` VALUES (107, 'CH202605270912124164', 3, NULL, '送物', '广州科技职业技术大学(广州校区) 广东省广州市白云区永九快速路', '广科大健身 广东省广州市黄埔区永九快速路', '23.401038', '113.467586', '23.400255', '113.468072', '送哑铃1546', '', '2026-07-22 19:00:00', 8.00, 5, 0, NULL, NULL, 0.40, 7.60, NULL, NULL, '2026-05-27 09:42:13', 0, 0, 0, 0, NULL, NULL, 0, '2026-05-27 09:12:12', '2026-05-27 09:12:12', 0, NULL);
INSERT INTO `orders` VALUES (108, 'CH202605270930273814', 3, NULL, '代取快递', '广州市白云区人民政府 广东省广州市白云区广园中路238号', '广州中医药大学(三元里校区) 广东省广州市白云区女贞路', '23.15792', '113.27324', '23.157485967728444', '113.26881971954343', '快递0325', '', '2026-05-27 11:30:02', 8.00, 0, 0, NULL, NULL, 0.40, 7.60, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-27 09:30:28', '2026-05-27 09:30:28', 0, NULL);
INSERT INTO `orders` VALUES (109, 'CH202605270952557958', 3, NULL, '代取快递', '新惠爱(松柏东街店) 广东省广州市白云区三元里街道广园中路松柏东街11号1楼', '嗨鹅电竞·宽大店 广东省广州市白云区三元里街道松柏北街1号二楼', '23.160495', '113.266894', '23.162544', '113.264441', '快递565', '', '2026-05-27 11:52:28', 5.00, 1, 1, '2026052722001447410508770006', '2026-05-27 10:08:51', 0.25, 4.75, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-27 09:52:56', '2026-05-27 09:52:56', 0, NULL);
INSERT INTO `orders` VALUES (110, 'CH202605271215250479', 3, NULL, '送物', '白云山风景名胜区 广东省广州市白云区麓景路', '广州市广骏旅游汽车企业集团有限公司 广东省广州市白云区飞鹅路2号办公楼', '23.15444770245158', '113.27624407409665', '23.149395', '113.26734', '送货1545', '', '2026-05-27 14:14:44', 8.00, 1, 1, '2026052722001447410508771335', '2026-05-27 12:23:12', 0.40, 7.60, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-27 12:15:27', '2026-05-27 12:15:27', 0, NULL);
INSERT INTO `orders` VALUES (111, 'CH202605282206418319', 3, NULL, '代购', '广州科技职业技术大学(广州校区)6号饭堂 广东省广州市白云区钟落潭镇科技职业大学宇通充电站东北侧70米', '广州科技职业技术大学(广州校区)10座公寓 广东省广州市黄埔区广从九路1038号广州科技职业技术大学(广州校区)', '23.401038', '113.467586', '23.400825', '113.469116', '买饭6号饭堂', '', '2026-05-29 19:10:00', 5.00, 1, 1, NULL, NULL, 0.25, 4.75, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-05-28 22:06:42', '2026-05-28 22:06:42', 0, NULL);
INSERT INTO `orders` VALUES (112, 'CH202605282208292574', 3, NULL, '代购', '广州科技职业技术大学(广州校区)6号饭堂 广东省广州市白云区钟落潭镇科技职业大学宇通充电站东北侧70米', '广州科技职业技术大学(广州校区)10座公寓 广东省广州市黄埔区广从九路1038号广州科技职业技术大学(广州校区)', '23.401038', '113.467586', '23.400825', '113.469116', '买饭6号饭堂', '', '2026-05-29 19:00:00', 5.00, 5, 0, NULL, NULL, 0.25, 4.75, NULL, NULL, '2026-05-29 09:13:04', 0, 0, 0, 0, NULL, NULL, 0, '2026-05-28 22:08:30', '2026-05-28 22:08:30', 0, NULL);
INSERT INTO `orders` VALUES (113, 'ADMIN1781513733320', 2, NULL, '代取快递', 'a', 'b', NULL, NULL, NULL, NULL, 'test', '', '2026-06-15 18:55:28', 8.00, 0, 0, NULL, NULL, 0.00, 8.00, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-06-15 16:55:33', '2026-06-15 16:55:33', 0, NULL);
INSERT INTO `orders` VALUES (114, 'ADMIN1781513822968', 2, NULL, '代取快递', '东莞城市学院3B栋501', '东莞城市学院7栋511', NULL, NULL, NULL, NULL, '送教材', '', '2026-06-15 18:56:34', 8.00, 0, 0, NULL, NULL, 0.00, 8.00, NULL, NULL, NULL, 0, 0, 0, 0, NULL, NULL, 0, '2026-06-15 16:57:02', '2026-06-15 16:57:02', 0, NULL);

-- ----------------------------
-- Table structure for permissions
-- ----------------------------
DROP TABLE IF EXISTS `permissions`;
CREATE TABLE `permissions`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `permission_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '权限名称',
  `permission_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '权限编码，如：admin:dashboard:view',
  `resource_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '资源类型：menu, button, api',
  `resource_path` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '资源路径或API路径',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限描述',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_permission_code`(`permission_code`) USING BTREE,
  INDEX `idx_resource_type`(`resource_type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of permissions
-- ----------------------------
INSERT INTO `permissions` VALUES (1, '查看数据大屏', 'admin:dashboard:view', 'menu', '/dashboard', '查看运营数据大屏', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (2, '查看订单趋势', 'admin:dashboard:order-trend', 'api', '/admin/dashboard', 'API：查看订单趋势', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (3, '查看订单列表', 'admin:order:list', 'menu', '/orders', '查看订单列表', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (4, '查看订单详情', 'admin:order:detail', 'api', '/admin/orders/*', 'API：查看订单详情', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (5, '处理订单', 'admin:order:handle', 'button', 'order-handle', '按钮：处理订单', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (6, '查看用户列表', 'admin:user:list', 'menu', '/users', '查看用户列表', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (7, '冻结解冻用户', 'admin:user:freeze', 'button', 'user-freeze', '按钮：冻结/解冻用户', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (8, '调整信用分', 'admin:user:credit', 'api', '/admin/users/*/credit', 'API：调整信用分', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (9, '查看用户详情', 'admin:user:detail', 'api', '/admin/users/*', 'API：查看用户详情', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (10, '认证审核列表', 'admin:verify:list', 'menu', '/verify', '查看认证审核列表', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (11, '通过认证', 'admin:verify:approve', 'button', 'verify-approve', '按钮：通过认证', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (12, '驳回认证', 'admin:verify:reject', 'button', 'verify-reject', '按钮：驳回认证', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (13, '待审核列表API', 'admin:verify:pending', 'api', '/admin/users/pending-verify', 'API：获取待审核用户列表', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (14, '申诉列表', 'admin:appeal:list', 'menu', '/appeals', '查看申诉列表', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (15, '处理申诉', 'admin:appeal:handle', 'api', '/admin/appeals/*/handle', 'API：处理申诉', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (16, '提现列表', 'admin:withdraw:list', 'menu', '/withdraws', '查看提现列表', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (17, '处理提现', 'admin:withdraw:handle', 'api', '/admin/withdraws/*/process', 'API：处理提现', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (18, '系统配置', 'admin:config:view', 'menu', '/config', '查看系统配置', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);
INSERT INTO `permissions` VALUES (19, '修改配置', 'admin:config:edit', 'api', '/admin/config', 'API：修改系统配置', '2026-04-26 22:23:17', '2026-04-26 22:23:17', 0);

-- ----------------------------
-- Table structure for push_log
-- ----------------------------
DROP TABLE IF EXISTS `push_log`;
CREATE TABLE `push_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NULL DEFAULT NULL,
  `template_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `page` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` int(11) NULL DEFAULT 0 COMMENT '0-待发送，1-发送成功，2-发送失败，3-重试中',
  `retry_count` int(11) NULL DEFAULT 0,
  `next_retry_time` datetime NULL DEFAULT NULL,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_next_retry_time`(`next_retry_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '微信订阅消息推送日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of push_log
-- ----------------------------

-- ----------------------------
-- Table structure for role_permissions
-- ----------------------------
DROP TABLE IF EXISTS `role_permissions`;
CREATE TABLE `role_permissions`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `permission_id` bigint(20) NOT NULL COMMENT '权限ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_permission`(`role_id`, `permission_id`) USING BTREE,
  INDEX `idx_permission_id`(`permission_id`) USING BTREE,
  CONSTRAINT `FKegdk29eiy7mdtefy5c7eirr6e` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKn5fotdgk8d1xvo8nav9uv3muc` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 41 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色权限关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role_permissions
-- ----------------------------
INSERT INTO `role_permissions` VALUES (1, 2, 2, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (2, 2, 4, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (3, 2, 8, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (4, 2, 9, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (5, 2, 13, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (6, 2, 15, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (7, 2, 17, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (8, 2, 19, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (9, 2, 5, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (10, 2, 7, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (11, 2, 11, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (12, 2, 12, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (13, 2, 1, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (14, 2, 3, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (15, 2, 6, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (16, 2, 10, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (17, 2, 14, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (18, 2, 16, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (19, 2, 18, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (34, 1, 3, '2026-04-26 22:23:17');
INSERT INTO `role_permissions` VALUES (39, 1, 5, '2026-04-27 09:50:01');
INSERT INTO `role_permissions` VALUES (40, 1, 16, '2026-04-27 09:50:01');

-- ----------------------------
-- Table structure for roles
-- ----------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `role_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色描述',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT 0,
  `is_system` tinyint(4) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of roles
-- ----------------------------
INSERT INTO `roles` VALUES (1, '用户', 'USER', '普通用户，发布需求，接单完成任务', 1, 1, '2026-04-26 22:23:17', '2026-04-28 17:40:33', 0, 0);
INSERT INTO `roles` VALUES (2, '管理员', 'ADMIN', '系统管理员，拥有所有权限', 1, 2, '2026-04-26 22:23:17', '2026-04-28 17:44:22', 0, 0);

-- ----------------------------
-- Table structure for system_config
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置键',
  `config_value` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '配置值',
  `description` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '配置描述',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of system_config
-- ----------------------------
INSERT INTO `system_config` VALUES (1, 'platformFeeRate', '0.03', '平台抽成比例', '2026-04-27 09:58:07', '2026-04-27 15:37:02');
INSERT INTO `system_config` VALUES (2, 'creditThreshold', '60', '接单信用分门槛', '2026-04-27 09:58:07', '2026-04-27 15:37:02');
INSERT INTO `system_config` VALUES (3, 'maxOrderDistance', '3', '最大接单距离(公里)', '2026-04-27 09:58:07', '2026-04-27 09:58:07');
INSERT INTO `system_config` VALUES (4, 'maxDistanceKm', '10', '', '2026-04-27 10:02:44', '2026-04-27 15:37:02');
INSERT INTO `system_config` VALUES (5, 'notice', '欢迎使用校园帮互助跑腿平台！', '', '2026-04-27 10:02:44', '2026-04-27 15:37:02');

-- ----------------------------
-- Table structure for user_roles
-- ----------------------------
DROP TABLE IF EXISTS `user_roles`;
CREATE TABLE `user_roles`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_id`, `role_id`) USING BTREE,
  INDEX `idx_role_id`(`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_roles
-- ----------------------------
INSERT INTO `user_roles` VALUES (1, 10, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (2, 2, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (3, 3, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (4, 4, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (5, 5, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (6, 11, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (7, 12, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (8, 16, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (9, 17, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (10, 18, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (11, 21, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (12, 22, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (13, 25, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (14, 26, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (15, 28, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (16, 29, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (17, 31, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (18, 32, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (19, 34, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (20, 35, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (21, 37, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (22, 38, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (23, 6, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (24, 7, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (25, 8, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (26, 9, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (27, 13, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (28, 14, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (29, 15, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (30, 19, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (31, 20, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (32, 23, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (33, 24, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (34, 27, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (35, 30, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (36, 33, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (37, 36, 1, '2026-04-26 22:23:17');
INSERT INTO `user_roles` VALUES (38, 1, 2, '2026-04-26 22:23:17');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `open_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `student_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `role` tinyint(4) NOT NULL DEFAULT 0,
  `avatar_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `real_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `phone` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `verified_status` tinyint(4) NULL DEFAULT 0,
  `credit_score` int(11) NULL DEFAULT 100,
  `status` tinyint(4) NULL DEFAULT 1,
  `wallet_balance` decimal(10, 2) NULL DEFAULT 0.00,
  `frozen_balance` decimal(10, 2) NULL DEFAULT 0.00,
  `total_earnings` decimal(10, 2) NULL DEFAULT 0.00,
  `completed_orders` int(11) NULL DEFAULT 0,
  `acceptance_rate` decimal(3, 2) NULL DEFAULT 0.00,
  `student_cert_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `alipay_account` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `latitude` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `longitude` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `is_online` tinyint(4) NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT 0,
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_open_id`(`open_id`) USING BTREE,
  INDEX `idx_phone`(`phone`) USING BTREE,
  INDEX `idx_role_status`(`role`, `status`) USING BTREE,
  INDEX `idx_verified`(`verified_status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 83 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'admin_openid_001', '系统管理员', 'admin', 2, '/avatars/1_1777279107885.jpg', '管理员', '13800000001', 1, 100, 1, 0.00, 0.00, 0.00, 1, 0.00, '/avatars/1_1777279107885.jpg', NULL, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-04-28 17:51:51', 0, '$2a$10$mYFiN4SOl8mlGleCI0IUrer3HllM/S2GyT.ZTvhhYzOoDuHRbaMdq');
INSERT INTO `users` VALUES (2, 'publisher_openid_001', '张同学', '2021001001', 0, NULL, '张明', '13811111111', 1, 98, 1, 50.00, 0.00, 0.00, 5, 0.00, '/avatars/1_1777279107885.jpg', NULL, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (3, 'publisher_openid_002', '李同学', '2021001002', 1, NULL, '李华', '13822222222', 1, 95, 1, 69.25, 20.00, 14.25, 4, 0.00, '/avatars/cert-3-a2382ee3-7ac5-44e1-a634-981fdd96abad.png', NULL, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (4, 'publisher_openid_003', '王同学', '2021001003', 0, NULL, '王芳', '13833333333', 2, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, '/avatars/1_1777279107885.jpg', NULL, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (5, 'publisher_openid_004', '赵同学', '2021001004', 0, NULL, '赵强', '13844444444', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, '/avatars/1_1777279107885.jpg', NULL, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (6, 'runner_openid_001', '快递小哥小陈', '2020002001', 1, NULL, '陈志远', '13855555555', 1, 102, 1, 285.50, 0.00, 1250.00, 43, 0.92, '/avatars/1_1777279107885.jpg', NULL, NULL, NULL, 1, '2026-03-24 11:00:04', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (7, 'runner_openid_002', '超速跑腿刘哥', '2020002002', 1, NULL, '刘建国', '13866666666', 1, 96, 1, 128.00, 0.00, 680.00, 28, 0.85, '/avatars/1_1777279107885.jpg', NULL, NULL, NULL, 1, '2026-03-24 11:00:04', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (8, 'runner_openid_003', '校园配送小吴', '2019003001', 1, NULL, '吴晓燕', '13877777777', 1, 88, 1, 56.00, 0.00, 320.00, 15, 0.78, '/avatars/1_1777279107885.jpg', NULL, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (9, 'runner_openid_004', '闪电跑腿小孙', '2020002003', 1, NULL, '孙磊', '13888888888', 1, 72, 1, 12.00, 0.00, 95.00, 6, 0.60, '/avatars/1_1777279107885.jpg', NULL, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (10, 'frozen_openid_001', '被冻结用户', '2018005001', 0, NULL, '郑某某', '13899999999', 3, 45, 0, 0.00, 0.00, 0.00, 0, 0.00, '/avatars/1_1777279107885.jpg', NULL, NULL, NULL, 0, '2026-03-24 11:00:04', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (11, 'mock_publisher_001', '王同学', '2021001003', 0, NULL, '王芳', '13833333333', 2, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, '/avatars/1_1777279107885.jpg', NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (12, 'mock_publisher_002', '赵同学', '2021001004', 0, NULL, '赵伟', '13844444444', 1, 96, 1, 25.50, 0.00, 0.00, 2, 0.00, '/avatars/1_1777279107885.jpg', NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (13, 'mock_runner_001', '张跑腿', '2020001001', 1, NULL, '张强', '13855555555', 1, 98, 1, 120.80, 0.00, 150.50, 18, 0.96, '/avatars/1_1777279107885.jpg', 'zhangsan@alipay.com', NULL, NULL, 1, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (14, 'mock_runner_002', '李跑腿', '2020001002', 1, NULL, '李明', '13866666666', 1, 95, 1, 85.30, 0.00, 92.00, 12, 0.92, '/avatars/1_1777279107885.jpg', 'liming@alipay.com', NULL, NULL, 1, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (15, 'mock_runner_003', '王跑腿', '2020001003', 1, NULL, '王刚', '13877777777', 2, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (16, 'mock_publisher_003', '孙同学', '2021001005', 0, NULL, '孙静', '13888888888', 1, 92, 1, 15.00, 0.00, 0.00, 1, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (17, 'mock_publisher_004', '周同学', '2021001006', 0, NULL, '周明', '13899999999', 1, 88, 1, 8.50, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (18, 'mock_publisher_005', '吴同学', '2021001007', 0, NULL, '吴芳', '13900000000', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (19, 'mock_runner_004', '赵跑腿', '2020001004', 1, NULL, '赵刚', '13911111111', 1, 94, 1, 45.60, 0.00, 48.20, 6, 0.88, NULL, 'zhaogang@alipay.com', NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (20, 'mock_runner_005', '钱跑腿', '2020001005', 1, NULL, '钱磊', '13922222222', 3, 97, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (21, 'mock_publisher_006', '郑同学', '2022001001', 0, NULL, '郑小丽', '13933333333', 1, 99, 1, 30.00, 0.00, 0.00, 2, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (22, 'mock_publisher_007', '王同学', '2022001002', 0, NULL, '王小明', '13944444444', 1, 85, 1, 12.80, 0.00, 0.00, 1, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (23, 'mock_runner_006', '刘跑腿', '2019001001', 1, NULL, '刘强', '13955555555', 1, 93, 1, 156.20, 0.00, 188.50, 25, 0.94, NULL, 'liuqiang@alipay.com', NULL, NULL, 1, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (24, 'mock_runner_007', '陈跑腿', '2019001002', 1, NULL, '陈明', '13966666666', 1, 91, 1, 98.70, 0.00, 102.30, 14, 0.90, NULL, 'chenming@alipay.com', NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (25, 'mock_publisher_008', '朱同学', '2022001003', 0, NULL, '朱红', '13977777777', 2, 97, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (26, 'mock_publisher_009', '林同学', '2022001004', 0, NULL, '林小芳', '13988888888', 1, 94, 1, 22.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (27, 'mock_runner_008', '何跑腿', '2019001003', 1, NULL, '何勇', '13999999999', 1, 89, 1, 66.50, 0.00, 72.80, 9, 0.85, NULL, 'heyong@alipay.com', NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (28, 'mock_publisher_010', '谢同学', '2022001005', 0, NULL, '谢兰', '13500000000', 1, 96, 1, 18.50, 0.00, 0.00, 1, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (29, 'mock_publisher_011', '韩同学', '2022001006', 0, NULL, '韩磊', '13511111111', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (30, 'mock_runner_009', '唐跑腿', '2018001001', 1, NULL, '唐伟', '13522222222', 1, 92, 1, 208.30, 0.00, 245.60, 32, 0.92, NULL, 'tangwei@alipay.com', NULL, NULL, 1, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (31, 'mock_publisher_012', '冯同学', '2023001001', 0, NULL, '冯燕', '13533333333', 1, 98, 1, 45.00, 0.00, 0.00, 3, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (32, 'mock_publisher_013', '褚同学', '2023001002', 0, NULL, '褚健', '13544444444', 1, 86, 1, 10.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (33, 'mock_runner_010', '魏跑腿', '2018001002', 1, NULL, '魏亮', '13555555555', 2, 95, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (34, 'mock_publisher_014', '蒋同学', '2023001003', 0, NULL, '蒋敏', '13566666666', 1, 93, 1, 28.00, 0.00, 0.00, 2, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (35, 'mock_publisher_015', '沈同学', '2023001004', 0, NULL, '沈涛', '13577777777', 1, 90, 1, 16.50, 0.00, 0.00, 1, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (36, 'mock_runner_011', '姜跑腿', '2018001003', 1, NULL, '姜涛', '13588888888', 1, 96, 1, 87.40, 0.00, 95.20, 11, 0.93, NULL, 'jiangtao@alipay.com', NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (37, 'mock_publisher_016', '潘同学', '2023001005', 0, NULL, '潘美', '13599999999', 3, 95, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (38, 'mock_publisher_017', '彭同学', '2023001006', 0, NULL, '彭帅', '13600000000', 2, 94, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-26 20:53:35', '2026-04-28 14:30:58', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (39, 'mock_0e1Kmlll2scQCh4vR0ol2GZ8fF3KmllY', '微信用户', '2023001007', 0, '', '', '13600000007', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-28 11:27:06', '2026-04-28 15:07:41', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (40, 'onXGa7VlXyLvPKUzG_SnQKxiZ76k', '微信用户', '2023001008', 0, '', '', '13600000008', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-28 11:36:33', '2026-04-28 16:56:15', 0, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO `users` VALUES (44, '', 'JerryMo', '2023001009', 0, NULL, '', '13600000009', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-28 15:29:57', '2026-04-28 15:29:57', 0, '$2a$10$7PujitJnuScPr7OjdPs91On2N/LUdYg0DgqJumA.9x1hs0UKPim0m');
INSERT INTO `users` VALUES (48, 'mock_the code is a mock one', '微信用户', '', 0, '', '', NULL, 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-04-28 15:40:10', '2026-04-28 15:40:10', 0, NULL);
INSERT INTO `users` VALUES (51, 'user_test006_1778933916965', '????006', 'test006', 1, NULL, '', '13800138006', 0, 100, 1, 7.60, 0.00, 7.60, 1, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 20:18:37', '2026-05-16 20:18:37', 0, '$2a$10$QA.QBRzH/lSfPc.OHjZ.QOrDhjXiiH962BnGXY4gTYaFtwcNl0zTS');
INSERT INTO `users` VALUES (52, 'user_test_4582_1778934223209', '??????', 'test_4582', 0, NULL, '', '13800000001', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 20:23:43', '2026-05-16 20:23:43', 0, '$2a$10$VoMh//BiR5egYwvU8Su6HeHvwPtWcL2Zy2oMcr6ngwWGM8HtbBsxe');
INSERT INTO `users` VALUES (53, 'user_test_2026_1778934236318', 'test', 'test_2026', 0, NULL, '', '13800000000', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 20:23:56', '2026-05-16 20:23:56', 0, '$2a$10$MhvZ8mBcrvL7yBRzI0EQYOvUwoUl4L8YgkD.aVNzkSFc6TNPFx/2C');
INSERT INTO `users` VALUES (54, 'user_test_jwt_214_1778934258201', 'test', 'test_jwt_214', 0, NULL, '', '13800000000', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 20:24:18', '2026-05-16 20:24:18', 0, '$2a$10$nRWWeqVYkITqyc45QgCgq.VfunhXs80PcWBiTegW8pkWMJiMOt486');
INSERT INTO `users` VALUES (55, 'user_order_test_1562_1778934365918', 'OrderTester', 'order_test_1562', 0, NULL, '', '13800000002', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 20:26:06', '2026-05-16 20:26:06', 0, '$2a$10$0XO3M.65lfE.osHR8lhEa.QotWqvwJm9oB75IPomlLF1BCQ6Cd3.u');
INSERT INTO `users` VALUES (56, 'user_test_order_7103_1778936269494', 'OrderTest', 'test_order_7103', 0, NULL, '', '13800000001', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 20:57:50', '2026-05-16 20:57:50', 0, '$2a$10$5Y.2ov6iDmjt0yzWMbbvFenIt3HyoOfSkyfyuEQzw/CGspg8FwUKa');
INSERT INTO `users` VALUES (57, 'user_test_full_6063_1778936307470', 'FullTest', 'test_full_6063', 0, NULL, '', '13800000006063', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 20:58:27', '2026-05-16 20:58:27', 0, '$2a$10$eeSH0KnwzKlegYgtXFA2u.Rw3EUoOL/2ONAsR0uoSH5AUWY5NoWPC');
INSERT INTO `users` VALUES (58, 'user_pub_1627_1778937192377', '?????', 'pub_1627', 0, NULL, '', '1380000001', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 21:13:12', '2026-05-16 21:13:12', 0, '$2a$10$eLjT6gbxqpdTKYmz8Ov0GeQRj3co44qJgV5/8WsV2loLCLOwXcafK');
INSERT INTO `users` VALUES (59, 'user_run_1117_1778937192507', '?????', 'run_1117', 1, NULL, '', '1380000002', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 21:13:13', '2026-05-16 21:13:13', 0, '$2a$10$2oFk9pppwtchCNv6IeLRKeIxLhYpgMRW.m9GQH1lqK7Z6x.vri08i');
INSERT INTO `users` VALUES (60, 'user_mod_test_9429_1778937226099', '????', 'mod_test_9429', 1, NULL, '', '1380000009429', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, '30.5721', '113.2735', 1, '2026-05-16 21:13:46', '2026-05-16 21:13:46', 0, '$2a$10$VGpB0/kgydcudHYxsXu3ZescKPVStvQqEAK57cSAyi4nUhKS00Cka');
INSERT INTO `users` VALUES (61, 'user_runner001_1778937585403', '???001', 'runner001', 1, NULL, '', '13800138007', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 21:19:45', '2026-05-16 21:19:45', 0, '$2a$10$KRm8wmV6AZ.9REsTtp47k.oBBIyYp7oPMKXuU554G3Vg3gB2V453K');
INSERT INTO `users` VALUES (62, 'user_pub_6098_1778937768074', '???A', 'pub_6098', 0, NULL, '', '1380000001', 0, 100, 1, 0.00, 0.00, 0.00, 1, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 21:22:48', '2026-05-16 21:22:48', 0, '$2a$10$dAMsAsEgB288A05qvCjaTuVA.Z31N4FGJ162f4Jj4wDpOiIFJkGAG');
INSERT INTO `users` VALUES (63, 'user_run_1729_1778937768205', '???B', 'run_1729', 1, NULL, '', '1380000002', 0, 100, 1, 23.75, 0.00, 23.75, 1, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 21:22:48', '2026-05-16 21:22:48', 0, '$2a$10$u8qYIz8hatf3NBQBbQ.QHurLhEfTU1TPP/u3OcIkSUgtG8PGyN2zi');
INSERT INTO `users` VALUES (64, 'user_pub_lock_1812_1778937812915', '???', 'pub_lock_1812', 0, NULL, '', '1380000001', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 21:23:33', '2026-05-16 21:23:33', 0, '$2a$10$WRNosjLQTTuovmPdhhQ99egYN5olIyZr0CiZlDJnIixTpLXLUNi5m');
INSERT INTO `users` VALUES (65, 'user_run_lock_75631_1778937813024', '???1', 'run_lock_75631', 1, NULL, '', '1380000001', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 21:23:33', '2026-05-16 21:23:33', 0, '$2a$10$keOdVzDdd2gIEuDnJy.BjOhMFtUru6clzq.o/SC0T2dfujbYZJRgy');
INSERT INTO `users` VALUES (66, 'user_run_lock_53352_1778937813123', '???2', 'run_lock_53352', 1, NULL, '', '1380000002', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 21:23:33', '2026-05-16 21:23:33', 0, '$2a$10$NA.gZkwE2bUGtl6GMlfo4upZw9SAVNNH0whOK1p.whhLLYmdre5UG');
INSERT INTO `users` VALUES (67, 'user_run_lock_56823_1778937813223', '???3', 'run_lock_56823', 1, NULL, '', '1380000003', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 21:23:33', '2026-05-16 21:23:33', 0, '$2a$10$n6YajuCyJqm02FODlMeUDeTw1mtF9y0LfWbyK125rIecs277mWusW');
INSERT INTO `users` VALUES (68, 'user_run_lock_45204_1778937813313', '???4', 'run_lock_45204', 1, NULL, '', '1380000004', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 21:23:33', '2026-05-16 21:23:33', 0, '$2a$10$7NI8Ot.inL.LRbY4in8pmuNCHMaBMif2Jm1CsyoP4CgOpExTh4Toy');
INSERT INTO `users` VALUES (69, 'user_run_lock_63905_1778937813414', '???5', 'run_lock_63905', 1, NULL, '', '1380000005', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-16 21:23:33', '2026-05-16 21:23:33', 0, '$2a$10$tGFwX1HEZ6Pmeagf7bGtN.KgblxVneZB9wVop9L6cSsqPXGhNWhB.');
INSERT INTO `users` VALUES (70, 'user_geo_run_99780_1778937850835', '?????A(???)', 'geo_run_99780', 1, NULL, '', '1380000099780', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, '30.5721', '113.2735', 1, '2026-05-16 21:24:11', '2026-05-16 21:24:11', 0, '$2a$10$zjrpi5Zxbo3/dISlaUCYCucpIdQVYDR1NJ6Lh1b0GLjAeeUCi6tgu');
INSERT INTO `users` VALUES (71, 'user_geo_run_79511_1778937851003', '?????B(???)', 'geo_run_79511', 1, NULL, '', '1380000079511', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, '30.5730', '113.2745', 1, '2026-05-16 21:24:11', '2026-05-16 21:24:11', 0, '$2a$10$.JWnNkPMM/UtZnJeHHbT0OGiWp5Pp/ITYrJLSRgWN/v4zvM24ghwu');
INSERT INTO `users` VALUES (72, 'user_geo_run_28432_1778937851131', '???C(???)', 'geo_run_28432', 1, NULL, '', '1380000028432', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, '30.5800', '113.2800', 1, '2026-05-16 21:24:11', '2026-05-16 21:24:11', 0, '$2a$10$YuwydAoOcqVX9iVCGZ.P7.vW2rXY3uyXtY0OjhKIXAyubrEQVSG4C');
INSERT INTO `users` VALUES (73, 'user_verify_user_97662_1778978425603', '??????97662', 'verify_user_97662', 1, NULL, '', '13810097662', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-17 08:40:26', '2026-05-17 08:40:26', 0, '$2a$10$7w3qiFiGjC5pzV.iRZBh9.h1zpTW7qZw29Kt/LRho2XslEX80Gbqq');
INSERT INTO `users` VALUES (74, 'user_test_runner_59726_1778978505397', '?????', 'test_runner_59726', 1, NULL, '', '1390059726', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-17 08:41:45', '2026-05-17 08:41:45', 0, '$2a$10$obxPJofiF2LPOcIEcr68P.vXUm.FZhlg28zpx0Y0sP3hvnbPQ8pta');
INSERT INTO `users` VALUES (75, 'user_runner2_70907_1778978506624', '???2', 'runner2_70907', 1, NULL, '', '1391070907', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-17 08:41:47', '2026-05-17 08:41:47', 0, '$2a$10$Tg0CoD/5Qs.C7/54VhXxUuVyzguP7zh/JzPxm7WAo3tMxn3KITBZ.');
INSERT INTO `users` VALUES (76, 'user_geo_test_13925_1778978535359', 'GEO?????', 'geo_test_13925', 1, NULL, '', '1380013925', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, '30.5721', '113.2735', 1, '2026-05-17 08:42:15', '2026-05-17 08:42:15', 0, '$2a$10$N7CdQldDXg3FFw0s7azpCeeYYwhSGumUynzuNC0dML18PDfX7ZhCq');
INSERT INTO `users` VALUES (77, 'user_test_pub_002_1778979001461', '?????', 'test_pub_002', 0, NULL, '', '13800138000', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-17 08:50:01', '2026-05-17 08:50:01', 0, '$2a$10$B31ffc/cDEkmQTO9.gg32evusG1Yp/bKz8Qlt46Gtf3p6LfUydj1G');
INSERT INTO `users` VALUES (79, 'user_publisher_A_17878_1778979489866', '???A', 'publisher_A_17878', 0, NULL, '', '13810017878', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-17 08:58:10', '2026-05-17 08:58:10', 0, '$2a$10$JPPBu9SrJAMsZPy7z4rK5.rnWmfpBDIyfoBbK1Yhe9Lf2fY3jklrO');
INSERT INTO `users` VALUES (80, 'user_runner_B_17878_1778979489999', 'lisiDADA', 'runner_B_17878', 1, NULL, '李四DADA', '13820017878', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-05-17 08:58:10', '2026-06-02 17:00:32', 0, '$2a$10$S27xzfDcBZ43x/.0c6Kba.UYjhXltfBaNZ3W0QetDx0d9Dv5o1KIC');
INSERT INTO `users` VALUES (81, 'user_2023001010_1781487327240', 'lisiBABA', '2023001010', 0, NULL, '', '13820017879', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-06-15 09:35:27', '2026-06-15 09:35:27', 0, '$2a$10$AjsW6QggRUkuvb6YB8eLX.xCQNtgLte0FaWBCK4TQWrYq1XCK6n9a');
INSERT INTO `users` VALUES (82, 'user_2023001011_1781507083938', 'lisiCBCB', '2023001011', 0, NULL, '', '13820017881', 0, 100, 1, 0.00, 0.00, 0.00, 0, 0.00, NULL, NULL, NULL, NULL, 0, '2026-06-15 15:04:43', '2026-06-15 15:04:43', 0, '$2a$10$vcAmSm8COsBL46I7wMMPBO6R3jbeOiavyYpHGK2J4Gs0gSYbgYKaK');

-- ----------------------------
-- Table structure for wallet_transactions
-- ----------------------------
DROP TABLE IF EXISTS `wallet_transactions`;
CREATE TABLE `wallet_transactions`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `transaction_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `amount` decimal(10, 2) NOT NULL,
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `order_id` bigint(20) NULL DEFAULT NULL,
  `status` int(11) NOT NULL DEFAULT 0,
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `alipay_trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `third_party_account` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '第三方支付账号',
  `pay_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付时间',
  `related_id` bigint(20) NULL DEFAULT NULL COMMENT '关联业务ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tx_no`(`transaction_no`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_type_status`(`type`, `status`) USING BTREE,
  INDEX `idx_transaction_no`(`transaction_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 81 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '钱包流水表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of wallet_transactions
-- ----------------------------
INSERT INTO `wallet_transactions` VALUES (1, 6, 'TX20240301120001', 7.60, 'order_earn', 1, 1, '订单收益', NULL, NULL, NULL, NULL, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `wallet_transactions` VALUES (2, 6, 'TX20240302090001', 5.70, 'order_earn', 2, 1, '订单收益', NULL, NULL, NULL, NULL, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `wallet_transactions` VALUES (3, 7, 'TX20240303150001', 11.40, 'order_earn', 3, 1, '订单收益', NULL, NULL, NULL, NULL, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `wallet_transactions` VALUES (4, 7, 'TX20240304100001', 9.50, 'order_earn', 4, 1, '订单收益', NULL, NULL, NULL, NULL, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `wallet_transactions` VALUES (5, 8, 'TX20240305140001', 6.65, 'order_earn', 5, 1, '订单收益', NULL, NULL, NULL, NULL, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `wallet_transactions` VALUES (6, 6, 'TX20240310130001', 14.25, 'order_earn', 6, 1, '订单收益', NULL, NULL, NULL, NULL, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `wallet_transactions` VALUES (7, 6, 'TX20240301999001', 50.00, 'withdraw', NULL, 1, '提现到支付宝:138****5555', NULL, NULL, NULL, NULL, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `wallet_transactions` VALUES (8, 7, 'TX20240310999001', 30.00, 'withdraw', NULL, 0, '提现到支付宝:138****6666', NULL, NULL, NULL, NULL, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `wallet_transactions` VALUES (9, 2, 'TX20240315000001', 8.00, 'order_pay', 7, 1, '订单支付', NULL, NULL, NULL, NULL, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `wallet_transactions` VALUES (10, 3, 'TX20240316000001', 20.00, 'order_pay', 8, 1, '订单支付', NULL, NULL, NULL, NULL, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `wallet_transactions` VALUES (11, 2, 'TX20240317000001', 9.00, 'order_pay', 9, 1, '订单支付', NULL, NULL, NULL, NULL, '2026-03-24 11:00:04', '2026-03-24 11:00:04', 0);
INSERT INTO `wallet_transactions` VALUES (12, 11, 'TX1001', 50.00, 'recharge', NULL, 1, '支付宝充值', 'ALIPAY1001', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (13, 12, 'TX1002', 30.00, 'recharge', NULL, 1, '支付宝充值', 'ALIPAY1002', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (14, 13, 'TX1003', 20.00, 'recharge', NULL, 1, '支付宝充值', 'ALIPAY1003', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (15, 14, 'TX1004', 100.00, 'recharge', NULL, 1, '支付宝充值', 'ALIPAY1004', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (16, 15, 'TX1005', 80.00, 'recharge', NULL, 1, '支付宝充值', 'ALIPAY1005', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (17, 16, 'TX1006', 40.00, 'recharge', NULL, 1, '支付宝充值', 'ALIPAY1006', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (18, 17, 'TX1007', 50.00, 'recharge', NULL, 1, '支付宝充值', 'ALIPAY1007', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (19, 18, 'TX1008', 30.00, 'recharge', NULL, 1, '支付宝充值', 'ALIPAY1008', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (20, 19, 'TX1009', 100.00, 'recharge', NULL, 1, '支付宝充值', 'ALIPAY1009', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (21, 20, 'TX1010', 60.00, 'recharge', NULL, 1, '支付宝充值', 'ALIPAY1010', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (22, 21, 'TX1011', 25.00, 'recharge', NULL, 1, '支付宝充值', 'ALIPAY1011', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (23, 22, 'TX1012', 45.00, 'recharge', NULL, 1, '支付宝充值', 'alipay1012', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (24, 23, 'TX1013', 35.00, 'recharge', NULL, 1, '支付宝充值', 'alipay1013', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (25, 24, 'TX1014', 55.00, 'recharge', NULL, 1, '支付宝充值', 'alipay1014', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (26, 25, 'TX1015', 28.00, 'recharge', NULL, 1, '支付宝充值', 'alipay1015', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (27, 26, 'TX1016', 75.00, 'recharge', NULL, 1, '支付宝充值', 'alipay1016', NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (28, 1, 'WITH001', 60.00, 'withdraw', NULL, 1, '申请提现', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (29, 11, 'PAY1001', 8.00, 'order_pay', 13, 1, '支付订单：CH20240321080001', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (30, 12, 'PAY1002', 6.00, 'order_pay', 14, 1, '支付订单：CH20240321090002', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (31, 13, 'PAY1003', 10.00, 'order_pay', 15, 1, '支付订单：CH20240321100003', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (32, 14, 'PAY1004', 7.00, 'order_pay', 16, 1, '支付订单：CH20240321110004', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (33, 15, 'PAY1005', 12.00, 'order_pay', 17, 1, '支付订单：CH20240321120005', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (34, 16, 'PAY1006', 8.00, 'order_pay', 18, 1, '支付订单：CH20240321130006', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (35, 17, 'PAY1007', 10.00, 'order_pay', 19, 1, '支付订单：CH20240322080007', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (36, 2, 'EARN001', 7.60, 'order_earn', 1, 1, '跑腿收入：订单CH20240301120001', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (37, 6, 'EARN002', 5.70, 'order_earn', 2, 1, '跑腿收入：订单CH20240302090001', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (38, 7, 'EARN003', 11.40, 'order_earn', 3, 1, '跑腿收入：订单CH20240303150001', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (39, 6, 'EARN004', 9.50, 'order_earn', 4, 1, '跑腿收入：订单CH20240304100001', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (40, 8, 'EARN005', 6.65, 'order_earn', 5, 1, '跑腿收入：订单CH20240305140001', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (41, 6, 'EARN006', 14.25, 'order_earn', 6, 1, '跑腿收入：订单CH20240310130001', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (42, 9, 'EARN007', 8.55, 'order_earn', 9, 1, '跑腿收入：订单CH20240317140001', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (43, 3, 'EARN008', 11.40, 'order_earn', 11, 1, '跑腿收入：订单CH20240319150001', NULL, NULL, NULL, NULL, '2026-04-26 20:53:35', '2026-04-26 20:53:35', 0);
INSERT INTO `wallet_transactions` VALUES (56, 3, 'RE17791147725390003', 20.00, 'recharge', NULL, 1, '钱包充值', '2026051822001447410508729244', NULL, '2026-05-18 22:33:18', NULL, '2026-05-18 22:32:53', '2026-05-18 22:32:53', 0);
INSERT INTO `wallet_transactions` VALUES (57, 3, 'RE17791149315400003', 10.00, 'recharge', NULL, 1, '钱包充值', '2026051822001447410508727662', NULL, '2026-05-18 22:36:13', NULL, '2026-05-18 22:35:32', '2026-05-18 22:35:32', 0);
INSERT INTO `wallet_transactions` VALUES (58, 3, 'RE17791155124750003', 10.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-18 22:45:12', '2026-05-18 22:45:12', 0);
INSERT INTO `wallet_transactions` VALUES (59, 3, 'RE17791155185580003', 10.00, 'recharge', NULL, 1, '钱包充值', '2026051822001447410508724608', NULL, '2026-05-18 22:45:42', NULL, '2026-05-18 22:45:19', '2026-05-18 22:45:19', 0);
INSERT INTO `wallet_transactions` VALUES (60, 3, 'CH202605182308464263', -5.00, 'order_pay', NULL, 1, '订单支付（轮询补单）-代购', '2026051822001447410508727663', NULL, '2026-05-18 23:09:46', 105, '2026-05-18 23:09:48', '2026-05-18 23:09:48', 0);
INSERT INTO `wallet_transactions` VALUES (61, 3, 'TX20260521120301136211', 30.00, 'withdraw', NULL, 0, '提现到支付宝: 134135161441', NULL, NULL, NULL, NULL, '2026-05-21 12:03:01', '2026-05-21 12:03:01', 0);
INSERT INTO `wallet_transactions` VALUES (62, 3, 'RE17798453396320003', 10.00, 'recharge', NULL, 1, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-27 09:29:00', '2026-05-27 09:29:00', 0);
INSERT INTO `wallet_transactions` VALUES (63, 3, 'RE17798453462610003', 10.00, 'recharge', NULL, 1, '钱包充值', '2026052722001447410508765421', NULL, '2026-05-27 09:29:46', NULL, '2026-05-27 09:29:06', '2026-05-27 09:29:06', 0);
INSERT INTO `wallet_transactions` VALUES (64, 3, 'RE17798455854050003', 20.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-27 09:33:05', '2026-05-27 09:33:05', 0);
INSERT INTO `wallet_transactions` VALUES (65, 3, 'CH202605270952557958', -5.00, 'order_pay', NULL, 1, '订单支付（轮询补单）-代取快递', '2026052722001447410508770006', NULL, '2026-05-27 10:08:51', 109, '2026-05-27 10:08:53', '2026-05-27 10:08:53', 0);
INSERT INTO `wallet_transactions` VALUES (66, 3, 'CH202605271215250479', -8.00, 'order_pay', NULL, 1, '订单支付（轮询补单）-送物', '2026052722001447410508771335', NULL, '2026-05-27 12:23:12', 110, '2026-05-27 12:23:14', '2026-05-27 12:23:14', 0);
INSERT INTO `wallet_transactions` VALUES (67, 3, 'RE17799767323780003', 10.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-28 21:58:52', '2026-05-28 21:58:52', 0);
INSERT INTO `wallet_transactions` VALUES (68, 3, 'RE17799769011120003', 10.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-28 22:01:41', '2026-05-28 22:01:41', 0);
INSERT INTO `wallet_transactions` VALUES (69, 3, 'RE17799769493980003', 20.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-28 22:02:29', '2026-05-28 22:02:29', 0);
INSERT INTO `wallet_transactions` VALUES (70, 3, 'RE17799770684110003', 10.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-28 22:04:28', '2026-05-28 22:04:28', 0);
INSERT INTO `wallet_transactions` VALUES (71, 3, 'RE17800174297760003', 10.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-29 09:17:10', '2026-05-29 09:17:10', 0);
INSERT INTO `wallet_transactions` VALUES (72, 3, 'RE17800178961450003', 10.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-29 09:24:56', '2026-05-29 09:24:56', 0);
INSERT INTO `wallet_transactions` VALUES (73, 3, 'RE17800185410780003', 20.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-29 09:35:41', '2026-05-29 09:35:41', 0);
INSERT INTO `wallet_transactions` VALUES (74, 3, 'RE17800189867690003', 10.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-29 09:43:07', '2026-05-29 09:43:07', 0);
INSERT INTO `wallet_transactions` VALUES (75, 3, 'RE17800194599140003', 10.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-29 09:51:00', '2026-05-29 09:51:00', 0);
INSERT INTO `wallet_transactions` VALUES (76, 3, 'RE17800195601330003', 10.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-29 09:52:40', '2026-05-29 09:52:40', 0);
INSERT INTO `wallet_transactions` VALUES (77, 3, 'RE17800605531440003', 10.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-29 21:15:53', '2026-05-29 21:15:53', 0);
INSERT INTO `wallet_transactions` VALUES (78, 3, 'RE17800605803130003', 10.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-29 21:16:20', '2026-05-29 21:16:20', 0);
INSERT INTO `wallet_transactions` VALUES (79, 3, 'RE17801024531440003', 10.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-30 08:54:13', '2026-05-30 08:54:13', 0);
INSERT INTO `wallet_transactions` VALUES (80, 3, 'RE17801024669790003', 10.00, 'recharge', NULL, 0, '钱包充值', NULL, NULL, NULL, NULL, '2026-05-30 08:54:27', '2026-05-30 08:54:27', 0);

SET FOREIGN_KEY_CHECKS = 1;

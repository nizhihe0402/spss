/*
 Navicat Premium Dump SQL

 Source Server         : 36.129.10.116
 Source Server Type    : MySQL
 Source Server Version : 50736 (5.7.36-log)
 Source Host           : 36.129.10.116:3366
 Source Schema         : healthdetection_2025

 Target Server Type    : MySQL
 Target Server Version : 50736 (5.7.36-log)
 File Encoding         : 65001

 Date: 08/06/2026 16:34:20
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ref_vision_level
-- ----------------------------
DROP TABLE IF EXISTS `ref_vision_level`;
CREATE TABLE `ref_vision_level`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `std_id` bigint(20) NOT NULL COMMENT '标准版本ID → std_version.id（VISION_RULES）',
  `level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '等级名称：轻度/中度/重度',
  `comparator` enum('EQ','LE','BETWEEN') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '比较符：EQ=等于；LE=小于等于；BETWEEN=区间比较（含端点）',
  `va_min` decimal(3, 1) NULL DEFAULT NULL COMMENT '区间下限（BETWEEN时有效），单位：5.0制视力值',
  `va_max` decimal(3, 1) NULL DEFAULT NULL COMMENT '区间上限（BETWEEN时有效；或EQ时为等于的数值），单位：5.0制视力值',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注说明',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_std_level`(`std_id`, `level`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '【阈值】视力等级阈值（轻/中/重）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ref_vision_level
-- ----------------------------
INSERT INTO `ref_vision_level` VALUES (1, 1, '轻度', 'EQ', NULL, 4.9, '裸眼视力=4.9');
INSERT INTO `ref_vision_level` VALUES (2, 1, '中度', 'BETWEEN', 4.6, 4.8, '裸眼视力4.6–4.8（含端点）');
INSERT INTO `ref_vision_level` VALUES (3, 1, '重度', 'LE', NULL, 4.5, '裸眼视力≤4.5');

SET FOREIGN_KEY_CHECKS = 1;

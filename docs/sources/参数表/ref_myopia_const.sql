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

 Date: 08/06/2026 16:34:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ref_myopia_const
-- ----------------------------
DROP TABLE IF EXISTS `ref_myopia_const`;
CREATE TABLE `ref_myopia_const`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `std_id` bigint(20) NOT NULL COMMENT '标准版本ID → std_version.id（VISION_RULES）',
  `va_lt` decimal(3, 1) NOT NULL COMMENT '裸眼视力阈值：小于此值才参与近视判定（例如 5.0）',
  `se_lt` decimal(4, 2) NOT NULL COMMENT '等效球镜阈值：小于此屈光度视为近视（例如 -0.50D）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_std`(`std_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '【阈值】近视判定常量参数（视力与屈光度）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ref_myopia_const
-- ----------------------------
INSERT INTO `ref_myopia_const` VALUES (1, 1, 5.0, -0.50);

SET FOREIGN_KEY_CHECKS = 1;

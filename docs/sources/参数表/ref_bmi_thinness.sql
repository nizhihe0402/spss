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

 Date: 08/06/2026 16:33:28
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ref_bmi_thinness
-- ----------------------------
DROP TABLE IF EXISTS `ref_bmi_thinness`;
CREATE TABLE `ref_bmi_thinness`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `std_id` bigint(20) NOT NULL COMMENT '标准版本ID → std_version.id（WS456_2014）',
  `sex` enum('M','F') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '性别：M=男，F=女',
  `age_half` decimal(4, 1) NOT NULL COMMENT '半岁年龄：例 6.0、6.5 ...',
  `mild_min` decimal(5, 2) NULL DEFAULT NULL COMMENT '轻度消瘦-BMI区间下限（含）；若该段无轻度区间则为NULL',
  `mild_max` decimal(5, 2) NULL DEFAULT NULL COMMENT '轻度消瘦-BMI区间上限（含）；若该段无轻度区间则为NULL',
  `severe_le` decimal(5, 2) NOT NULL COMMENT '中重度消瘦-BMI阈值（≤该值判中重度）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_std_sex_age`(`std_id`, `sex`, `age_half`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 69 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '【阈值】消瘦-BMI区间/阈值（WS/T 456-2014）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ref_bmi_thinness
-- ----------------------------
INSERT INTO `ref_bmi_thinness` VALUES (21, 4, 'M', 6.0, 13.30, 13.40, 13.20);
INSERT INTO `ref_bmi_thinness` VALUES (22, 4, 'M', 6.5, 13.50, 13.80, 13.40);
INSERT INTO `ref_bmi_thinness` VALUES (23, 4, 'M', 7.0, 13.60, 13.90, 13.50);
INSERT INTO `ref_bmi_thinness` VALUES (24, 4, 'M', 7.5, 13.60, 13.90, 13.50);
INSERT INTO `ref_bmi_thinness` VALUES (25, 4, 'M', 8.0, 13.70, 14.00, 13.60);
INSERT INTO `ref_bmi_thinness` VALUES (26, 4, 'M', 8.5, 13.70, 14.00, 13.60);
INSERT INTO `ref_bmi_thinness` VALUES (27, 4, 'M', 9.0, 13.80, 14.10, 13.70);
INSERT INTO `ref_bmi_thinness` VALUES (28, 4, 'M', 9.5, 13.90, 14.20, 13.80);
INSERT INTO `ref_bmi_thinness` VALUES (29, 4, 'M', 10.0, 14.00, 14.40, 13.90);
INSERT INTO `ref_bmi_thinness` VALUES (30, 4, 'M', 10.5, 14.10, 14.60, 14.00);
INSERT INTO `ref_bmi_thinness` VALUES (31, 4, 'M', 11.0, 14.30, 14.90, 14.20);
INSERT INTO `ref_bmi_thinness` VALUES (32, 4, 'M', 11.5, 14.40, 15.10, 14.30);
INSERT INTO `ref_bmi_thinness` VALUES (33, 4, 'M', 12.0, 14.50, 15.40, 14.40);
INSERT INTO `ref_bmi_thinness` VALUES (34, 4, 'M', 12.5, 14.60, 15.60, 14.50);
INSERT INTO `ref_bmi_thinness` VALUES (35, 4, 'M', 13.0, 14.90, 15.90, 14.80);
INSERT INTO `ref_bmi_thinness` VALUES (36, 4, 'M', 13.5, 15.10, 16.10, 15.00);
INSERT INTO `ref_bmi_thinness` VALUES (37, 4, 'M', 14.0, 15.40, 16.40, 15.30);
INSERT INTO `ref_bmi_thinness` VALUES (38, 4, 'M', 14.5, 15.60, 16.70, 15.50);
INSERT INTO `ref_bmi_thinness` VALUES (39, 4, 'M', 15.0, 15.90, 16.90, 15.80);
INSERT INTO `ref_bmi_thinness` VALUES (40, 4, 'M', 15.5, 16.10, 17.00, 16.00);
INSERT INTO `ref_bmi_thinness` VALUES (41, 4, 'M', 16.0, 16.30, 17.30, 16.20);
INSERT INTO `ref_bmi_thinness` VALUES (42, 4, 'M', 16.5, 16.50, 17.50, 16.40);
INSERT INTO `ref_bmi_thinness` VALUES (43, 4, 'M', 17.0, 16.70, 17.70, 16.60);
INSERT INTO `ref_bmi_thinness` VALUES (44, 4, 'M', 17.5, 16.90, 17.90, 16.80);
INSERT INTO `ref_bmi_thinness` VALUES (45, 4, 'F', 6.0, 12.90, 13.10, 12.80);
INSERT INTO `ref_bmi_thinness` VALUES (46, 4, 'F', 6.5, 13.00, 13.30, 12.90);
INSERT INTO `ref_bmi_thinness` VALUES (47, 4, 'F', 7.0, 13.10, 13.40, 13.00);
INSERT INTO `ref_bmi_thinness` VALUES (48, 4, 'F', 7.5, 13.10, 13.50, 13.00);
INSERT INTO `ref_bmi_thinness` VALUES (49, 4, 'F', 8.0, 13.20, 13.60, 13.10);
INSERT INTO `ref_bmi_thinness` VALUES (50, 4, 'F', 8.5, 13.20, 13.70, 13.10);
INSERT INTO `ref_bmi_thinness` VALUES (51, 4, 'F', 9.0, 13.30, 13.80, 13.20);
INSERT INTO `ref_bmi_thinness` VALUES (52, 4, 'F', 9.5, 13.30, 13.90, 13.20);
INSERT INTO `ref_bmi_thinness` VALUES (53, 4, 'F', 10.0, 13.40, 14.00, 13.30);
INSERT INTO `ref_bmi_thinness` VALUES (54, 4, 'F', 10.5, 13.50, 14.10, 13.40);
INSERT INTO `ref_bmi_thinness` VALUES (55, 4, 'F', 11.0, 13.80, 14.30, 13.70);
INSERT INTO `ref_bmi_thinness` VALUES (56, 4, 'F', 11.5, 14.00, 14.50, 13.90);
INSERT INTO `ref_bmi_thinness` VALUES (57, 4, 'F', 12.0, 14.20, 14.70, 14.10);
INSERT INTO `ref_bmi_thinness` VALUES (58, 4, 'F', 12.5, 14.40, 14.90, 14.30);
INSERT INTO `ref_bmi_thinness` VALUES (59, 4, 'F', 13.0, 14.70, 15.30, 14.60);
INSERT INTO `ref_bmi_thinness` VALUES (60, 4, 'F', 13.5, 15.00, 15.60, 14.90);
INSERT INTO `ref_bmi_thinness` VALUES (61, 4, 'F', 14.0, 15.40, 16.00, 15.30);
INSERT INTO `ref_bmi_thinness` VALUES (62, 4, 'F', 14.5, 15.80, 16.30, 15.70);
INSERT INTO `ref_bmi_thinness` VALUES (63, 4, 'F', 15.0, 16.10, 16.60, 16.00);
INSERT INTO `ref_bmi_thinness` VALUES (64, 4, 'F', 15.5, 16.30, 16.80, 16.20);
INSERT INTO `ref_bmi_thinness` VALUES (65, 4, 'F', 16.0, 16.50, 17.00, 16.40);
INSERT INTO `ref_bmi_thinness` VALUES (66, 4, 'F', 16.5, 16.60, 17.10, 16.50);
INSERT INTO `ref_bmi_thinness` VALUES (67, 4, 'F', 17.0, 16.70, 17.20, 16.60);
INSERT INTO `ref_bmi_thinness` VALUES (68, 4, 'F', 17.5, 16.80, 17.30, 16.70);

SET FOREIGN_KEY_CHECKS = 1;

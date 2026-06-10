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

 Date: 08/06/2026 16:33:19
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ref_bmi_over_obesity
-- ----------------------------
DROP TABLE IF EXISTS `ref_bmi_over_obesity`;
CREATE TABLE `ref_bmi_over_obesity`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `std_id` bigint(20) NOT NULL COMMENT '标准版本ID → std_version.id（WS586_2018）',
  `sex` enum('M','F') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '性别：M=男，F=女',
  `age_half` decimal(4, 1) NOT NULL COMMENT '半岁年龄：例 6.0、6.5、7.0 ...，计算时建议四舍五入到0.5',
  `over_bmi` decimal(5, 2) NOT NULL COMMENT '超重界值（含）',
  `obese_bmi` decimal(5, 2) NOT NULL COMMENT '肥胖界值（含）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_std_sex_age`(`std_id`, `sex`, `age_half`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 51 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '【阈值】BMI超重/肥胖筛查阈值（WS/T 586-2018）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ref_bmi_over_obesity
-- ----------------------------
INSERT INTO `ref_bmi_over_obesity` VALUES (1, 3, 'M', 6.0, 16.40, 17.70);
INSERT INTO `ref_bmi_over_obesity` VALUES (2, 3, 'M', 6.5, 16.70, 18.10);
INSERT INTO `ref_bmi_over_obesity` VALUES (3, 3, 'M', 7.0, 17.00, 18.70);
INSERT INTO `ref_bmi_over_obesity` VALUES (4, 3, 'M', 7.5, 17.40, 19.20);
INSERT INTO `ref_bmi_over_obesity` VALUES (5, 3, 'M', 8.0, 17.80, 19.70);
INSERT INTO `ref_bmi_over_obesity` VALUES (6, 3, 'M', 8.5, 18.10, 20.30);
INSERT INTO `ref_bmi_over_obesity` VALUES (7, 3, 'M', 9.0, 18.50, 20.80);
INSERT INTO `ref_bmi_over_obesity` VALUES (8, 3, 'M', 9.5, 18.90, 21.40);
INSERT INTO `ref_bmi_over_obesity` VALUES (9, 3, 'M', 10.0, 19.20, 21.90);
INSERT INTO `ref_bmi_over_obesity` VALUES (10, 3, 'F', 6.0, 16.20, 17.50);
INSERT INTO `ref_bmi_over_obesity` VALUES (11, 3, 'F', 6.5, 16.50, 18.00);
INSERT INTO `ref_bmi_over_obesity` VALUES (12, 3, 'F', 7.0, 16.80, 18.50);
INSERT INTO `ref_bmi_over_obesity` VALUES (13, 3, 'F', 7.5, 17.20, 19.00);
INSERT INTO `ref_bmi_over_obesity` VALUES (14, 3, 'F', 8.0, 17.60, 19.40);
INSERT INTO `ref_bmi_over_obesity` VALUES (15, 3, 'F', 8.5, 18.10, 19.90);
INSERT INTO `ref_bmi_over_obesity` VALUES (16, 3, 'F', 9.0, 18.50, 20.40);
INSERT INTO `ref_bmi_over_obesity` VALUES (17, 3, 'F', 9.5, 19.00, 21.00);
INSERT INTO `ref_bmi_over_obesity` VALUES (18, 3, 'F', 10.0, 19.50, 21.50);
INSERT INTO `ref_bmi_over_obesity` VALUES (19, 3, 'M', 10.5, 19.60, 22.50);
INSERT INTO `ref_bmi_over_obesity` VALUES (20, 3, 'M', 11.0, 19.90, 23.00);
INSERT INTO `ref_bmi_over_obesity` VALUES (21, 3, 'M', 11.5, 20.30, 23.60);
INSERT INTO `ref_bmi_over_obesity` VALUES (22, 3, 'M', 12.0, 20.70, 24.10);
INSERT INTO `ref_bmi_over_obesity` VALUES (23, 3, 'M', 12.5, 21.00, 24.70);
INSERT INTO `ref_bmi_over_obesity` VALUES (24, 3, 'M', 13.0, 21.40, 25.20);
INSERT INTO `ref_bmi_over_obesity` VALUES (25, 3, 'M', 13.5, 21.90, 25.70);
INSERT INTO `ref_bmi_over_obesity` VALUES (26, 3, 'M', 14.0, 22.30, 26.10);
INSERT INTO `ref_bmi_over_obesity` VALUES (27, 3, 'M', 14.5, 22.60, 26.40);
INSERT INTO `ref_bmi_over_obesity` VALUES (28, 3, 'M', 15.0, 22.90, 26.60);
INSERT INTO `ref_bmi_over_obesity` VALUES (29, 3, 'M', 15.5, 23.10, 26.90);
INSERT INTO `ref_bmi_over_obesity` VALUES (30, 3, 'M', 16.0, 23.30, 27.10);
INSERT INTO `ref_bmi_over_obesity` VALUES (31, 3, 'M', 16.5, 23.50, 27.40);
INSERT INTO `ref_bmi_over_obesity` VALUES (32, 3, 'M', 17.0, 23.70, 27.60);
INSERT INTO `ref_bmi_over_obesity` VALUES (33, 3, 'M', 17.5, 23.80, 27.80);
INSERT INTO `ref_bmi_over_obesity` VALUES (34, 3, 'M', 18.0, 24.00, 28.00);
INSERT INTO `ref_bmi_over_obesity` VALUES (35, 3, 'F', 10.5, 20.00, 22.10);
INSERT INTO `ref_bmi_over_obesity` VALUES (36, 3, 'F', 11.0, 20.50, 22.70);
INSERT INTO `ref_bmi_over_obesity` VALUES (37, 3, 'F', 11.5, 21.10, 23.30);
INSERT INTO `ref_bmi_over_obesity` VALUES (38, 3, 'F', 12.0, 21.50, 23.90);
INSERT INTO `ref_bmi_over_obesity` VALUES (39, 3, 'F', 12.5, 21.90, 24.50);
INSERT INTO `ref_bmi_over_obesity` VALUES (40, 3, 'F', 13.0, 22.60, 25.60);
INSERT INTO `ref_bmi_over_obesity` VALUES (41, 3, 'F', 13.5, 22.60, 25.60);
INSERT INTO `ref_bmi_over_obesity` VALUES (42, 3, 'F', 14.0, 22.80, 25.90);
INSERT INTO `ref_bmi_over_obesity` VALUES (43, 3, 'F', 14.5, 23.00, 26.30);
INSERT INTO `ref_bmi_over_obesity` VALUES (44, 3, 'F', 15.0, 23.20, 26.60);
INSERT INTO `ref_bmi_over_obesity` VALUES (45, 3, 'F', 15.5, 23.40, 26.90);
INSERT INTO `ref_bmi_over_obesity` VALUES (46, 3, 'F', 16.0, 23.60, 27.10);
INSERT INTO `ref_bmi_over_obesity` VALUES (47, 3, 'F', 16.5, 23.70, 27.40);
INSERT INTO `ref_bmi_over_obesity` VALUES (48, 3, 'F', 17.0, 23.80, 27.60);
INSERT INTO `ref_bmi_over_obesity` VALUES (49, 3, 'F', 17.5, 23.90, 27.80);
INSERT INTO `ref_bmi_over_obesity` VALUES (50, 3, 'F', 18.0, 24.00, 28.00);

SET FOREIGN_KEY_CHECKS = 1;

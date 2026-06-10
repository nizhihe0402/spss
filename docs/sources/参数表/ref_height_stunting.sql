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

 Date: 08/06/2026 16:34:05
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ref_height_stunting
-- ----------------------------
DROP TABLE IF EXISTS `ref_height_stunting`;
CREATE TABLE `ref_height_stunting`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `std_id` bigint(20) NOT NULL COMMENT '标准版本ID → std_version.id（WS456_2014）',
  `sex` enum('M','F') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '性别：M=男，F=女',
  `age_half` decimal(4, 1) NOT NULL COMMENT '半岁年龄：例 6.0、6.5 ...',
  `height_le` decimal(5, 1) NOT NULL COMMENT '身高界值（cm）：≤此值判为生长迟缓',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_std_sex_age`(`std_id`, `sex`, `age_half`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 49 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '【阈值】生长迟缓-身高≤界值（WS/T 456-2014）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ref_height_stunting
-- ----------------------------
INSERT INTO `ref_height_stunting` VALUES (1, 4, 'M', 6.0, 106.3);
INSERT INTO `ref_height_stunting` VALUES (2, 4, 'M', 6.5, 109.5);
INSERT INTO `ref_height_stunting` VALUES (3, 4, 'M', 7.0, 111.3);
INSERT INTO `ref_height_stunting` VALUES (4, 4, 'M', 7.5, 112.8);
INSERT INTO `ref_height_stunting` VALUES (5, 4, 'M', 8.0, 115.4);
INSERT INTO `ref_height_stunting` VALUES (6, 4, 'M', 8.5, 117.6);
INSERT INTO `ref_height_stunting` VALUES (7, 4, 'M', 9.0, 120.6);
INSERT INTO `ref_height_stunting` VALUES (8, 4, 'M', 9.5, 123.0);
INSERT INTO `ref_height_stunting` VALUES (9, 4, 'M', 10.0, 125.2);
INSERT INTO `ref_height_stunting` VALUES (10, 4, 'M', 10.5, 127.0);
INSERT INTO `ref_height_stunting` VALUES (11, 4, 'M', 11.0, 129.1);
INSERT INTO `ref_height_stunting` VALUES (12, 4, 'M', 11.5, 130.8);
INSERT INTO `ref_height_stunting` VALUES (13, 4, 'M', 12.0, 133.1);
INSERT INTO `ref_height_stunting` VALUES (14, 4, 'F', 6.0, 105.7);
INSERT INTO `ref_height_stunting` VALUES (15, 4, 'F', 6.5, 108.0);
INSERT INTO `ref_height_stunting` VALUES (16, 4, 'F', 7.0, 110.2);
INSERT INTO `ref_height_stunting` VALUES (17, 4, 'F', 7.5, 111.8);
INSERT INTO `ref_height_stunting` VALUES (18, 4, 'F', 8.0, 114.5);
INSERT INTO `ref_height_stunting` VALUES (19, 4, 'F', 8.5, 116.8);
INSERT INTO `ref_height_stunting` VALUES (20, 4, 'F', 9.0, 119.5);
INSERT INTO `ref_height_stunting` VALUES (21, 4, 'F', 9.5, 121.7);
INSERT INTO `ref_height_stunting` VALUES (22, 4, 'F', 10.0, 123.9);
INSERT INTO `ref_height_stunting` VALUES (23, 4, 'F', 10.5, 125.7);
INSERT INTO `ref_height_stunting` VALUES (24, 4, 'F', 11.0, 128.6);
INSERT INTO `ref_height_stunting` VALUES (25, 4, 'F', 11.5, 131.0);
INSERT INTO `ref_height_stunting` VALUES (26, 4, 'F', 12.0, 133.6);
INSERT INTO `ref_height_stunting` VALUES (27, 4, 'M', 12.5, 134.9);
INSERT INTO `ref_height_stunting` VALUES (28, 4, 'M', 13.0, 136.9);
INSERT INTO `ref_height_stunting` VALUES (29, 4, 'M', 13.5, 138.6);
INSERT INTO `ref_height_stunting` VALUES (30, 4, 'M', 14.0, 141.9);
INSERT INTO `ref_height_stunting` VALUES (31, 4, 'M', 14.5, 144.7);
INSERT INTO `ref_height_stunting` VALUES (32, 4, 'M', 15.0, 149.6);
INSERT INTO `ref_height_stunting` VALUES (33, 4, 'M', 15.5, 153.6);
INSERT INTO `ref_height_stunting` VALUES (34, 4, 'M', 16.0, 155.1);
INSERT INTO `ref_height_stunting` VALUES (35, 4, 'M', 16.5, 156.4);
INSERT INTO `ref_height_stunting` VALUES (36, 4, 'M', 17.0, 156.8);
INSERT INTO `ref_height_stunting` VALUES (37, 4, 'M', 17.5, 157.1);
INSERT INTO `ref_height_stunting` VALUES (38, 4, 'F', 12.5, 135.7);
INSERT INTO `ref_height_stunting` VALUES (39, 4, 'F', 13.0, 138.8);
INSERT INTO `ref_height_stunting` VALUES (40, 4, 'F', 13.5, 141.4);
INSERT INTO `ref_height_stunting` VALUES (41, 4, 'F', 14.0, 142.9);
INSERT INTO `ref_height_stunting` VALUES (42, 4, 'F', 14.5, 144.1);
INSERT INTO `ref_height_stunting` VALUES (43, 4, 'F', 15.0, 145.4);
INSERT INTO `ref_height_stunting` VALUES (44, 4, 'F', 15.5, 146.5);
INSERT INTO `ref_height_stunting` VALUES (45, 4, 'F', 16.0, 146.8);
INSERT INTO `ref_height_stunting` VALUES (46, 4, 'F', 16.5, 147.0);
INSERT INTO `ref_height_stunting` VALUES (47, 4, 'F', 17.0, 147.3);
INSERT INTO `ref_height_stunting` VALUES (48, 4, 'F', 17.5, 147.5);

SET FOREIGN_KEY_CHECKS = 1;

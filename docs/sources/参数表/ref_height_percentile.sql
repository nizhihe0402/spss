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

 Date: 08/06/2026 16:33:57
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ref_height_percentile
-- ----------------------------
DROP TABLE IF EXISTS `ref_height_percentile`;
CREATE TABLE `ref_height_percentile`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `std_id` bigint(20) NOT NULL COMMENT '标准版本ID → std_version.id（WS610_2018）',
  `sex` enum('M','F') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '性别：M=男，F=女',
  `age_year` int(11) NOT NULL COMMENT '年龄（整岁）：7~17',
  `p5` decimal(5, 1) NULL DEFAULT NULL COMMENT 'P5 身高（cm）',
  `p10` decimal(5, 1) NULL DEFAULT NULL COMMENT 'P10 身高（cm）',
  `p25` decimal(5, 1) NULL DEFAULT NULL COMMENT 'P25 身高（cm）',
  `p50` decimal(5, 1) NULL DEFAULT NULL COMMENT 'P50 身高（cm）',
  `p75` decimal(5, 1) NULL DEFAULT NULL COMMENT 'P75 身高（cm）',
  `p90` decimal(5, 1) NULL DEFAULT NULL COMMENT 'P90 身高（cm）',
  `p95` decimal(5, 1) NULL DEFAULT NULL COMMENT 'P95 身高（cm）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_std_sex_age`(`std_id`, `sex`, `age_year`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '【参考】血压判定-身高分位参考表（WS/T 610-2018）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ref_height_percentile
-- ----------------------------
INSERT INTO `ref_height_percentile` VALUES (1, 5, 'M', 7, 115.7, 117.9, 121.5, 125.5, 129.5, 133.3, 135.4);
INSERT INTO `ref_height_percentile` VALUES (2, 5, 'M', 8, 120.6, 122.9, 126.5, 130.7, 134.9, 138.7, 141.0);
INSERT INTO `ref_height_percentile` VALUES (3, 5, 'M', 9, 125.0, 127.4, 131.4, 135.8, 140.3, 144.2, 146.6);
INSERT INTO `ref_height_percentile` VALUES (4, 5, 'M', 10, 130.0, 132.1, 136.1, 140.8, 145.4, 149.8, 152.4);
INSERT INTO `ref_height_percentile` VALUES (5, 5, 'M', 11, 133.7, 136.4, 141.0, 146.0, 151.3, 156.5, 159.7);
INSERT INTO `ref_height_percentile` VALUES (6, 5, 'M', 12, 138.4, 141.2, 146.0, 152.0, 158.6, 164.1, 167.3);
INSERT INTO `ref_height_percentile` VALUES (7, 5, 'M', 13, 145.1, 148.2, 154.0, 160.2, 166.2, 170.7, 173.4);
INSERT INTO `ref_height_percentile` VALUES (8, 5, 'M', 14, 151.6, 155.0, 160.4, 165.7, 170.5, 175.0, 177.5);
INSERT INTO `ref_height_percentile` VALUES (9, 5, 'M', 15, 157.2, 160.0, 164.4, 169.0, 173.4, 177.4, 180.0);
INSERT INTO `ref_height_percentile` VALUES (10, 5, 'M', 16, 160.0, 162.4, 166.3, 170.5, 174.9, 178.8, 181.0);
INSERT INTO `ref_height_percentile` VALUES (11, 5, 'M', 17, 161.2, 163.3, 167.1, 171.4, 175.6, 179.5, 181.9);
INSERT INTO `ref_height_percentile` VALUES (12, 5, 'F', 7, 114.3, 116.5, 120.1, 124.1, 128.1, 131.9, 134.0);
INSERT INTO `ref_height_percentile` VALUES (13, 5, 'F', 8, 119.2, 121.5, 125.2, 129.3, 133.6, 137.2, 139.6);
INSERT INTO `ref_height_percentile` VALUES (14, 5, 'F', 9, 124.0, 126.4, 130.3, 135.0, 139.6, 143.9, 146.5);
INSERT INTO `ref_height_percentile` VALUES (15, 5, 'F', 10, 129.1, 131.8, 136.0, 141.2, 146.3, 150.7, 153.3);
INSERT INTO `ref_height_percentile` VALUES (16, 5, 'F', 11, 134.2, 137.1, 142.0, 147.3, 152.7, 157.1, 159.6);
INSERT INTO `ref_height_percentile` VALUES (17, 5, 'F', 12, 139.7, 142.6, 147.5, 152.5, 157.1, 160.9, 163.5);
INSERT INTO `ref_height_percentile` VALUES (18, 5, 'F', 13, 145.6, 148.0, 152.0, 156.1, 160.0, 163.8, 166.0);
INSERT INTO `ref_height_percentile` VALUES (19, 5, 'F', 14, 148.2, 150.4, 154.0, 157.8, 161.6, 165.1, 167.2);
INSERT INTO `ref_height_percentile` VALUES (20, 5, 'F', 15, 149.2, 151.4, 154.8, 158.4, 162.2, 166.0, 168.0);
INSERT INTO `ref_height_percentile` VALUES (21, 5, 'F', 16, 150.0, 151.7, 155.2, 159.0, 162.8, 166.3, 168.3);
INSERT INTO `ref_height_percentile` VALUES (22, 5, 'F', 17, 150.1, 152.0, 155.3, 159.2, 163.1, 166.6, 168.8);

SET FOREIGN_KEY_CHECKS = 1;

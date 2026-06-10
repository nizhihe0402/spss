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

 Date: 08/06/2026 16:33:34
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ref_bp_adult
-- ----------------------------
DROP TABLE IF EXISTS `ref_bp_adult`;
CREATE TABLE `ref_bp_adult`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `std_id` bigint(20) NOT NULL COMMENT '标准版本ID → std_version.id（WS610_2018）',
  `sbp_ge` int(11) NOT NULL COMMENT '成人高血压判定：收缩压 ≥ 此值（mmHg），默认 140',
  `dbp_ge` int(11) NOT NULL COMMENT '成人高血压判定：舒张压 ≥ 此值（mmHg），默认 90',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_std`(`std_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '【阈值】成人（≥18岁）血压判定阈值' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ref_bp_adult
-- ----------------------------
INSERT INTO `ref_bp_adult` VALUES (1, 5, 140, 90);

SET FOREIGN_KEY_CHECKS = 1;

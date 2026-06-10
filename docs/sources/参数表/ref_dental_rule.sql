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

 Date: 08/06/2026 16:33:49
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ref_dental_rule
-- ----------------------------
DROP TABLE IF EXISTS `ref_dental_rule`;
CREATE TABLE `ref_dental_rule`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `std_id` bigint(20) NOT NULL COMMENT '标准版本ID → std_version.id（DENTAL_RULES）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则代号：如 CARIES_SUM_GT0',
  `expr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则表达式说明：如 \"q51+...+q56 > 0\"',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注说明',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_std_name`(`std_id`, `name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '【口径】口腔-龋齿判定规则（表达式留痕）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ref_dental_rule
-- ----------------------------
INSERT INTO `ref_dental_rule` VALUES (1, 2, 'CARIES_SUM_GT0', 'q51+q52+q53+q54+q55+q56 > 0', '和>0判患龋，和=患龋牙数');

SET FOREIGN_KEY_CHECKS = 1;

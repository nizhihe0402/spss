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

 Date: 08/06/2026 16:33:41
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ref_bp_p95
-- ----------------------------
DROP TABLE IF EXISTS `ref_bp_p95`;
CREATE TABLE `ref_bp_p95`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `std_id` bigint(20) NOT NULL COMMENT '标准版本ID → std_version.id（WS610_2018）',
  `sex` enum('M','F') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '性别：M=男，F=女',
  `age_year` int(11) NOT NULL COMMENT '年龄（整岁）：7~17',
  `height_bucket` enum('LT_P5','P5','P10','P25','P50','P75','P90','P95','GT_P95') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '身高桶：根据 ref_height_percentile 与学生身高映射得到',
  `sbp95` int(11) NOT NULL COMMENT '收缩压P95阈值（mmHg）',
  `dbp95` int(11) NOT NULL COMMENT '舒张压P95阈值（mmHg）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_std_sex_age_bucket`(`std_id`, `sex`, `age_year`, `height_bucket`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 177 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '【阈值】血压偏高-身高桶对应的P95阈值（WS/T 610-2018）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ref_bp_p95
-- ----------------------------
INSERT INTO `ref_bp_p95` VALUES (1, 5, 'M', 7, 'LT_P5', 108, 72);
INSERT INTO `ref_bp_p95` VALUES (2, 5, 'M', 7, 'P5', 110, 74);
INSERT INTO `ref_bp_p95` VALUES (3, 5, 'M', 7, 'P10', 112, 74);
INSERT INTO `ref_bp_p95` VALUES (4, 5, 'M', 7, 'P25', 113, 74);
INSERT INTO `ref_bp_p95` VALUES (5, 5, 'M', 7, 'P50', 115, 75);
INSERT INTO `ref_bp_p95` VALUES (6, 5, 'M', 7, 'P75', 117, 77);
INSERT INTO `ref_bp_p95` VALUES (7, 5, 'M', 7, 'P90', 118, 78);
INSERT INTO `ref_bp_p95` VALUES (8, 5, 'M', 7, 'P95', 121, 79);
INSERT INTO `ref_bp_p95` VALUES (9, 5, 'M', 8, 'LT_P5', 110, 73);
INSERT INTO `ref_bp_p95` VALUES (10, 5, 'M', 8, 'P5', 112, 75);
INSERT INTO `ref_bp_p95` VALUES (11, 5, 'M', 8, 'P10', 113, 75);
INSERT INTO `ref_bp_p95` VALUES (12, 5, 'M', 8, 'P25', 115, 76);
INSERT INTO `ref_bp_p95` VALUES (13, 5, 'M', 8, 'P50', 117, 76);
INSERT INTO `ref_bp_p95` VALUES (14, 5, 'M', 8, 'P75', 119, 78);
INSERT INTO `ref_bp_p95` VALUES (15, 5, 'M', 8, 'P90', 120, 80);
INSERT INTO `ref_bp_p95` VALUES (16, 5, 'M', 8, 'P95', 124, 80);
INSERT INTO `ref_bp_p95` VALUES (17, 5, 'M', 9, 'LT_P5', 112, 74);
INSERT INTO `ref_bp_p95` VALUES (18, 5, 'M', 9, 'P5', 114, 76);
INSERT INTO `ref_bp_p95` VALUES (19, 5, 'M', 9, 'P10', 114, 76);
INSERT INTO `ref_bp_p95` VALUES (20, 5, 'M', 9, 'P25', 117, 77);
INSERT INTO `ref_bp_p95` VALUES (21, 5, 'M', 9, 'P50', 119, 77);
INSERT INTO `ref_bp_p95` VALUES (22, 5, 'M', 9, 'P75', 121, 79);
INSERT INTO `ref_bp_p95` VALUES (23, 5, 'M', 9, 'P90', 122, 81);
INSERT INTO `ref_bp_p95` VALUES (24, 5, 'M', 9, 'P95', 126, 81);
INSERT INTO `ref_bp_p95` VALUES (25, 5, 'M', 10, 'LT_P5', 113, 75);
INSERT INTO `ref_bp_p95` VALUES (26, 5, 'M', 10, 'P5', 115, 76);
INSERT INTO `ref_bp_p95` VALUES (27, 5, 'M', 10, 'P10', 116, 76);
INSERT INTO `ref_bp_p95` VALUES (28, 5, 'M', 10, 'P25', 119, 78);
INSERT INTO `ref_bp_p95` VALUES (29, 5, 'M', 10, 'P50', 121, 78);
INSERT INTO `ref_bp_p95` VALUES (30, 5, 'M', 10, 'P75', 123, 80);
INSERT INTO `ref_bp_p95` VALUES (31, 5, 'M', 10, 'P90', 125, 81);
INSERT INTO `ref_bp_p95` VALUES (32, 5, 'M', 10, 'P95', 129, 82);
INSERT INTO `ref_bp_p95` VALUES (33, 5, 'M', 11, 'LT_P5', 115, 76);
INSERT INTO `ref_bp_p95` VALUES (34, 5, 'M', 11, 'P5', 116, 77);
INSERT INTO `ref_bp_p95` VALUES (35, 5, 'M', 11, 'P10', 117, 77);
INSERT INTO `ref_bp_p95` VALUES (36, 5, 'M', 11, 'P25', 120, 79);
INSERT INTO `ref_bp_p95` VALUES (37, 5, 'M', 11, 'P50', 123, 79);
INSERT INTO `ref_bp_p95` VALUES (38, 5, 'M', 11, 'P75', 126, 81);
INSERT INTO `ref_bp_p95` VALUES (39, 5, 'M', 11, 'P90', 128, 81);
INSERT INTO `ref_bp_p95` VALUES (40, 5, 'M', 11, 'P95', 131, 82);
INSERT INTO `ref_bp_p95` VALUES (41, 5, 'M', 12, 'LT_P5', 116, 77);
INSERT INTO `ref_bp_p95` VALUES (42, 5, 'M', 12, 'P5', 117, 77);
INSERT INTO `ref_bp_p95` VALUES (43, 5, 'M', 12, 'P10', 119, 78);
INSERT INTO `ref_bp_p95` VALUES (44, 5, 'M', 12, 'P25', 122, 79);
INSERT INTO `ref_bp_p95` VALUES (45, 5, 'M', 12, 'P50', 125, 80);
INSERT INTO `ref_bp_p95` VALUES (46, 5, 'M', 12, 'P75', 128, 81);
INSERT INTO `ref_bp_p95` VALUES (47, 5, 'M', 12, 'P90', 130, 82);
INSERT INTO `ref_bp_p95` VALUES (48, 5, 'M', 12, 'P95', 133, 83);
INSERT INTO `ref_bp_p95` VALUES (49, 5, 'M', 13, 'LT_P5', 117, 78);
INSERT INTO `ref_bp_p95` VALUES (50, 5, 'M', 13, 'P5', 119, 78);
INSERT INTO `ref_bp_p95` VALUES (51, 5, 'M', 13, 'P10', 122, 79);
INSERT INTO `ref_bp_p95` VALUES (52, 5, 'M', 13, 'P25', 125, 80);
INSERT INTO `ref_bp_p95` VALUES (53, 5, 'M', 13, 'P50', 127, 81);
INSERT INTO `ref_bp_p95` VALUES (54, 5, 'M', 13, 'P75', 130, 82);
INSERT INTO `ref_bp_p95` VALUES (55, 5, 'M', 13, 'P90', 132, 82);
INSERT INTO `ref_bp_p95` VALUES (56, 5, 'M', 13, 'P95', 134, 83);
INSERT INTO `ref_bp_p95` VALUES (57, 5, 'M', 14, 'LT_P5', 120, 79);
INSERT INTO `ref_bp_p95` VALUES (58, 5, 'M', 14, 'P5', 122, 79);
INSERT INTO `ref_bp_p95` VALUES (59, 5, 'M', 14, 'P10', 125, 80);
INSERT INTO `ref_bp_p95` VALUES (60, 5, 'M', 14, 'P25', 128, 81);
INSERT INTO `ref_bp_p95` VALUES (61, 5, 'M', 14, 'P50', 129, 82);
INSERT INTO `ref_bp_p95` VALUES (62, 5, 'M', 14, 'P75', 131, 83);
INSERT INTO `ref_bp_p95` VALUES (63, 5, 'M', 14, 'P90', 133, 83);
INSERT INTO `ref_bp_p95` VALUES (64, 5, 'M', 14, 'P95', 135, 84);
INSERT INTO `ref_bp_p95` VALUES (65, 5, 'M', 15, 'LT_P5', 123, 80);
INSERT INTO `ref_bp_p95` VALUES (66, 5, 'M', 15, 'P5', 126, 80);
INSERT INTO `ref_bp_p95` VALUES (67, 5, 'M', 15, 'P10', 128, 82);
INSERT INTO `ref_bp_p95` VALUES (68, 5, 'M', 15, 'P25', 130, 82);
INSERT INTO `ref_bp_p95` VALUES (69, 5, 'M', 15, 'P50', 131, 83);
INSERT INTO `ref_bp_p95` VALUES (70, 5, 'M', 15, 'P75', 132, 83);
INSERT INTO `ref_bp_p95` VALUES (71, 5, 'M', 15, 'P90', 133, 83);
INSERT INTO `ref_bp_p95` VALUES (72, 5, 'M', 15, 'P95', 136, 84);
INSERT INTO `ref_bp_p95` VALUES (73, 5, 'M', 16, 'LT_P5', 127, 81);
INSERT INTO `ref_bp_p95` VALUES (74, 5, 'M', 16, 'P5', 129, 82);
INSERT INTO `ref_bp_p95` VALUES (75, 5, 'M', 16, 'P10', 130, 83);
INSERT INTO `ref_bp_p95` VALUES (76, 5, 'M', 16, 'P25', 131, 83);
INSERT INTO `ref_bp_p95` VALUES (77, 5, 'M', 16, 'P50', 132, 84);
INSERT INTO `ref_bp_p95` VALUES (78, 5, 'M', 16, 'P75', 133, 84);
INSERT INTO `ref_bp_p95` VALUES (79, 5, 'M', 16, 'P90', 134, 84);
INSERT INTO `ref_bp_p95` VALUES (80, 5, 'M', 16, 'P95', 136, 85);
INSERT INTO `ref_bp_p95` VALUES (81, 5, 'M', 17, 'LT_P5', 129, 82);
INSERT INTO `ref_bp_p95` VALUES (82, 5, 'M', 17, 'P5', 131, 83);
INSERT INTO `ref_bp_p95` VALUES (83, 5, 'M', 17, 'P10', 131, 84);
INSERT INTO `ref_bp_p95` VALUES (84, 5, 'M', 17, 'P25', 132, 84);
INSERT INTO `ref_bp_p95` VALUES (85, 5, 'M', 17, 'P50', 133, 85);
INSERT INTO `ref_bp_p95` VALUES (86, 5, 'M', 17, 'P75', 134, 85);
INSERT INTO `ref_bp_p95` VALUES (87, 5, 'M', 17, 'P90', 135, 85);
INSERT INTO `ref_bp_p95` VALUES (88, 5, 'M', 17, 'P95', 136, 86);
INSERT INTO `ref_bp_p95` VALUES (89, 5, 'F', 7, 'LT_P5', 109, 73);
INSERT INTO `ref_bp_p95` VALUES (90, 5, 'F', 7, 'P5', 109, 73);
INSERT INTO `ref_bp_p95` VALUES (91, 5, 'F', 7, 'P10', 111, 74);
INSERT INTO `ref_bp_p95` VALUES (92, 5, 'F', 7, 'P25', 111, 74);
INSERT INTO `ref_bp_p95` VALUES (93, 5, 'F', 7, 'P50', 113, 74);
INSERT INTO `ref_bp_p95` VALUES (94, 5, 'F', 7, 'P75', 115, 75);
INSERT INTO `ref_bp_p95` VALUES (95, 5, 'F', 7, 'P90', 117, 76);
INSERT INTO `ref_bp_p95` VALUES (96, 5, 'F', 7, 'P95', 121, 77);
INSERT INTO `ref_bp_p95` VALUES (97, 5, 'F', 8, 'LT_P5', 110, 74);
INSERT INTO `ref_bp_p95` VALUES (98, 5, 'F', 8, 'P5', 110, 74);
INSERT INTO `ref_bp_p95` VALUES (99, 5, 'F', 8, 'P10', 113, 75);
INSERT INTO `ref_bp_p95` VALUES (100, 5, 'F', 8, 'P25', 113, 75);
INSERT INTO `ref_bp_p95` VALUES (101, 5, 'F', 8, 'P50', 115, 76);
INSERT INTO `ref_bp_p95` VALUES (102, 5, 'F', 8, 'P75', 117, 77);
INSERT INTO `ref_bp_p95` VALUES (103, 5, 'F', 8, 'P90', 119, 78);
INSERT INTO `ref_bp_p95` VALUES (104, 5, 'F', 8, 'P95', 123, 78);
INSERT INTO `ref_bp_p95` VALUES (105, 5, 'F', 9, 'LT_P5', 112, 75);
INSERT INTO `ref_bp_p95` VALUES (106, 5, 'F', 9, 'P5', 112, 76);
INSERT INTO `ref_bp_p95` VALUES (107, 5, 'F', 9, 'P10', 114, 76);
INSERT INTO `ref_bp_p95` VALUES (108, 5, 'F', 9, 'P25', 115, 77);
INSERT INTO `ref_bp_p95` VALUES (109, 5, 'F', 9, 'P50', 117, 77);
INSERT INTO `ref_bp_p95` VALUES (110, 5, 'F', 9, 'P75', 119, 78);
INSERT INTO `ref_bp_p95` VALUES (111, 5, 'F', 9, 'P90', 121, 79);
INSERT INTO `ref_bp_p95` VALUES (112, 5, 'F', 9, 'P95', 124, 81);
INSERT INTO `ref_bp_p95` VALUES (113, 5, 'F', 10, 'LT_P5', 113, 77);
INSERT INTO `ref_bp_p95` VALUES (114, 5, 'F', 10, 'P5', 114, 77);
INSERT INTO `ref_bp_p95` VALUES (115, 5, 'F', 10, 'P10', 116, 78);
INSERT INTO `ref_bp_p95` VALUES (116, 5, 'F', 10, 'P25', 117, 78);
INSERT INTO `ref_bp_p95` VALUES (117, 5, 'F', 10, 'P50', 119, 79);
INSERT INTO `ref_bp_p95` VALUES (118, 5, 'F', 10, 'P75', 121, 79);
INSERT INTO `ref_bp_p95` VALUES (119, 5, 'F', 10, 'P90', 123, 80);
INSERT INTO `ref_bp_p95` VALUES (120, 5, 'F', 10, 'P95', 125, 81);
INSERT INTO `ref_bp_p95` VALUES (121, 5, 'F', 11, 'LT_P5', 115, 78);
INSERT INTO `ref_bp_p95` VALUES (122, 5, 'F', 11, 'P5', 116, 78);
INSERT INTO `ref_bp_p95` VALUES (123, 5, 'F', 11, 'P10', 117, 79);
INSERT INTO `ref_bp_p95` VALUES (124, 5, 'F', 11, 'P25', 119, 79);
INSERT INTO `ref_bp_p95` VALUES (125, 5, 'F', 11, 'P50', 121, 79);
INSERT INTO `ref_bp_p95` VALUES (126, 5, 'F', 11, 'P75', 123, 80);
INSERT INTO `ref_bp_p95` VALUES (127, 5, 'F', 11, 'P90', 124, 81);
INSERT INTO `ref_bp_p95` VALUES (128, 5, 'F', 11, 'P95', 126, 81);
INSERT INTO `ref_bp_p95` VALUES (129, 5, 'F', 12, 'LT_P5', 116, 79);
INSERT INTO `ref_bp_p95` VALUES (130, 5, 'F', 12, 'P5', 117, 79);
INSERT INTO `ref_bp_p95` VALUES (131, 5, 'F', 12, 'P10', 119, 79);
INSERT INTO `ref_bp_p95` VALUES (132, 5, 'F', 12, 'P25', 121, 80);
INSERT INTO `ref_bp_p95` VALUES (133, 5, 'F', 12, 'P50', 122, 80);
INSERT INTO `ref_bp_p95` VALUES (134, 5, 'F', 12, 'P75', 124, 80);
INSERT INTO `ref_bp_p95` VALUES (135, 5, 'F', 12, 'P90', 125, 81);
INSERT INTO `ref_bp_p95` VALUES (136, 5, 'F', 12, 'P95', 126, 81);
INSERT INTO `ref_bp_p95` VALUES (137, 5, 'F', 13, 'LT_P5', 118, 79);
INSERT INTO `ref_bp_p95` VALUES (138, 5, 'F', 13, 'P5', 119, 79);
INSERT INTO `ref_bp_p95` VALUES (139, 5, 'F', 13, 'P10', 120, 80);
INSERT INTO `ref_bp_p95` VALUES (140, 5, 'F', 13, 'P25', 122, 80);
INSERT INTO `ref_bp_p95` VALUES (141, 5, 'F', 13, 'P50', 123, 80);
INSERT INTO `ref_bp_p95` VALUES (142, 5, 'F', 13, 'P75', 124, 80);
INSERT INTO `ref_bp_p95` VALUES (143, 5, 'F', 13, 'P90', 125, 81);
INSERT INTO `ref_bp_p95` VALUES (144, 5, 'F', 13, 'P95', 126, 81);
INSERT INTO `ref_bp_p95` VALUES (145, 5, 'F', 14, 'LT_P5', 120, 80);
INSERT INTO `ref_bp_p95` VALUES (146, 5, 'F', 14, 'P5', 120, 80);
INSERT INTO `ref_bp_p95` VALUES (147, 5, 'F', 14, 'P10', 121, 80);
INSERT INTO `ref_bp_p95` VALUES (148, 5, 'F', 14, 'P25', 122, 80);
INSERT INTO `ref_bp_p95` VALUES (149, 5, 'F', 14, 'P50', 123, 80);
INSERT INTO `ref_bp_p95` VALUES (150, 5, 'F', 14, 'P75', 124, 80);
INSERT INTO `ref_bp_p95` VALUES (151, 5, 'F', 14, 'P90', 125, 81);
INSERT INTO `ref_bp_p95` VALUES (152, 5, 'F', 14, 'P95', 126, 81);
INSERT INTO `ref_bp_p95` VALUES (153, 5, 'F', 15, 'LT_P5', 122, 80);
INSERT INTO `ref_bp_p95` VALUES (154, 5, 'F', 15, 'P5', 122, 80);
INSERT INTO `ref_bp_p95` VALUES (155, 5, 'F', 15, 'P10', 122, 80);
INSERT INTO `ref_bp_p95` VALUES (156, 5, 'F', 15, 'P25', 123, 80);
INSERT INTO `ref_bp_p95` VALUES (157, 5, 'F', 15, 'P50', 123, 80);
INSERT INTO `ref_bp_p95` VALUES (158, 5, 'F', 15, 'P75', 124, 81);
INSERT INTO `ref_bp_p95` VALUES (159, 5, 'F', 15, 'P90', 126, 82);
INSERT INTO `ref_bp_p95` VALUES (160, 5, 'F', 15, 'P95', 126, 82);
INSERT INTO `ref_bp_p95` VALUES (161, 5, 'F', 16, 'LT_P5', 122, 80);
INSERT INTO `ref_bp_p95` VALUES (162, 5, 'F', 16, 'P5', 122, 80);
INSERT INTO `ref_bp_p95` VALUES (163, 5, 'F', 16, 'P10', 122, 80);
INSERT INTO `ref_bp_p95` VALUES (164, 5, 'F', 16, 'P25', 123, 80);
INSERT INTO `ref_bp_p95` VALUES (165, 5, 'F', 16, 'P50', 123, 81);
INSERT INTO `ref_bp_p95` VALUES (166, 5, 'F', 16, 'P75', 125, 81);
INSERT INTO `ref_bp_p95` VALUES (167, 5, 'F', 16, 'P90', 126, 82);
INSERT INTO `ref_bp_p95` VALUES (168, 5, 'F', 16, 'P95', 126, 82);
INSERT INTO `ref_bp_p95` VALUES (169, 5, 'F', 17, 'LT_P5', 122, 80);
INSERT INTO `ref_bp_p95` VALUES (170, 5, 'F', 17, 'P5', 122, 80);
INSERT INTO `ref_bp_p95` VALUES (171, 5, 'F', 17, 'P10', 123, 81);
INSERT INTO `ref_bp_p95` VALUES (172, 5, 'F', 17, 'P25', 124, 81);
INSERT INTO `ref_bp_p95` VALUES (173, 5, 'F', 17, 'P50', 124, 81);
INSERT INTO `ref_bp_p95` VALUES (174, 5, 'F', 17, 'P75', 125, 81);
INSERT INTO `ref_bp_p95` VALUES (175, 5, 'F', 17, 'P90', 126, 82);
INSERT INTO `ref_bp_p95` VALUES (176, 5, 'F', 17, 'P95', 126, 82);

SET FOREIGN_KEY_CHECKS = 1;

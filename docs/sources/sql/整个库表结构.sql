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

 Date: 05/06/2026 13:09:51
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for app_upload_data
-- ----------------------------
DROP TABLE IF EXISTS `app_upload_data`;
CREATE TABLE `app_upload_data`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `state` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '状态(1-待处理数据;2-处理失败;3-处理成功;4-处理中)',
  `student_id` bigint(20) NULL DEFAULT NULL COMMENT '学生ID',
  `project_id` bigint(20) NULL DEFAULT NULL COMMENT '项目ID',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '表ID',
  `checktype_id` bigint(20) NULL DEFAULT NULL COMMENT '检查项ID',
  `data_type` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据类型(Y-原测;F-复测)',
  `data` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '解密后的数据',
  `upload_time` datetime NULL DEFAULT NULL COMMENT '上传时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2636 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for app_upload_data_log
-- ----------------------------
DROP TABLE IF EXISTS `app_upload_data_log`;
CREATE TABLE `app_upload_data_log`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `data_type` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据类型Y-原测; F-复测',
  `student_id` bigint(20) NULL DEFAULT NULL COMMENT '学生ID',
  `project_id` bigint(20) NULL DEFAULT NULL COMMENT '项目ID',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '表ID',
  `checktype_id` bigint(20) NULL DEFAULT NULL COMMENT '检查项ID',
  `upload_time` datetime NULL DEFAULT NULL COMMENT '上传时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3004 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_action
-- ----------------------------
DROP TABLE IF EXISTS `bus_action`;
CREATE TABLE `bus_action`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `school_id` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '行动学校，多选下拉，存储学校ID集合，逗号分隔',
  `action_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '行动类型，单选下拉',
  `action_topic` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '行动主题，必填，2-50字符',
  `audience_count` int(11) NOT NULL COMMENT '受众人数，1-100000（在业务代码中控制范围）',
  `action_object` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '行动对象，必填，2-50字符',
  `action_location` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '行动地点，必填，2-50字符',
  `action_date_start` date NOT NULL COMMENT '行动开始日期',
  `action_date_end` date NULL DEFAULT NULL COMMENT '行动结束日期（若为单日则与开始日期一致）',
  `action_person` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '行动人员，必填，2-50字符',
  `action_content` varchar(550) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '行动内容，必填，2-500字符',
  `action_files` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '行动资料，存储文件路径/URL，多个用逗号分隔',
  `env_assess_files` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '评估报告文件路径，逗号分隔',
  `env_follow_files` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '整改随访记录文件路径，逗号分隔',
  `parent_adv_files` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '宣传教育资料文件路径，逗号分隔',
  `parent_notice_files` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '家长告知资料文件路径，逗号分隔',
  `parent_plan_files` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '干预方案文件路径，逗号分隔',
  `parent_eval_files` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '效果评估报告文件路径，逗号分隔',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '审核状态：NULL-未提交/草稿，0-待审核，1-已通过，2-已驳回，3-已撤回',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '其他说明，0-200字符',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `division_id` int(11) NOT NULL COMMENT '区域编码',
  `dept_id` bigint(20) NULL DEFAULT NULL COMMENT '填报单位id',
  `submit_time` datetime NULL DEFAULT NULL COMMENT '提交审核时间',
  `parent_notice_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '家长告知单签收率，百分比数值如 95.00',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 94 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '行动记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_action_approval_log
-- ----------------------------
DROP TABLE IF EXISTS `bus_action_approval_log`;
CREATE TABLE `bus_action_approval_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `action_id` bigint(20) NOT NULL COMMENT '关联行动ID（bus_action.id）',
  `opinion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审批/驳回说明（驳回时建议必填）',
  `operator_id` bigint(20) NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `op_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '状态',
  `submit_time` datetime NULL DEFAULT NULL COMMENT '提交时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '行动审批日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_action_quota
-- ----------------------------
DROP TABLE IF EXISTS `bus_action_quota`;
CREATE TABLE `bus_action_quota`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `year` int(11) NOT NULL,
  `city_code` varchar(12) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `area_code` varchar(12) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `action_type` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `metric_key` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `quota_value` int(10) NOT NULL,
  `unit` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_approve_detail
-- ----------------------------
DROP TABLE IF EXISTS `bus_approve_detail`;
CREATE TABLE `bus_approve_detail`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
  `approve_id` bigint(20) NOT NULL COMMENT '对应bus_user_answer_log表的id',
  `user_id` bigint(20) NOT NULL COMMENT '审批人ID',
  `submit_time` datetime NOT NULL COMMENT '提交时间',
  `state` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审批状态，0=待审核，1=已通过，2=已驳回，3=已撤回',
  `reject_time` datetime NULL DEFAULT NULL COMMENT '驳回时间（如果被驳回才有值）',
  `reject_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '驳回意见，可为空',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，自动更新',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 185 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '审批详情表，记录审批操作的历史信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_device_qc
-- ----------------------------
DROP TABLE IF EXISTS `bus_device_qc`;
CREATE TABLE `bus_device_qc`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `org_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备所属单位',
  `device_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '电脑验光仪' COMMENT '设备类型（电脑验光仪、身高测量设备、体重测量设备、血压测量设备）',
  `device_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备编号，必填，30字符',
  `device_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备名称，必填，30字符',
  `qc_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '质控内容，必填，500字符',
  `other_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '其他说明，非必填',
  `reporter_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '填报人（当前账号医生姓名）',
  `report_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '填报时间（当前时间）',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '操作人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '设备质控信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_doctor_answer
-- ----------------------------
DROP TABLE IF EXISTS `bus_doctor_answer`;
CREATE TABLE `bus_doctor_answer`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NOT NULL COMMENT '问题id',
  `option_id` bigint(20) NULL DEFAULT NULL COMMENT '答案id',
  `student_id` bigint(20) NOT NULL COMMENT '学生id',
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '答案内容',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目id',
  `table_id` bigint(20) NOT NULL COMMENT '关联调查表id',
  `times` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `create_time`(`create_time`) USING BTREE,
  INDEX `joint_index1`(`question_id`, `student_id`, `project_id`, `table_id`) USING BTREE,
  INDEX `student_id`(`student_id`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19827 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_doctor_answer_error
-- ----------------------------
DROP TABLE IF EXISTS `bus_doctor_answer_error`;
CREATE TABLE `bus_doctor_answer_error`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `project_id` bigint(20) NULL DEFAULT NULL COMMENT '项目id',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '表id',
  `school_id` bigint(20) NULL DEFAULT NULL COMMENT '学校id',
  `student_id` bigint(20) NULL DEFAULT NULL COMMENT '学生id',
  `question_id` bigint(20) NULL DEFAULT NULL COMMENT '问题id',
  `checktype_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '检查项id',
  `type` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '问题类型',
  `result` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '原测结果',
  `retest_result` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '复测结果',
  `differ` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '差值',
  `reason` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '误差原因',
  `person` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '质控人员',
  `quality_id` bigint(20) NULL DEFAULT NULL COMMENT '质控id',
  `quality_type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '质控类型（1视力 2形态）',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 971 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_doctor_answer_error_intervene
-- ----------------------------
DROP TABLE IF EXISTS `bus_doctor_answer_error_intervene`;
CREATE TABLE `bus_doctor_answer_error_intervene`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `project_id` bigint(20) NULL DEFAULT NULL COMMENT '项目id',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '表id',
  `school_id` bigint(20) NULL DEFAULT NULL COMMENT '学校id',
  `student_id` bigint(20) NULL DEFAULT NULL COMMENT '学生id',
  `question_id` bigint(20) NULL DEFAULT NULL COMMENT '问题id',
  `checktype_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '检查项id',
  `type` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '问题类型',
  `result` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '原测结果',
  `retest_result` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '复测结果',
  `differ` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '差值',
  `reason` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '误差原因',
  `person` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '质控人员',
  `quality_id` bigint(20) NULL DEFAULT NULL COMMENT '质控id',
  `quality_type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '质控类型（1视力 2形态）',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 64 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_doctor_answer_intervene
-- ----------------------------
DROP TABLE IF EXISTS `bus_doctor_answer_intervene`;
CREATE TABLE `bus_doctor_answer_intervene`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NOT NULL COMMENT '问题id',
  `option_id` bigint(20) NULL DEFAULT NULL COMMENT '答案id',
  `student_id` bigint(20) NOT NULL COMMENT '学生id',
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '答案内容',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目id',
  `table_id` bigint(20) NOT NULL COMMENT '关联调查表id',
  `times` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `create_time`(`create_time`) USING BTREE,
  INDEX `joint_index1`(`question_id`, `student_id`, `project_id`, `table_id`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `student_id`(`student_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3351 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_doctor_answer_log
-- ----------------------------
DROP TABLE IF EXISTS `bus_doctor_answer_log`;
CREATE TABLE `bus_doctor_answer_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(16) NOT NULL COMMENT '学生id',
  `project_id` bigint(20) NOT NULL,
  `table_id` bigint(20) NOT NULL COMMENT '表id',
  `checktype_id` bigint(20) NOT NULL COMMENT '检查项id',
  `times` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `student_id`(`student_id`) USING BTREE,
  INDEX `create_time`(`create_time`) USING BTREE,
  INDEX `index1`(`project_id`, `table_id`, `student_id`, `checktype_id`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 507 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_doctor_answer_log_intervene
-- ----------------------------
DROP TABLE IF EXISTS `bus_doctor_answer_log_intervene`;
CREATE TABLE `bus_doctor_answer_log_intervene`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(16) NOT NULL COMMENT '学生id',
  `project_id` bigint(20) NOT NULL,
  `table_id` bigint(20) NOT NULL COMMENT '表id',
  `checktype_id` bigint(20) NOT NULL COMMENT '检查项id',
  `times` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `create_time`(`create_time`) USING BTREE,
  INDEX `index1`(`project_id`, `table_id`, `student_id`, `checktype_id`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `student_id`(`student_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 186 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_doctor_answer_retest
-- ----------------------------
DROP TABLE IF EXISTS `bus_doctor_answer_retest`;
CREATE TABLE `bus_doctor_answer_retest`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NOT NULL COMMENT '问题id',
  `option_id` bigint(20) NULL DEFAULT NULL COMMENT '答案id',
  `student_id` bigint(20) NOT NULL COMMENT '学生id',
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '答案内容',
  `project_id` bigint(20) NOT NULL,
  `table_id` bigint(20) NOT NULL COMMENT '关联表id',
  `times` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `student_id`(`student_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2533 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_doctor_answer_retest_intervene
-- ----------------------------
DROP TABLE IF EXISTS `bus_doctor_answer_retest_intervene`;
CREATE TABLE `bus_doctor_answer_retest_intervene`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NOT NULL COMMENT '问题id',
  `option_id` bigint(20) NULL DEFAULT NULL COMMENT '答案id',
  `student_id` bigint(20) NOT NULL COMMENT '学生id',
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '答案内容',
  `project_id` bigint(20) NOT NULL,
  `table_id` bigint(20) NOT NULL COMMENT '关联表id',
  `times` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `student_id`(`student_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 224 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_doctor_answer_retest_log
-- ----------------------------
DROP TABLE IF EXISTS `bus_doctor_answer_retest_log`;
CREATE TABLE `bus_doctor_answer_retest_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(16) NOT NULL COMMENT '问卷code码',
  `project_id` bigint(20) NOT NULL,
  `table_id` bigint(20) NOT NULL COMMENT '表id',
  `checktype_id` bigint(20) NOT NULL COMMENT '检查项id',
  `times` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `retest_log_id`(`project_id`, `table_id`, `student_id`, `checktype_id`) USING BTREE,
  INDEX `code`(`student_id`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 126 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_doctor_answer_retest_log_intervene
-- ----------------------------
DROP TABLE IF EXISTS `bus_doctor_answer_retest_log_intervene`;
CREATE TABLE `bus_doctor_answer_retest_log_intervene`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(16) NOT NULL COMMENT '问卷code码',
  `project_id` bigint(20) NOT NULL,
  `table_id` bigint(20) NOT NULL COMMENT '表id',
  `checktype_id` bigint(20) NOT NULL COMMENT '检查项id',
  `times` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `retest_log_id`(`project_id`, `table_id`, `student_id`, `checktype_id`) USING BTREE,
  INDEX `code`(`student_id`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_doctor_check_rounds
-- ----------------------------
DROP TABLE IF EXISTS `bus_doctor_check_rounds`;
CREATE TABLE `bus_doctor_check_rounds`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(20) NOT NULL COMMENT '学生id',
  `rounds` int(11) NOT NULL COMMENT '检查轮次（1原测，2复测....）',
  `school_id` bigint(20) NULL DEFAULT NULL COMMENT '学校id',
  `grade_id` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '年级id',
  `project_id` bigint(20) NOT NULL COMMENT '项目id',
  `table_id` bigint(20) NOT NULL COMMENT '调查表id',
  `check_date` date NOT NULL COMMENT '检查日期',
  `check_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '检查项',
  `check_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '检查项',
  `check_count` int(11) NOT NULL COMMENT '检查项计数',
  `start_time` datetime NOT NULL COMMENT '体检开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '体检结束时间',
  `end_flag` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '结束标记(1结束)',
  `division_id` bigint(20) NOT NULL COMMENT '学生所在地区',
  `quality_id` bigint(20) NULL DEFAULT NULL COMMENT '质控id',
  `quality_type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '质控类型（1视力 2形态）',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `check_type2` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '应检查项',
  `check_name2` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '应检查项',
  `check_count2` int(11) NULL DEFAULT NULL COMMENT '应检查多少项',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE,
  INDEX `school_id`(`school_id`) USING BTREE,
  INDEX `grade_id`(`grade_id`) USING BTREE,
  INDEX `student_id`(`student_id`) USING BTREE,
  INDEX `check_date`(`check_date`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 219 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_doctor_check_rounds_intervene
-- ----------------------------
DROP TABLE IF EXISTS `bus_doctor_check_rounds_intervene`;
CREATE TABLE `bus_doctor_check_rounds_intervene`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(20) NOT NULL COMMENT '学生id',
  `rounds` int(11) NOT NULL COMMENT '检查轮次（1原测，2复测....）',
  `school_id` bigint(20) NULL DEFAULT NULL COMMENT '学校id',
  `grade_id` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '年级id',
  `project_id` bigint(20) NOT NULL COMMENT '项目id',
  `table_id` bigint(20) NOT NULL COMMENT '调查表id',
  `check_date` date NOT NULL COMMENT '检查日期',
  `check_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '检查项',
  `check_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '检查项',
  `check_count` int(11) NOT NULL COMMENT '检查项计数',
  `start_time` datetime NOT NULL COMMENT '体检开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '体检结束时间',
  `end_flag` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '结束标记(1结束)',
  `division_id` bigint(20) NOT NULL COMMENT '学生所在地区',
  `quality_id` bigint(20) NULL DEFAULT NULL COMMENT '质控id',
  `quality_type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '质控类型（1视力 2形态）',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `check_type2` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '应检查项',
  `check_name2` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '应检查项',
  `check_count2` int(11) NULL DEFAULT NULL COMMENT '应检查多少项',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `check_date`(`check_date`) USING BTREE,
  INDEX `grade_id`(`grade_id`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `school_id`(`school_id`) USING BTREE,
  INDEX `student_id`(`student_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 56 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_evidence
-- ----------------------------
DROP TABLE IF EXISTS `bus_evidence`;
CREATE TABLE `bus_evidence`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `year` int(11) NULL DEFAULT NULL COMMENT '年度',
  `dept_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '填报单位',
  `dept_setup` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '科所设置',
  `people_setup` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '人员配置',
  `issue_plan` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '下发方案',
  `plan_drafting` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '制定计划',
  `unplanned_schools` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '未制定计划学校数',
  `kickoff_meeting` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '开启动会',
  `training_evaluation` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '培训考核',
  `document_prep` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '材料准备',
  `material_type` char(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '材料类型',
  `way` char(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '下文方式',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `expert_team` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '专家队伍',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 44 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '佐证材料表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_evidence_file
-- ----------------------------
DROP TABLE IF EXISTS `bus_evidence_file`;
CREATE TABLE `bus_evidence_file`  (
  `file_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `evidence_id` bigint(20) NOT NULL,
  `evidence_type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '佐证材料细分类型',
  `file_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `path` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `type` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文件类型',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`file_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 864 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_exam_qc
-- ----------------------------
DROP TABLE IF EXISTS `bus_exam_qc`;
CREATE TABLE `bus_exam_qc`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dept_id` bigint(20) NOT NULL COMMENT '填报单位ID（当前登录用户所在疾控单位，对应部门/单位表）',
  `school_id` bigint(20) NOT NULL COMMENT '体检学校ID（当前APP下载的学校，必选）',
  `exam_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '电脑验光' COMMENT '体检类型（八个环节，单选，默认电脑验光）',
  `exam_date` date NOT NULL COMMENT '体检日期（日历选择，默认当前日期）',
  `check_plan` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '检查方案，文本域，500字符，必填',
  `qc_plan` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '质控方案，文本域，500字符，必填',
  `issue_report` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '问题报告，文本域，500字符，非必填',
  `reporter_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '填报人（当前账号医生姓名）',
  `report_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '填报时间（当前时间）',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '操作人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '体检质控与问题报告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_file
-- ----------------------------
DROP TABLE IF EXISTS `bus_file`;
CREATE TABLE `bus_file`  (
  `file_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `filename` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `path` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`file_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 125 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_health_assessment
-- ----------------------------
DROP TABLE IF EXISTS `bus_health_assessment`;
CREATE TABLE `bus_health_assessment`  (
  `assessment_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '评估ID',
  `exam_item` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '体检项目：视力、龋齿、体格、脊柱、血压',
  `assessment_result` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '评估结果',
  `data_range` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '数据范围，逗号分隔：默认,2025,2026',
  `assessment_suggestion` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '评估建议',
  `health_education` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '健康宣教URL',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`assessment_id`) USING BTREE,
  INDEX `idx_exam_item`(`exam_item`) USING BTREE,
  INDEX `idx_assessment_result`(`assessment_result`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '体检评估管理' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_informed
-- ----------------------------
DROP TABLE IF EXISTS `bus_informed`;
CREATE TABLE `bus_informed`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(16) NULL DEFAULT NULL COMMENT '学生编码',
  `qrcode_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '扫码时间',
  `year` char(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '体检年份',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 68 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_log
-- ----------------------------
DROP TABLE IF EXISTS `bus_log`;
CREATE TABLE `bus_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '自增主键',
  `student_id` bigint(20) NULL DEFAULT NULL COMMENT '学生ID',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '表ID',
  `checktype_id` bigint(20) NULL DEFAULT NULL COMMENT '检查项ID',
  `data_type` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据类型',
  `upload_time` datetime NULL DEFAULT NULL COMMENT '上传时间',
  `data` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '解密的数据',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2846 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_logic_relation
-- ----------------------------
DROP TABLE IF EXISTS `bus_logic_relation`;
CREATE TABLE `bus_logic_relation`  (
  `id` bigint(20) NOT NULL,
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '表id',
  `question_id` bigint(20) NULL DEFAULT NULL COMMENT '问题id',
  `option_id` bigint(20) NULL DEFAULT NULL COMMENT '选项id',
  `ocode` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '选项code',
  `r_question_id` bigint(20) NULL DEFAULT NULL COMMENT '关联问题id',
  `d_question_id` varchar(5000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '隐藏的问题id',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '逻辑关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for bus_option
-- ----------------------------
DROP TABLE IF EXISTS `bus_option`;
CREATE TABLE `bus_option`  (
  `option_id` bigint(20) NOT NULL,
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '选项描述',
  `code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '选项值',
  `question_id` bigint(20) NULL DEFAULT NULL COMMENT '问题id',
  `sort` int(11) NULL DEFAULT NULL COMMENT '排序',
  `role_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '角色',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '关联表id',
  `checktype_id` bigint(20) NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`option_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '选项表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for bus_option_copy1
-- ----------------------------
DROP TABLE IF EXISTS `bus_option_copy1`;
CREATE TABLE `bus_option_copy1`  (
  `option_id` bigint(20) NOT NULL,
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '选项描述',
  `code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '选项值',
  `question_id` bigint(20) NULL DEFAULT NULL COMMENT '问题id',
  `sort` int(11) NULL DEFAULT NULL COMMENT '排序',
  `role_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '角色',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '关联表id',
  `checktype_id` bigint(20) NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`option_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '选项表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_parent_know_result
-- ----------------------------
DROP TABLE IF EXISTS `bus_parent_know_result`;
CREATE TABLE `bus_parent_know_result`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(16) NULL DEFAULT NULL COMMENT '学生id',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '问卷id',
  `create_time` datetime NULL DEFAULT NULL,
  `project_id` bigint(20) NULL DEFAULT NULL COMMENT '项目id',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_id`(`student_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 50 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_parent_view_not_confirm
-- ----------------------------
DROP TABLE IF EXISTS `bus_parent_view_not_confirm`;
CREATE TABLE `bus_parent_view_not_confirm`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `province_id` int(2) NULL DEFAULT NULL,
  `province_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `city_id` int(4) NULL DEFAULT NULL,
  `city_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `division_id` int(11) NULL DEFAULT NULL,
  `division_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `school_id` bigint(20) NULL DEFAULT NULL,
  `school_name` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `grade` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `grade_name` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `student_class` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `project_id` bigint(20) NULL DEFAULT NULL COMMENT '项目id',
  `table_id` bigint(20) NULL DEFAULT NULL,
  `student_id` bigint(16) NULL DEFAULT NULL,
  `student_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30352 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '家长查看未确认学生详情' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_parent_view_statistics
-- ----------------------------
DROP TABLE IF EXISTS `bus_parent_view_statistics`;
CREATE TABLE `bus_parent_view_statistics`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `province_id` int(2) NULL DEFAULT NULL,
  `city_id` int(4) NULL DEFAULT NULL,
  `division_id` int(11) NULL DEFAULT NULL,
  `school_id` bigint(20) NULL DEFAULT NULL,
  `grade` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `student_class` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `project_id` bigint(20) NULL DEFAULT NULL COMMENT '项目id',
  `table_id` bigint(20) NULL DEFAULT NULL,
  `count` int(11) NULL DEFAULT NULL,
  `confirm` int(11) NULL DEFAULT NULL,
  `not_confirm` int(11) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19377 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '家长查看统计中间表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_project
-- ----------------------------
DROP TABLE IF EXISTS `bus_project`;
CREATE TABLE `bus_project`  (
  `project_id` bigint(20) NOT NULL,
  `project_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `year` char(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '项目状态：0未发布，1已发布',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `alias_project_id` bigint(20) NULL DEFAULT NULL COMMENT '项目别称，2-国家级；3-省级；5-省级综合干预',
  `gen_execl` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '生成execl，0-不生成，1-生成',
  `gen_statistics` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '生成国家统计，0-不生成，1-生成',
  PRIMARY KEY (`project_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_project_division
-- ----------------------------
DROP TABLE IF EXISTS `bus_project_division`;
CREATE TABLE `bus_project_division`  (
  `project_id` bigint(20) NOT NULL,
  `division_id` bigint(20) NOT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`project_id`, `division_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_project_table
-- ----------------------------
DROP TABLE IF EXISTS `bus_project_table`;
CREATE TABLE `bus_project_table`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `project_id` bigint(20) NOT NULL,
  `table_id` bigint(20) NOT NULL,
  `school_type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `grade` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '项目状态：0启用，1禁用',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_quality_body
-- ----------------------------
DROP TABLE IF EXISTS `bus_quality_body`;
CREATE TABLE `bus_quality_body`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `project_id` bigint(20) NULL DEFAULT NULL,
  `division_id` bigint(20) NULL DEFAULT NULL,
  `school_id` bigint(20) NULL DEFAULT NULL COMMENT '学校id',
  `table_id` bigint(20) NULL DEFAULT NULL,
  `start_time` datetime NULL DEFAULT NULL COMMENT '体检开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '体检结束时间',
  `team_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '检测队名称',
  `team_leader` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '检测队长',
  `quality_person` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '质控员',
  `number` int(11) NULL DEFAULT NULL COMMENT '当日检测人数',
  `retest_number` int(11) NULL DEFAULT NULL COMMENT '复测人数（N）',
  `retest_target` int(11) NULL DEFAULT NULL COMMENT '复测指标数（A）',
  `retest_item` int(11) NULL DEFAULT NULL COMMENT '复测项次（A*N）',
  `error_item` int(11) NULL DEFAULT NULL COMMENT '错误项次数(Σn）',
  `error_rate` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '错误率（P）',
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_quality_eyesight
-- ----------------------------
DROP TABLE IF EXISTS `bus_quality_eyesight`;
CREATE TABLE `bus_quality_eyesight`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `project_id` bigint(20) NULL DEFAULT NULL,
  `division_id` bigint(20) NULL DEFAULT NULL,
  `school_id` bigint(20) NULL DEFAULT NULL COMMENT '学校id',
  `table_id` bigint(20) NULL DEFAULT NULL,
  `start_time` datetime NULL DEFAULT NULL COMMENT '体检开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '体检结束时间',
  `team_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '检测队名称',
  `team_leader` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '检测队长',
  `quality_person` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '质控员',
  `number` int(11) NULL DEFAULT NULL COMMENT '当日检测人数',
  `glasses_retest_number` int(11) NULL DEFAULT NULL COMMENT '戴镜复测人数（N1）',
  `glasses_retest_target` int(11) NULL DEFAULT NULL COMMENT '戴镜指标数（A1）',
  `no_glasses_retest_number` int(11) NULL DEFAULT NULL COMMENT '非戴镜复测人数（N2）',
  `no_glasses_retest_target` int(11) NULL DEFAULT NULL COMMENT '非戴镜复测指标数（A2）',
  `retest_item` int(11) NULL DEFAULT NULL COMMENT '复测项次（A1*N1+A2*N2）',
  `error_item` int(11) NULL DEFAULT NULL COMMENT '错误项次数(Σn）',
  `error_rate` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '错误率（P）',
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_question
-- ----------------------------
DROP TABLE IF EXISTS `bus_question`;
CREATE TABLE `bus_question`  (
  `question_id` bigint(20) NOT NULL,
  `content` varchar(300) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '问题描述',
  `type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '问题类型：（单选题，多选题、填空等）',
  `role_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '所属角色',
  `checktype_id` bigint(20) NULL DEFAULT NULL COMMENT '检查项目id（只有角色是医生时，需要此列）',
  `public_flag` int(11) NULL DEFAULT 0 COMMENT '扫码自动填写标记',
  `sort` int(11) NULL DEFAULT NULL COMMENT '顺序',
  `num` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '题号',
  `pid` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '父题id',
  `bitian` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '是否必填（0不必填，1必填）',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '关联表id',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `export_content` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '导出描述',
  `export_sort` bigint(20) NULL DEFAULT NULL COMMENT '导出顺序',
  `has_specific` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '是否特性题，默认值0（0-不是；1-是）',
  `specific_level` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '特性级别（1-地区；2-学校；3-年级），1：在特性表中仅需要有地区编码即可；2：在特性表中需同时存在地区编码及学校ID；3：在特性表中需同时存在地区编码、学校ID、年级编码；',
  `missing_value` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '缺失值，主要用于导出',
  `exprot_type` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '1' COMMENT '导出类型，用于导出（1-字符串（空字符串）；2-整数（0）；3-1位小数（0.0）；4-2位小数（0.00）；5-3位小数（0.000）；6-4位小数（0.0000）；）',
  PRIMARY KEY (`question_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '问题表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for bus_question_backup_20230807
-- ----------------------------
DROP TABLE IF EXISTS `bus_question_backup_20230807`;
CREATE TABLE `bus_question_backup_20230807`  (
  `question_id` bigint(20) NOT NULL,
  `content` varchar(300) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '问题描述',
  `type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '问题类型：（单选题，多选题、填空等）',
  `role_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '所属角色',
  `checktype_id` bigint(20) NULL DEFAULT NULL COMMENT '检查项目id（只有角色是医生时，需要此列）',
  `public_flag` int(11) NULL DEFAULT 0 COMMENT '扫码自动填写标记',
  `sort` int(11) NULL DEFAULT NULL COMMENT '顺序',
  `num` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '题号',
  `pid` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '父题id',
  `bitian` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '是否必填（0不必填，1必填）',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '关联表id',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `export_content` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '导出描述',
  `export_sort` bigint(20) NULL DEFAULT NULL COMMENT '导出顺序',
  `has_specific` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '是否特性题，默认值0（0-不是；1-是）',
  `specific_level` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '特性级别（1-地区；2-学校；3-年级），1：在特性表中仅需要有地区编码即可；2：在特性表中需同时存在地区编码及学校ID；3：在特性表中需同时存在地区编码、学校ID、年级编码；',
  `missing_value` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '缺失值，主要用于导出',
  `exprot_type` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '1' COMMENT '导出类型，用于导出（1-字符串（空字符串）；2-整数（0）；3-1位小数（0.0）；4-2位小数（0.00）；5-3位小数（0.000）；6-4位小数（0.0000）；）'
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_question_copy1
-- ----------------------------
DROP TABLE IF EXISTS `bus_question_copy1`;
CREATE TABLE `bus_question_copy1`  (
  `question_id` bigint(20) NOT NULL,
  `content` varchar(300) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '问题描述',
  `type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '问题类型：（单选题，多选题、填空等）',
  `role_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '所属角色',
  `checktype_id` bigint(20) NULL DEFAULT NULL COMMENT '检查项目id（只有角色是医生时，需要此列）',
  `public_flag` int(11) NULL DEFAULT 0 COMMENT '扫码自动填写标记',
  `sort` int(11) NULL DEFAULT NULL COMMENT '顺序',
  `num` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '题号',
  `pid` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '父题id',
  `bitian` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '是否必填（0不必填，1必填）',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '关联表id',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `export_content` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '导出描述',
  `export_sort` bigint(20) NULL DEFAULT NULL COMMENT '导出顺序',
  `has_specific` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '是否特性题，默认值0（0-不是；1-是）',
  `specific_level` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '特性级别（1-地区；2-学校；3-年级），1：在特性表中仅需要有地区编码即可；2：在特性表中需同时存在地区编码及学校ID；3：在特性表中需同时存在地区编码、学校ID、年级编码；',
  `missing_value` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '缺失值，主要用于导出',
  `exprot_type` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '1' COMMENT '导出类型，用于导出（1-字符串（空字符串）；2-整数（0）；3-1位小数（0.0）；4-2位小数（0.00）；5-3位小数（0.000）；6-4位小数（0.0000）；）',
  PRIMARY KEY (`question_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '问题表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_question_specific
-- ----------------------------
DROP TABLE IF EXISTS `bus_question_specific`;
CREATE TABLE `bus_question_specific`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NULL DEFAULT NULL COMMENT '问题ID',
  `specific_level` char(1) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '特性级别（1-地区；2-学校；3-年级）（1-division_code必填；2-division_code、school_id必填；3-division_code、school_id、grade_code必填）',
  `division_code` bigint(20) NULL DEFAULT NULL COMMENT '地区编码',
  `school_id` bigint(20) NULL DEFAULT NULL COMMENT '学校',
  `grade_code` varchar(2) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '年级',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 259 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '问题特性表，用于解决部分问题仅在部分地区才有情况。精确到学校时，地区编码不能为空吗个；精确到年级时，地区编码和学校ID均不能为空。\r\n例：2023年仅大连地区有眼轴检查项' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_region_workload_settings
-- ----------------------------
DROP TABLE IF EXISTS `bus_region_workload_settings`;
CREATE TABLE `bus_region_workload_settings`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `year` int(11) NOT NULL COMMENT '年份',
  `area_code` varchar(8) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '地区编码',
  `city_code` varchar(8) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '城市编码',
  `spot` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '监测点名称',
  `setting_type` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '设置类型(0：体检，1：问卷)',
  `school_type` tinyint(4) NOT NULL COMMENT '学校类型：0-小学，1-初中，2-普通高中，3-职业高中，4-大学，5-幼儿园',
  `people_value` int(10) NOT NULL COMMENT '人员数值',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标记 0未删除，1删除',
  `school_value` int(10) NOT NULL COMMENT '学校数值',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '修改者',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1145 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '地区工作量设置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_school
-- ----------------------------
DROP TABLE IF EXISTS `bus_school`;
CREATE TABLE `bus_school`  (
  `school_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '学校id',
  `school_name` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '部门名称',
  `order_num` int(4) NULL DEFAULT 0 COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `school_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '学校类别：bus_yschool_grade幼儿园、bus_xschool_grade小学、bus_cschool_grade初中、bus_gschool_grade高中、	bus_zschool_grade职高、bus_dschool_grade大学',
  `school_code` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '学校编码(各区/县从01开始排序，区/县内不要重复,同名学校（分校区）若按独立学校分配样本量，应单独编号)',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `division_id` bigint(20) NOT NULL COMMENT '所属地区id',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `is_intervene` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '0' COMMENT '是否参与干预，0不参加 1参加',
  PRIMARY KEY (`school_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2557 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_student
-- ----------------------------
DROP TABLE IF EXISTS `bus_student`;
CREATE TABLE `bus_student`  (
  `student_id` bigint(16) NOT NULL COMMENT '16位编码',
  `student_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '学生姓名',
  `sex` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `national` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '民族',
  `birthday` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '出生日期',
  `card` varchar(35) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '身份证号',
  `school_id` bigint(20) NOT NULL COMMENT '学校id',
  `grade` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '年级',
  `student_class` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '班级',
  `year` char(4) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '年份',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `notice` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '家长验看标识（0表示未查看，1表示查看过）',
  `division_id` int(11) NOT NULL COMMENT '所属地区id',
  `isCommonDiseaseIncluded` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '0' COMMENT '是否参与常见病检测，0参加 1不参加',
  `id_type` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '证件类型（0为居民身份证，1为其他证件）',
  PRIMARY KEY (`student_id`) USING BTREE,
  INDEX `code`(`student_id`) USING BTREE,
  INDEX `school_id`(`school_id`) USING BTREE,
  INDEX `division_id`(`division_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_student_answer
-- ----------------------------
DROP TABLE IF EXISTS `bus_student_answer`;
CREATE TABLE `bus_student_answer`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NOT NULL COMMENT '问题id',
  `option_id` bigint(20) NULL DEFAULT NULL COMMENT '答案id',
  `student_id` bigint(20) NOT NULL COMMENT '学生id',
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '答案内容',
  `project_id` bigint(20) NOT NULL,
  `table_id` bigint(20) NOT NULL COMMENT '关联表id',
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE,
  INDEX `student_id`(`student_id`) USING BTREE,
  INDEX `question_id`(`question_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12394 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_student_answer_intervene
-- ----------------------------
DROP TABLE IF EXISTS `bus_student_answer_intervene`;
CREATE TABLE `bus_student_answer_intervene`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NOT NULL COMMENT '问题id',
  `option_id` bigint(20) NULL DEFAULT NULL COMMENT '答案id',
  `student_id` bigint(20) NOT NULL COMMENT '学生id',
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '答案内容',
  `project_id` bigint(20) NOT NULL,
  `table_id` bigint(20) NOT NULL COMMENT '关联表id',
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `question_id`(`question_id`) USING BTREE,
  INDEX `student_id`(`student_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3185 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_student_answer_log
-- ----------------------------
DROP TABLE IF EXISTS `bus_student_answer_log`;
CREATE TABLE `bus_student_answer_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '问卷code码',
  `table_id` bigint(20) NOT NULL COMMENT '表id',
  `project_id` bigint(20) NOT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `student_answer_log_id`(`project_id`, `table_id`, `student_id`) USING BTREE,
  INDEX `code`(`student_id`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE,
  INDEX `create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 165 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_student_answer_log_intervene
-- ----------------------------
DROP TABLE IF EXISTS `bus_student_answer_log_intervene`;
CREATE TABLE `bus_student_answer_log_intervene`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '问卷code码',
  `table_id` bigint(20) NOT NULL COMMENT '表id',
  `project_id` bigint(20) NOT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `student_answer_log_id`(`project_id`, `table_id`, `student_id`) USING BTREE,
  INDEX `code`(`student_id`) USING BTREE,
  INDEX `create_time`(`create_time`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 53 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_student_backup
-- ----------------------------
DROP TABLE IF EXISTS `bus_student_backup`;
CREATE TABLE `bus_student_backup`  (
  `student_id` bigint(16) NOT NULL COMMENT '16位编码',
  `student_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '学生姓名',
  `sex` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `national` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '民族',
  `birthday` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '出生日期',
  `card` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '身份证号',
  `school_id` bigint(20) NOT NULL COMMENT '学校id',
  `grade` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '年级',
  `student_class` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '班级',
  `year` char(4) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '年份',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `notice` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '家长验看标识（0表示未查看，1表示查看过）',
  `division_id` int(11) NOT NULL COMMENT '所属地区id',
  `new_student_id` bigint(16) NOT NULL COMMENT '新student_id',
  `new_school_id` bigint(20) NOT NULL COMMENT '新school_id',
  `new_school_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '新增字段-学校名称',
  `new_school_code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '新增字段-学校编码',
  `new_student_class` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '新student_class',
  `new_student_grade` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '新grade',
  `new_division_id` int(11) NOT NULL COMMENT '新division_id',
  `new_student_code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '新增字段-学生编码(导入时信息)'
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_student_backup_school_grade_class
-- ----------------------------
DROP TABLE IF EXISTS `bus_student_backup_school_grade_class`;
CREATE TABLE `bus_student_backup_school_grade_class`  (
  `school_id` bigint(20) NOT NULL COMMENT '学校id',
  `grade` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '年级',
  `student_class` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '班级',
  `new_school_id` bigint(20) NOT NULL COMMENT '新school_id',
  `new_student_grade` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '新grade',
  `new_student_class` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '新student_class'
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_table
-- ----------------------------
DROP TABLE IF EXISTS `bus_table`;
CREATE TABLE `bus_table`  (
  `table_id` bigint(20) NOT NULL,
  `table_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '表名',
  `role_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '填写角色(多个用逗号隔开)',
  `respondents` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '调查对象（1学生）',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '调查表信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_table_checktype
-- ----------------------------
DROP TABLE IF EXISTS `bus_table_checktype`;
CREATE TABLE `bus_table_checktype`  (
  `id` bigint(20) NOT NULL,
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '调查表id',
  `checktype_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '检查项id',
  `checktype_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '检查项id',
  `retest_flag` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否是复测项（1是，2否）',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_traceability_qc
-- ----------------------------
DROP TABLE IF EXISTS `bus_traceability_qc`;
CREATE TABLE `bus_traceability_qc`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_table` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '关联的业务表名',
  `data_source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源来源，例如：app, import, api, system',
  `source_system` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '来源系统标识，例如：口腔、学校卫生监测、app缓存、导入、扫码',
  `op_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作类型：1=修改，2=删除',
  `operator_id` bigint(20) NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作人姓名',
  `op_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `old_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '变更前数据（明文 JSON/XML等）',
  `new_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '修改后的数据，仅修改时有值，删除时为NULL',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注说明',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据变更溯源追溯表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for bus_user_answer
-- ----------------------------
DROP TABLE IF EXISTS `bus_user_answer`;
CREATE TABLE `bus_user_answer`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NOT NULL COMMENT '问题id',
  `option_id` bigint(20) NULL DEFAULT NULL COMMENT '答案id',
  `code` bigint(10) NOT NULL COMMENT '学生id',
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '答案内容',
  `project_id` bigint(20) NULL DEFAULT NULL,
  `table_id` bigint(20) NOT NULL COMMENT '关联表id',
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE,
  INDEX `code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27605 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_user_answer_clean
-- ----------------------------
DROP TABLE IF EXISTS `bus_user_answer_clean`;
CREATE TABLE `bus_user_answer_clean`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NOT NULL COMMENT '问题id',
  `option_id` bigint(20) NULL DEFAULT NULL COMMENT '答案id',
  `code` bigint(10) NOT NULL COMMENT '学生id',
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '答案内容',
  `project_id` bigint(20) NULL DEFAULT NULL,
  `table_id` bigint(20) NOT NULL COMMENT '关联表id',
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `is_valid` tinyint(1) NULL DEFAULT NULL COMMENT '清洗是否有效：1有效，0无效',
  `clean_task_id` bigint(20) NULL DEFAULT NULL COMMENT '清洗任务ID',
  `invalid_reason` json NULL COMMENT '无效原因JSON',
  `clean_warning` json NULL COMMENT '清洗警告JSON',
  `clean_time` datetime NULL DEFAULT NULL COMMENT '清洗时间',
  `source_id` bigint(20) NULL DEFAULT NULL COMMENT '源answer表ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_source_id`(`source_id`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE,
  INDEX `code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 29152 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_user_answer_log
-- ----------------------------
DROP TABLE IF EXISTS `bus_user_answer_log`;
CREATE TABLE `bus_user_answer_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '问卷code码',
  `project_id` bigint(20) NOT NULL,
  `table_id` bigint(20) NOT NULL COMMENT '表id',
  `division_id` int(11) NULL DEFAULT NULL,
  `school_id` bigint(20) NULL DEFAULT NULL,
  `dept_id` bigint(20) NULL DEFAULT NULL,
  `year` varchar(4) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `state` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '0 待审核 1已通过 2已驳回 3已撤回 4待提交',
  `submit_time` datetime NULL DEFAULT NULL COMMENT '提交时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `code`(`code`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE,
  INDEX `project_id`(`project_id`) USING BTREE,
  INDEX `create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 147 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_version
-- ----------------------------
DROP TABLE IF EXISTS `bus_version`;
CREATE TABLE `bus_version`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `package_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'app包名',
  `version` int(10) NULL DEFAULT NULL COMMENT '版本号',
  `force_status` int(2) NULL DEFAULT NULL COMMENT '是否强制升级1强制0不强制',
  `md_detail` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文件md5',
  `remark` varchar(2000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新信息',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `path` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '下载路径',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = 'app包是否升级表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_work_statistics
-- ----------------------------
DROP TABLE IF EXISTS `bus_work_statistics`;
CREATE TABLE `bus_work_statistics`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `division_id` bigint(20) NOT NULL COMMENT '地区id',
  `project_id` bigint(20) NOT NULL COMMENT '项目id',
  `table_id` bigint(20) NOT NULL COMMENT '表id',
  `work_sum` bigint(20) NOT NULL COMMENT '工作量',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `check_date` date NULL DEFAULT NULL COMMENT '填表日期',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE,
  INDEX `idx_bus_work_statistics_project_id_check_date`(`project_id`, `check_date`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30902 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bus_work_statistics_school_grade
-- ----------------------------
DROP TABLE IF EXISTS `bus_work_statistics_school_grade`;
CREATE TABLE `bus_work_statistics_school_grade`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `project_id` bigint(20) NOT NULL COMMENT '项目id',
  `table_id` bigint(20) NOT NULL COMMENT '调查表id',
  `division_id` bigint(20) NOT NULL COMMENT '学生所在地区',
  `school_id` bigint(20) NOT NULL COMMENT '学校id',
  `grade_id` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '年级id',
  `work_sum` bigint(20) NOT NULL DEFAULT 0 COMMENT '人数',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `check_date` date NULL DEFAULT NULL COMMENT '填表日期',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bus_work_statistics_school_grade_project_id_check_date`(`project_id`, `check_date`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 55417 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for check_distribute
-- ----------------------------
DROP TABLE IF EXISTS `check_distribute`;
CREATE TABLE `check_distribute`  (
  `job_id` bigint(11) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`job_id`, `user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for check_job
-- ----------------------------
DROP TABLE IF EXISTS `check_job`;
CREATE TABLE `check_job`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_id` bigint(20) NOT NULL COMMENT '检查计划id',
  `division_id` bigint(20) NOT NULL COMMENT '地区id',
  `start_date` date NULL DEFAULT NULL COMMENT '数据开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '数据结束日期',
  `alarm_levels` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '报警范围(0:严重,1:轻微,2:可忽略)',
  `notify_methods` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '问题数据分发方式(0:站内信,1:邮件)',
  `push_method` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '分发时间(0:自动推送 1:手动推送)',
  `execute_method` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '执行方式(0:立即执行 1:定时执行)',
  `execute_time` datetime NULL DEFAULT NULL COMMENT '执行时间(当execute_method=1时有效)',
  `status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '状态(0:未开始 1:进行中 2:已完成)',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `execution_duration` int(11) NULL DEFAULT NULL COMMENT '任务执行时长（分钟）',
  `data_rows` bigint(20) NULL DEFAULT NULL COMMENT '检查数据行数',
  `serious_alarm_count` int(11) NULL DEFAULT 0 COMMENT '严重报警个数',
  `minor_alarm_count` int(11) NULL DEFAULT 0 COMMENT '轻微报警个数',
  `ignorable_alarm_count` int(11) NULL DEFAULT 0 COMMENT '可忽略报警个数',
  `distribute_status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '分发状态(0:未分发 1:已分发)',
  `distribute_desc` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '分发描述',
  `handle_status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '0' COMMENT '处理状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '检查任务' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for check_job_detail
-- ----------------------------
DROP TABLE IF EXISTS `check_job_detail`;
CREATE TABLE `check_job_detail`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_id` bigint(20) NULL DEFAULT NULL COMMENT '检查计划id',
  `job_id` bigint(20) NULL DEFAULT NULL COMMENT '检查任务id',
  `division_id` bigint(20) NOT NULL COMMENT '地区id',
  `data_time` datetime NULL DEFAULT NULL COMMENT '数据时间',
  `data_scope` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据范围',
  `data_item` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据项',
  `data_code` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据代码',
  `alarm_level` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '报警级别(0:严重,1:轻微,2:可忽略)',
  `problem_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '问题描述',
  `form_id` bigint(20) NULL DEFAULT NULL COMMENT '表单id',
  `current_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '当前数据值',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改数据值',
  `user_answer_id` bigint(20) NULL DEFAULT NULL COMMENT 'bus_user_answer表id,处理数据使用',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT 'sys_user表id,处理数据使用',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '表id',
  `question_id` bigint(20) NULL DEFAULT NULL COMMENT '问题id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '检查任务明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for clean_task
-- ----------------------------
DROP TABLE IF EXISTS `clean_task`;
CREATE TABLE `clean_task`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `table_id` bigint(20) NOT NULL,
  `answer_table` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `total_records` bigint(20) NULL DEFAULT 0,
  `valid_records` bigint(20) NULL DEFAULT 0,
  `invalid_records` bigint(20) NULL DEFAULT 0,
  `total_answer_rows` bigint(20) NULL DEFAULT 0,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `started_at` datetime NULL DEFAULT NULL,
  `finished_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dq_plan
-- ----------------------------
DROP TABLE IF EXISTS `dq_plan`;
CREATE TABLE `dq_plan`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '检查计划名称（如：表1数据检查）',
  `scope_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '质控范围（如：表2-1）',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1=启用，2=停用',
  `del_flag` tinyint(4) NOT NULL DEFAULT 1 COMMENT '逻辑删除：1=未删除，2=已删除',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据质量检查计划主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dq_plan_item
-- ----------------------------
DROP TABLE IF EXISTS `dq_plan_item`;
CREATE TABLE `dq_plan_item`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_id` bigint(20) NOT NULL COMMENT '关联 dq_plan.id',
  `data_item` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据项名称（学生编码/性别/裸眼视力等）',
  `alarm_level` tinyint(4) NOT NULL COMMENT '报警级别：1=严重 2=轻微 3=可忽略',
  `data_codes` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据代码（物理字段名，多个用逗号）',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1=启用 2=停用',
  `del_flag` tinyint(4) NOT NULL DEFAULT 1 COMMENT '逻辑删除：1=未删除 2=已删除',
  `sort_no` int(11) NULL DEFAULT NULL COMMENT '排序号',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_dq_plan_item_plan_id`(`plan_id`) USING BTREE,
  CONSTRAINT `fk_dq_plan_item_plan_id` FOREIGN KEY (`plan_id`) REFERENCES `dq_plan` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据质量检查项表（含数据代码）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dq_plan_rule
-- ----------------------------
DROP TABLE IF EXISTS `dq_plan_rule`;
CREATE TABLE `dq_plan_rule`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_id` bigint(20) NOT NULL COMMENT '关联 dq_plan_item.id',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则名称（不为空、长度范围等）',
  `rule_expr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则表达式（notnull/len/enum/range）',
  `display_msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '提示信息',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1=启用 2=停用',
  `del_flag` tinyint(4) NOT NULL DEFAULT 1 COMMENT '逻辑删除：1=未删除 2=已删除',
  `sort_no` int(11) NULL DEFAULT NULL COMMENT '规则排序',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_dq_plan_rule_item_id`(`item_id`) USING BTREE,
  CONSTRAINT `fk_dq_plan_rule_item_id` FOREIGN KEY (`item_id`) REFERENCES `dq_plan_item` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据质量规则明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table`  (
  `table_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作 sub主子表操作）',
  `package_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '代码生成业务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column`  (
  `column_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '字典类型',
  `sort` int(11) NULL DEFAULT NULL COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 423 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '代码生成业务表字段' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for log_statistics
-- ----------------------------
DROP TABLE IF EXISTS `log_statistics`;
CREATE TABLE `log_statistics`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint(20) NULL DEFAULT NULL,
  `table_id` bigint(20) NULL DEFAULT NULL,
  `table_id2` bigint(20) NULL DEFAULT NULL,
  `division_id` bigint(20) NULL DEFAULT NULL,
  `execution_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '执行时间',
  `time_consuming` bigint(20) NULL DEFAULT NULL,
  `row_count` bigint(20) NULL DEFAULT NULL,
  `method` smallint(6) NULL DEFAULT NULL COMMENT '1-统计 2-生成execl',
  `status` smallint(6) NULL DEFAULT NULL COMMENT '0-成功 1-失败 2-未执行',
  `remark` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1377240 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_blob_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_blob_triggers`;
CREATE TABLE `qrtz_blob_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `trigger_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `trigger_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `blob_data` blob NULL,
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_calendars
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_calendars`;
CREATE TABLE `qrtz_calendars`  (
  `sched_name` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `calendar_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `calendar` blob NOT NULL,
  PRIMARY KEY (`sched_name`, `calendar_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_cron_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_cron_triggers`;
CREATE TABLE `qrtz_cron_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `trigger_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `trigger_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `cron_expression` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `time_zone_id` varchar(80) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_fired_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_fired_triggers`;
CREATE TABLE `qrtz_fired_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `entry_id` varchar(95) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `trigger_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `trigger_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `instance_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `fired_time` bigint(13) NOT NULL,
  `sched_time` bigint(13) NOT NULL,
  `priority` int(11) NOT NULL,
  `state` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `job_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `job_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `requests_recovery` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`sched_name`, `entry_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_job_details
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_job_details`;
CREATE TABLE `qrtz_job_details`  (
  `sched_name` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `job_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `job_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `description` varchar(250) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `job_class_name` varchar(250) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `is_durable` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `is_update_data` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `requests_recovery` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `job_data` blob NULL,
  PRIMARY KEY (`sched_name`, `job_name`, `job_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_locks
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_locks`;
CREATE TABLE `qrtz_locks`  (
  `sched_name` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `lock_name` varchar(40) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`sched_name`, `lock_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_paused_trigger_grps
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
CREATE TABLE `qrtz_paused_trigger_grps`  (
  `sched_name` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `trigger_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`sched_name`, `trigger_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_scheduler_state
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_scheduler_state`;
CREATE TABLE `qrtz_scheduler_state`  (
  `sched_name` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `instance_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `last_checkin_time` bigint(13) NOT NULL,
  `checkin_interval` bigint(13) NOT NULL,
  PRIMARY KEY (`sched_name`, `instance_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_simple_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simple_triggers`;
CREATE TABLE `qrtz_simple_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `trigger_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `trigger_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `repeat_count` bigint(7) NOT NULL,
  `repeat_interval` bigint(12) NOT NULL,
  `times_triggered` bigint(10) NOT NULL,
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_simprop_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
CREATE TABLE `qrtz_simprop_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `trigger_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `trigger_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `str_prop_1` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `str_prop_2` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `str_prop_3` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `int_prop_1` int(11) NULL DEFAULT NULL,
  `int_prop_2` int(11) NULL DEFAULT NULL,
  `long_prop_1` bigint(20) NULL DEFAULT NULL,
  `long_prop_2` bigint(20) NULL DEFAULT NULL,
  `dec_prop_1` decimal(13, 4) NULL DEFAULT NULL,
  `dec_prop_2` decimal(13, 4) NULL DEFAULT NULL,
  `bool_prop_1` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `bool_prop_2` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_triggers`;
CREATE TABLE `qrtz_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `trigger_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `trigger_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `job_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `job_group` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `description` varchar(250) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `next_fire_time` bigint(13) NULL DEFAULT NULL,
  `prev_fire_time` bigint(13) NULL DEFAULT NULL,
  `priority` int(11) NULL DEFAULT NULL,
  `trigger_state` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `trigger_type` varchar(8) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `start_time` bigint(13) NOT NULL,
  `end_time` bigint(13) NULL DEFAULT NULL,
  `calendar_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `misfire_instr` smallint(2) NULL DEFAULT NULL,
  `job_data` blob NULL,
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  INDEX `sched_name`(`sched_name`, `job_name`, `job_group`) USING BTREE,
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for question_copy
-- ----------------------------
DROP TABLE IF EXISTS `question_copy`;
CREATE TABLE `question_copy`  (
  `question_id` bigint(20) NOT NULL,
  `content` varchar(300) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '问题描述',
  `type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '问题类型：（单选题，多选题、填空等）',
  `role_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '所属角色',
  `checktype_id` bigint(20) NULL DEFAULT NULL COMMENT '检查项目id（只有角色是医生时，需要此列）',
  `public_flag` int(11) NULL DEFAULT 0 COMMENT '扫码自动填写标记',
  `sort` int(11) NULL DEFAULT NULL COMMENT '顺序',
  `num` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '题号',
  `pid` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '父题id',
  `bitian` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '是否必填（0不必填，1必填）',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '关联表id',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `export_content` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '导出描述',
  `export_sort` bigint(20) NULL DEFAULT NULL COMMENT '导出顺序',
  `has_specific` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '是否特性题，默认值0（0-不是；1-是）',
  `specific_level` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '特性级别（1-地区；2-学校；3-年级），1：在特性表中仅需要有地区编码即可；2：在特性表中需同时存在地区编码及学校ID；3：在特性表中需同时存在地区编码、学校ID、年级编码；',
  `missing_value` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '缺失值，主要用于导出',
  `exprot_type` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '1' COMMENT '导出类型，用于导出（1-字符串（空字符串）；2-整数（0）；3-1位小数（0.0）；4-2位小数（0.00）；5-3位小数（0.000）；6-4位小数（0.0000）；）',
  `new_question_id` bigint(20) NOT NULL,
  PRIMARY KEY (`question_id`) USING BTREE,
  INDEX `table_id`(`table_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '问题表' ROW_FORMAT = Dynamic;

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
-- Table structure for std_version
-- ----------------------------
DROP TABLE IF EXISTS `std_version`;
CREATE TABLE `std_version`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标准代码：建议唯一且可读，如 WS586_2018 / WS456_2014 / WS610_2018 / VISION_RULES',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标准名称：便于人读，如“WS/T 586-2018 学龄儿童青少年超重与肥胖筛查”',
  `source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标准来源/出处：可填标准文件名、链接或内部文档说明',
  `effective_from` date NULL DEFAULT NULL COMMENT '标准生效日期（含）；可为空表示未知或全量适用',
  `effective_to` date NULL DEFAULT NULL COMMENT '标准失效日期（不含）；为空表示当前仍在使用',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用、0禁用',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code`) USING BTREE,
  INDEX `idx_active`(`is_active`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '【主数据】各判定标准的版本信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `config_id` int(5) NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '参数配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `dept_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint(20) NULL DEFAULT 0 COMMENT '父部门id',
  `ancestors` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '部门名称',
  `order_num` int(4) NULL DEFAULT 0 COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `division_id` int(11) NULL DEFAULT NULL COMMENT '所属地区id',
  `short_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`dept_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 233 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `dict_code` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int(4) NULL DEFAULT 0 COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 181 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '字典数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`  (
  `dict_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '字典类型',
  `status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`) USING BTREE,
  UNIQUE INDEX `dict_type`(`dict_type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '字典类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_divisionmsg
-- ----------------------------
DROP TABLE IF EXISTS `sys_divisionmsg`;
CREATE TABLE `sys_divisionmsg`  (
  `division_id` bigint(20) NOT NULL COMMENT '地区代码',
  `division_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '地区名称',
  `area` int(1) NULL DEFAULT NULL COMMENT '片区',
  `spot` int(1) NULL DEFAULT NULL COMMENT '监测点（城区1，郊县/区2）',
  `province_plan` int(1) NOT NULL DEFAULT 0 COMMENT '是否全省计划 0是，1不是',
  `parent_id` bigint(20) NULL DEFAULT NULL COMMENT '父id',
  `sort` int(11) NULL DEFAULT NULL COMMENT '排序列',
  `del_flag` int(1) NOT NULL COMMENT '删除标记 0未删除，1删除',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '修改者',
  `remark` text CHARACTER SET utf8 COLLATE utf8_bin NULL COMMENT '备注',
  PRIMARY KEY (`division_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_job
-- ----------------------------
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job`  (
  `job_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1暂停）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '备注信息',
  PRIMARY KEY (`job_id`, `job_name`, `job_group`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '定时任务调度表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_job_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log`  (
  `job_log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `job_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '日志信息',
  `status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '异常信息',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_log_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '定时任务调度日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_logininfor
-- ----------------------------
DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor`  (
  `info_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `login_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '登录账号',
  `ipaddr` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '操作系统',
  `status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '提示消息',
  `login_time` datetime NULL DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2996 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统访问记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `menu_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint(20) NULL DEFAULT 0 COMMENT '父菜单ID',
  `order_num` int(4) NULL DEFAULT 0 COMMENT '显示顺序',
  `url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '#' COMMENT '请求地址',
  `target` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '打开方式（menuItem页签 menuBlank新窗口）',
  `menu_type` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `perms` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11308 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '菜单权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
  `oper_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '模块标题',
  `business_type` int(2) NULL DEFAULT 0 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '请求方式',
  `operator_type` int(1) NULL DEFAULT 0 COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '返回参数',
  `status` int(1) NULL DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
  PRIMARY KEY (`oper_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 78 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '操作日志记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
  `post_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int(4) NOT NULL COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '岗位信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `role_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int(4) NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '角色信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `dept_id` bigint(20) NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`, `dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '角色和部门关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '角色和菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint(20) NULL DEFAULT NULL COMMENT '部门ID',
  `login_name` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '登录账号',
  `user_name` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '用户昵称',
  `user_type` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '00' COMMENT '用户类型（00系统用户 01注册用户）',
  `email` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '头像路径',
  `password` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '密码',
  `salt` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '盐加密',
  `status` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '最后登陆IP',
  `login_date` datetime NULL DEFAULT NULL COMMENT '最后登陆时间',
  `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8735 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user_online
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_online`;
CREATE TABLE `sys_user_online`  (
  `sessionId` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '用户会话id',
  `login_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '登录账号',
  `dept_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '部门名称',
  `ipaddr` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '操作系统',
  `status` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '在线状态on_line在线off_line离线',
  `start_timestamp` datetime NULL DEFAULT NULL COMMENT 'session创建时间',
  `last_access_time` datetime NULL DEFAULT NULL COMMENT 'session最后访问时间',
  `expire_time` int(5) NULL DEFAULT 0 COMMENT '超时时间，单位为分钟',
  PRIMARY KEY (`sessionId`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '在线用户记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`  (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `post_id` bigint(20) NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`, `post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '用户与岗位关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '用户和角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for wx_upload_data
-- ----------------------------
DROP TABLE IF EXISTS `wx_upload_data`;
CREATE TABLE `wx_upload_data`  (
  `c_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `c_state` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '状态(1-待处理数据;2-处理失败;3-处理成功;4-处理中)',
  `student_id` bigint(20) NULL DEFAULT NULL COMMENT '学生ID',
  `project_id` bigint(20) NULL DEFAULT NULL COMMENT '项目ID',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '表ID',
  `c_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'data数据',
  `c_log_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'log_data数据',
  `c_year` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '年份',
  `c_upload_time` datetime NULL DEFAULT NULL COMMENT '上传时间',
  PRIMARY KEY (`c_id`) USING BTREE,
  INDEX `idx_student_id`(`student_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 60 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for wx_upload_data_log
-- ----------------------------
DROP TABLE IF EXISTS `wx_upload_data_log`;
CREATE TABLE `wx_upload_data_log`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `student_id` bigint(20) NULL DEFAULT NULL COMMENT '学生ID',
  `project_id` bigint(20) NULL DEFAULT NULL COMMENT '项目ID',
  `table_id` bigint(20) NULL DEFAULT NULL COMMENT '表ID',
  `upload_time` datetime NULL DEFAULT NULL COMMENT '上传时间',
  `c_year` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '年份',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_id`(`student_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 172 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for yhsj
-- ----------------------------
DROP TABLE IF EXISTS `yhsj`;
CREATE TABLE `yhsj`  (
  `用户ID` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `bmbh` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `dlmc` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `用户名称` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `账号状态` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `用户角色ID` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `体检人员检查项` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- View structure for v_vision_stat
-- ----------------------------
DROP VIEW IF EXISTS `v_vision_stat`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `v_vision_stat` AS select `t`.`project_id` AS `project_id`,`t`.`data_year` AS `data_year`,`t`.`student_id` AS `student_id`,`t`.`school_id` AS `school_id`,`t`.`grade` AS `grade`,`t`.`sex` AS `sex`,`t`.`birthday` AS `birthday`,(case when (isnull(`t`.`birthday`) or (`t`.`birthday` = '')) then NULL else (cast(year(now()) as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) end) AS `age`,`t`.`division_code` AS `division_code`,left(`t`.`division_code`,2) AS `province_code`,left(`t`.`division_code`,4) AS `city_code`,left(`t`.`division_code`,6) AS `county_code`,`t`.`src_table_id` AS `src_table_id`,`t`.`vision_r` AS `vision_r`,`t`.`vision_l` AS `vision_l`,`t`.`glass_r` AS `glass_r`,`t`.`glass_l` AS `glass_l`,`t`.`ser` AS `ser`,`t`.`sel` AS `sel`,`t`.`glasses_type_code` AS `glasses_type_code`,`t`.`okr` AS `okr`,`t`.`okl` AS `okl`,`t`.`naked_min` AS `naked_min`,(case when ((`t`.`vision_r` is not null) or (`t`.`vision_l` is not null)) then 1 else 0 end) AS `has_vision`,(case when isnull(`t`.`naked_min`) then 0 when (`t`.`naked_min` < 5.0) then 1 else 0 end) AS `lowvision_flag`,(case when isnull(`t`.`naked_min`) then NULL when (`t`.`naked_min` >= 5.0) then '正常' when (`t`.`naked_min` = 4.9) then '轻度' when (`t`.`naked_min` between 4.6 and 4.8) then '中度' else '重度' end) AS `lowvision_level`,(case when (`t`.`glasses_type_code` = '3') then 1 when ((`t`.`okr` is not null) or (`t`.`okl` is not null)) then 1 else 0 end) AS `ortho_flag`,(case when isnull(`t`.`naked_min`) then 0 when (`t`.`src_table_id` = 4) then (case when (`t`.`naked_min` < 5.0) then 1 else 0 end) else (case when ((`t`.`naked_min` < 5.0) and ((`t`.`ser` < -(0.50)) or (`t`.`sel` < -(0.50)))) then 1 when ((case when (`t`.`glasses_type_code` = '3') then 1 when ((`t`.`okr` is not null) or (`t`.`okl` is not null)) then 1 else 0 end) = 1) then 1 else 0 end) end) AS `myopia_flag`,(case when ((case when isnull(`t`.`naked_min`) then 0 when (`t`.`src_table_id` = 4) then (case when (`t`.`naked_min` < 5.0) then 1 else 0 end) else (case when ((`t`.`naked_min` < 5.0) and ((`t`.`ser` < -(0.50)) or (`t`.`sel` < -(0.50)))) then 1 when ((`t`.`glasses_type_code` = '3') or (`t`.`okr` is not null) or (`t`.`okl` is not null)) then 1 else 0 end) end) = 0) then NULL when (`t`.`naked_min` between 4.7 and 4.9) then '低度' when (`t`.`naked_min` between 4.4 and 4.6) then '中度' when (`t`.`naked_min` <= 4.3) then '高度' else '未知' end) AS `myopia_degree`,(case when isnull(`t`.`naked_min`) then NULL when ((`t`.`grade` in ('01','02')) and (`t`.`naked_min` < 5.0)) then 1 when ((`t`.`grade` in ('03','04','05','06')) and (`t`.`naked_min` < 4.9)) then 2 when ((`t`.`grade` in ('31','32','33','41','42','43','44','53')) and (`t`.`naked_min` < 4.8)) then 3 else NULL end) AS `warn_level`,(case when ((case when (isnull(`t`.`birthday`) or (`t`.`birthday` = '')) then NULL else (cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) end) between 3 and 5) then 2.05 when ((case when (isnull(`t`.`birthday`) or (`t`.`birthday` = '')) then NULL else (cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) end) = 6) then 1.72 when ((case when (isnull(`t`.`birthday`) or (`t`.`birthday` = '')) then NULL else (cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) end) = 7) then 1.48 when ((case when (isnull(`t`.`birthday`) or (`t`.`birthday` = '')) then NULL else (cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) end) = 8) then 1.19 when ((case when (isnull(`t`.`birthday`) or (`t`.`birthday` = '')) then NULL else (cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) end) = 9) then 1.11 when ((case when (isnull(`t`.`birthday`) or (`t`.`birthday` = '')) then NULL else (cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) end) = 10) then 0.92 when ((case when (isnull(`t`.`birthday`) or (`t`.`birthday` = '')) then NULL else (cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) end) = 11) then 0.83 when ((case when (isnull(`t`.`birthday`) or (`t`.`birthday` = '')) then NULL else (cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) end) = 12) then 0.61 when ((case when (isnull(`t`.`birthday`) or (`t`.`birthday` = '')) then NULL else (cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) end) = 13) then 0.27 when ((case when (isnull(`t`.`birthday`) or (`t`.`birthday` = '')) then NULL else (cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) end) >= 14) then 0.09 else NULL end) AS `reserve_lower`,(case when isnull(`t`.`naked_min`) then 0 when (`t`.`naked_min` < 5.0) then 0 when (isnull(`t`.`ser`) or isnull(`t`.`sel`)) then 0 else (case when (least(`t`.`ser`,`t`.`sel`) < (case when ((cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) between 3 and 5) then 2.05 when ((cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) = 6) then 1.72 when ((cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) = 7) then 1.48 when ((cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) = 8) then 1.19 when ((cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) = 9) then 1.11 when ((cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) = 10) then 0.92 when ((cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) = 11) then 0.83 when ((cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) = 12) then 0.61 when ((cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) = 13) then 0.27 when ((cast(`t`.`data_year` as unsigned) - year(str_to_date(`t`.`birthday`,'%Y-%m-%d'))) >= 14) then 0.09 else NULL end)) then 1 else 0 end) end) AS `reserve_insuff_flag` from (select `a`.`project_id` AS `project_id`,`a`.`year` AS `data_year`,`a`.`student_id` AS `student_id`,`s`.`school_id` AS `school_id`,`s`.`grade` AS `grade`,`s`.`sex` AS `sex`,`s`.`birthday` AS `birthday`,left(lpad(cast(`s`.`division_id` as char charset utf8mb4),6,'0'),6) AS `division_code`,max(`a`.`table_id`) AS `src_table_id`,max((case when (`q`.`export_content` = 'visionR') then cast(nullif(nullif(`a`.`content`,'9'),'') as decimal(3,1)) end)) AS `vision_r`,max((case when (`q`.`export_content` = 'visionL') then cast(nullif(nullif(`a`.`content`,'9'),'') as decimal(3,1)) end)) AS `vision_l`,max((case when (`q`.`export_content` = 'glassR') then cast(nullif(nullif(`a`.`content`,'9'),'') as decimal(3,1)) end)) AS `glass_r`,max((case when (`q`.`export_content` = 'glassL') then cast(nullif(nullif(`a`.`content`,'9'),'') as decimal(3,1)) end)) AS `glass_l`,max((case when (`q`.`export_content` = 'SER') then cast(nullif(nullif(`a`.`content`,'999'),'') as decimal(6,2)) end)) AS `ser`,max((case when (`q`.`export_content` = 'SEL') then cast(nullif(nullif(`a`.`content`,'999'),'') as decimal(6,2)) end)) AS `sel`,max((case when (`q`.`export_content` = 'glasstype') then `o`.`code` end)) AS `glasses_type_code`,max((case when (`q`.`export_content` = 'OKR') then cast(nullif(nullif(`a`.`content`,'999'),'') as decimal(6,2)) end)) AS `okr`,max((case when (`q`.`export_content` = 'OKL') then cast(nullif(nullif(`a`.`content`,'999'),'') as decimal(6,2)) end)) AS `okl`,(case when (isnull(max((case when (`q`.`export_content` = 'visionR') then cast(nullif(nullif(`a`.`content`,'9'),'') as decimal(3,1)) end))) and isnull(max((case when (`q`.`export_content` = 'visionL') then cast(nullif(nullif(`a`.`content`,'9'),'') as decimal(3,1)) end)))) then NULL when isnull(max((case when (`q`.`export_content` = 'visionR') then cast(nullif(nullif(`a`.`content`,'9'),'') as decimal(3,1)) end))) then max((case when (`q`.`export_content` = 'visionL') then cast(nullif(nullif(`a`.`content`,'9'),'') as decimal(3,1)) end)) when isnull(max((case when (`q`.`export_content` = 'visionL') then cast(nullif(nullif(`a`.`content`,'9'),'') as decimal(3,1)) end))) then max((case when (`q`.`export_content` = 'visionR') then cast(nullif(nullif(`a`.`content`,'9'),'') as decimal(3,1)) end)) else least(max((case when (`q`.`export_content` = 'visionR') then cast(nullif(nullif(`a`.`content`,'9'),'') as decimal(3,1)) end)),max((case when (`q`.`export_content` = 'visionL') then cast(nullif(nullif(`a`.`content`,'9'),'') as decimal(3,1)) end))) end) AS `naked_min` from (((`healthdetection_2025`.`bus_doctor_answer` `a` join `healthdetection_2025`.`bus_question` `q` on(((`q`.`question_id` = `a`.`question_id`) and (`q`.`del_flag` = '0')))) left join `healthdetection_2025`.`bus_option` `o` on(((`o`.`option_id` = `a`.`option_id`) and (`o`.`del_flag` = '0')))) join `healthdetection_2025`.`bus_student` `s` on(((`s`.`student_id` = `a`.`student_id`) and (`s`.`del_flag` = '0')))) where ((`a`.`del_flag` = '0') and (`a`.`table_id` in (3,4,5))) group by `a`.`project_id`,`a`.`year`,`a`.`student_id`) `t`;

-- ----------------------------
-- Procedure structure for scan_string_date_columns
-- ----------------------------
DROP PROCEDURE IF EXISTS `scan_string_date_columns`;
delimiter ;;
CREATE PROCEDURE `scan_string_date_columns`()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_table  VARCHAR(64);
    DECLARE v_column VARCHAR(64);

    DECLARE cur CURSOR FOR
        SELECT TABLE_NAME, COLUMN_NAME
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'healthdetection_2025'
          AND DATA_TYPE IN ('char','varchar','text','tinytext','mediumtext','longtext');

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    -- 每次扫描前清空结果
    TRUNCATE TABLE suspect_date_columns;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO v_table, v_column;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET @sql = CONCAT(
            'INSERT INTO suspect_date_columns ',
            '  (table_name, column_name, total_rows, date_like_rows, sample_limit, check_formats) ',
            'SELECT ',
            '  ''', v_table, ''' AS table_name, ',
            '  ''', v_column, ''' AS column_name, ',
            '  COUNT(*) AS total_rows, ',
            '  SUM(CASE ',
            '        WHEN STR_TO_DATE(`', v_column, '` , ''%Y-%m-%d'') IS NOT NULL ',
            '          OR STR_TO_DATE(`', v_column, '` , ''%Y-%m-%d %H:%i:%s'') IS NOT NULL ',
            '          OR STR_TO_DATE(`', v_column, '` , ''%Y/%m/%d'') IS NOT NULL ',
            '          OR STR_TO_DATE(`', v_column, '` , ''%Y%m%d'') IS NOT NULL ',
            '        THEN 1 ELSE 0 ',
            '      END) AS date_like_rows, ',
            '  1000 AS sample_limit, ',
            '  ''%Y-%m-%d,%Y-%m-%d %H:%i:%s,%Y/%m/%d,%Y%m%d'' AS check_formats ',
            'FROM `healthdetection_2025`.`', v_table, '` ',
            'WHERE `', v_column, '` IS NOT NULL ',
            '  AND `', v_column, '` <> '''' ',
            'LIMIT 1000'
        );

        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;

    CLOSE cur;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for scan_string_date_columns_safe
-- ----------------------------
DROP PROCEDURE IF EXISTS `scan_string_date_columns_safe`;
delimiter ;;
CREATE PROCEDURE `scan_string_date_columns_safe`()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_table  VARCHAR(128);
    DECLARE v_column VARCHAR(128);

    -- 所有字符串列
    DECLARE cur CURSOR FOR
        SELECT TABLE_NAME, COLUMN_NAME
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'healthdetection_2025'
          AND DATA_TYPE IN ('char','varchar','text','tinytext','mediumtext','longtext');

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    TRUNCATE TABLE suspect_date_columns;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_table, v_column;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET @sql = CONCAT(
            'INSERT INTO suspect_date_columns ',
            '  (table_name, column_name, total_rows, date_like_rows, ratio, sample_limit, checked_formats) ',
            'SELECT ',
            '  ''', v_table, ''', ',
            '  ''', v_column, ''', ',
            '  COUNT(*) AS total_rows, ',

            -- ================== date_like_rows：加 IFNULL，避免 NULL ==================
            '  IFNULL(SUM( CASE ',
            '        WHEN (',
            -- 1) YYYY-MM-DD
            '              `', v_column, '` REGEXP ''^[0-9]{4}-[0-9]{2}-[0-9]{2}$'' ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,1,4) AS UNSIGNED) BETWEEN 1900 AND 2100 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,6,2) AS UNSIGNED) BETWEEN 1 AND 12 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,9,2) AS UNSIGNED) BETWEEN 1 AND 31 ',
            '        ) ',
            '        OR (',
            -- 2) YYYY-MM-DD HH:MM:SS
            '              `', v_column, '` REGEXP ''^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$'' ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,1,4) AS UNSIGNED) BETWEEN 1900 AND 2100 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,6,2) AS UNSIGNED) BETWEEN 1 AND 12 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,9,2) AS UNSIGNED) BETWEEN 1 AND 31 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,12,2) AS UNSIGNED) BETWEEN 0 AND 23 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,15,2) AS UNSIGNED) BETWEEN 0 AND 59 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,18,2) AS UNSIGNED) BETWEEN 0 AND 59 ',
            '        ) ',
            '        OR (',
            -- 3) YYYY/MM/DD
            '              `', v_column, '` REGEXP ''^[0-9]{4}/[0-9]{2}/[0-9]{2}$'' ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,1,4) AS UNSIGNED) BETWEEN 1900 AND 2100 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,6,2) AS UNSIGNED) BETWEEN 1 AND 12 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,9,2) AS UNSIGNED) BETWEEN 1 AND 31 ',
            '        ) ',
            '        OR (',
            -- 4) YYYYMMDD
            '              `', v_column, '` REGEXP ''^[0-9]{8}$'' ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,1,4) AS UNSIGNED) BETWEEN 1900 AND 2100 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,5,2) AS UNSIGNED) BETWEEN 1 AND 12 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,7,2) AS UNSIGNED) BETWEEN 1 AND 31 ',
            '        ) ',
            '      THEN 1 ELSE 0 ',
            '      END ),0) AS date_like_rows, ',
            -- ================== date_like_rows 结束 ==================

            -- ================== ratio：同样加 IFNULL，空表时给 0 ==================
            '  ROUND( ',
            '    IFNULL( ',
            '      SUM( CASE ',
            '        WHEN (',
            '              `', v_column, '` REGEXP ''^[0-9]{4}-[0-9]{2}-[0-9]{2}$'' ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,1,4) AS UNSIGNED) BETWEEN 1900 AND 2100 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,6,2) AS UNSIGNED) BETWEEN 1 AND 12 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,9,2) AS UNSIGNED) BETWEEN 1 AND 31 ',
            '        ) ',
            '        OR (',
            '              `', v_column, '` REGEXP ''^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$'' ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,1,4) AS UNSIGNED) BETWEEN 1900 AND 2100 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,6,2) AS UNSIGNED) BETWEEN 1 AND 12 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,9,2) AS UNSIGNED) BETWEEN 1 AND 31 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,12,2) AS UNSIGNED) BETWEEN 0 AND 23 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,15,2) AS UNSIGNED) BETWEEN 0 AND 59 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,18,2) AS UNSIGNED) BETWEEN 0 AND 59 ',
            '        ) ',
            '        OR (',
            '              `', v_column, '` REGEXP ''^[0-9]{4}/[0-9]{2}/[0-9]{2}$'' ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,1,4) AS UNSIGNED) BETWEEN 1900 AND 2100 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,6,2) AS UNSIGNED) BETWEEN 1 AND 12 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,9,2) AS UNSIGNED) BETWEEN 1 AND 31 ',
            '        ) ',
            '        OR (',
            '              `', v_column, '` REGEXP ''^[0-9]{8}$'' ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,1,4) AS UNSIGNED) BETWEEN 1900 AND 2100 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,5,2) AS UNSIGNED) BETWEEN 1 AND 12 ',
            '          AND  CAST(SUBSTRING(`', v_column, '`,7,2) AS UNSIGNED) BETWEEN 1 AND 31 ',
            '        ) ',
            '      THEN 1 ELSE 0 ',
            '      END ) / NULLIF(COUNT(*),0), ',
            '      0 ',
            '    ), ',
            '    4 ',
            '  ) AS ratio, ',
            -- ================== ratio 结束 ==================

            '  1000 AS sample_limit, ',
            '  ''pattern: YYYY-MM-DD,YYYY-MM-DD HH:MM:SS,YYYY/MM/DD,YYYYMMDD (纯规则判断)'' AS checked_formats ',
            'FROM `healthdetection_2025`.`', v_table, '` ',
            'WHERE `', v_column, '` IS NOT NULL ',
            '  AND `', v_column, '` <> '''' ',
            'LIMIT 1000'
        );

        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;

    CLOSE cur;
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;

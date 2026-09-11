/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

DROP DATABASE IF EXISTS `training_system`;
CREATE DATABASE IF NOT EXISTS `training_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `training_system`;

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `real_name` varchar(50) NOT NULL,
  `role` tinyint NOT NULL DEFAULT '3',
  `class_id` bigint DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELETE FROM `sys_user`;
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `role`, `class_id`, `email`, `phone`, `status`, `create_time`, `update_time`) VALUES
	(2, 'teacher1', '$2b$10$0klzX690TyRUNHYrkBcHu.qvwaG3i7vbgvcYEFU4FbzkqdEQ7nmIu', '张老师', 2, NULL, 'teacher1@edu.cn', NULL, 1, '2026-04-01 20:11:05', '2026-04-01 20:11:05'),
	(3, 'teacher2', '$2b$10$0klzX690TyRUNHYrkBcHu.qvwaG3i7vbgvcYEFU4FbzkqdEQ7nmIu', '李老师', 2, NULL, 'teacher2@edu.cn', NULL, 1, '2026-04-01 20:11:05', '2026-04-01 20:11:05'),
	(4, 'student1', '$2a$10$7qfKgBS5fUNYK4zg2awY4.iYzbuadCJjuiMl3c9HRd7gHYzFEtz4C', '王小明', 3, 1, 'student1@edu.cn', NULL, 1, '2026-04-01 20:11:05', '2026-04-01 20:11:05'),
	(5, 'student2', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', '李小红', 3, 1, 'student2@edu.cn', NULL, 1, '2026-04-01 20:11:05', '2026-04-01 20:11:05'),
	(6, 'student3', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', '张小华', 3, 2, 'student3@edu.cn', NULL, 1, '2026-04-01 20:11:05', '2026-04-01 20:11:05'),
	(7, 'student4', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', '刘大力', 3, 2, 'student4@edu.cn', NULL, 1, '2026-04-01 20:11:05', '2026-04-01 20:11:05'),
	(8, 'student5', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', '陈静静', 3, 3, 'student5@edu.cn', NULL, 1, '2026-04-01 20:11:05', '2026-04-01 20:11:05'),
	(9, 'admin', '$2a$10$yTF/xxJJpAHtoKObrH0hT.iydsscSp5PTBFa/ycWmEhlbJqIrwvpy', '系统管理员', 1, NULL, NULL, NULL, 1, '2026-04-01 20:51:45', '2026-04-01 21:04:47');

DROP TABLE IF EXISTS `sys_class`;
CREATE TABLE `sys_class` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_name` varchar(100) NOT NULL,
  `teacher_id` bigint DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELETE FROM `sys_class`;
INSERT INTO `sys_class` (`id`, `class_name`, `teacher_id`, `description`, `create_time`) VALUES
	(1, '软件工程2022级1班', 2, '软件工程专业2022级本科1班', '2026-04-01 20:11:05'),
	(2, '软件工程2022级2班', 3, '软件工程专业2022级本科2班', '2026-04-01 20:11:05'),
	(3, '计算机科学与技术2022级1班', NULL, '计算机科学与技术专业2022级本科1班', '2026-04-01 20:11:05');

DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `teacher_id` bigint DEFAULT NULL,
  `class_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELETE FROM `course`;
INSERT INTO `course` (`id`, `course_name`, `description`, `teacher_id`, `class_id`, `create_time`, `update_time`) VALUES
	(1, 'Java Web开发实训', '基于Spring Boot的Web应用开发实训课程', 2, 1, '2026-04-01 20:11:05', '2026-04-01 20:11:05'),
	(2, '数据库系统设计', 'MySQL数据库设计与 optimization实训', 2, 1, '2026-04-01 20:11:05', '2026-04-01 20:11:05'),
	(3, '前端框架应用', 'Vue.js和React框架实战训练', 3, 2, '2026-04-01 20:11:05', '2026-04-01 20:11:05'),
	(4, '移动应用开发', 'Android和iOS移动应用开发实训', 3, 2, '2026-04-01 20:11:05', '2026-04-01 20:11:05');

DROP TABLE IF EXISTS `student_course`;
CREATE TABLE `student_course` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_course` (`student_id`,`course_id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELETE FROM `student_course`;
INSERT INTO `student_course` (`id`, `student_id`, `course_id`, `create_time`) VALUES
	(1, 4, 1, '2026-04-01 20:11:05'),
	(2, 4, 2, '2026-04-01 20:11:05'),
	(3, 5, 1, '2026-04-01 20:11:05'),
	(4, 5, 3, '2026-04-01 20:11:05'),
	(5, 6, 3, '2026-04-01 20:11:05'),
	(6, 6, 4, '2026-04-01 20:11:05'),
	(7, 7, 2, '2026-04-01 20:11:05'),
	(8, 8, 4, '2026-04-01 20:11:05');

DROP TABLE IF EXISTS `training_report`;
CREATE TABLE `training_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `course_id` bigint DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `content` text,
  `file_path` varchar(500) DEFAULT NULL,
  `file_name` varchar(200) DEFAULT NULL,
  `file_type` varchar(20) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELETE FROM `training_report`;
INSERT INTO `training_report` (`id`, `student_id`, `course_id`, `title`, `content`, `file_path`, `file_name`, `file_type`, `status`, `create_time`, `update_time`) VALUES
	(1, 4, 1, '校园食堂订餐系统实训报告', '本报告详细描述了校园食堂订餐系统的设计与实现过程...', NULL, NULL, NULL, 1, '2026-03-27 20:11:05', '2026-04-01 20:11:05'),
	(2, 5, 1, '在线考试系统实训报告', '本报告介绍在线考试系统的需求分析、系统设计和实现...', NULL, NULL, NULL, 1, '2026-03-29 20:11:05', '2026-04-01 20:11:05'),
	(3, 6, 3, '学生信息管理系统实训报告', '本报告阐述了学生信息管理系统的开发过程...', NULL, NULL, NULL, 0, '2026-03-31 20:11:05', '2026-04-01 20:11:05'),
	(4, 7, 2, '图书管理系统实训报告', '本报告介绍了图书管理系统的功能模块设计...', NULL, NULL, NULL, 0, '2026-04-01 20:11:05', '2026-04-01 20:11:05'),
	(5, 3, NULL, '校园食堂订餐系统实训报告', '本报告详细描述了校园食堂订餐系统的设计与实现过程...', NULL, NULL, NULL, 1, '2026-03-27 20:11:05', '2026-04-01 20:11:05'),
	(6, 4, NULL, '在线考试系统实训报告', '本报告介绍在线考试系统的需求分析、系统设计和实现...', NULL, NULL, NULL, 1, '2026-03-29 20:11:05', '2026-04-01 20:11:05'),
	(7, 5, NULL, '学生信息管理系统实训报告', '本报告阐述了学生信息管理系统的开发过程...', NULL, NULL, NULL, 0, '2026-03-31 20:11:05', '2026-04-01 20:11:05'),
	(8, 6, NULL, '图书管理系统实训报告', '本报告介绍了图书管理系统的功能模块设计...', NULL, NULL, NULL, 0, '2026-04-01 20:11:05', '2026-04-01 20:11:05');

-- 【升级：无损融入 dynamic_scores_json 文本大口袋】
DROP TABLE IF EXISTS `evaluation_record`;
CREATE TABLE `evaluation_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_id` bigint NOT NULL,
  `teacher_id` bigint DEFAULT NULL,
  `completeness_score` decimal(5,2) DEFAULT '0.00',
  `specification_score` decimal(5,2) DEFAULT '0.00',
  `knowledge_score` decimal(5,2) DEFAULT '0.00',
  `total_score` decimal(5,2) DEFAULT '0.00',
  `ai_evaluation` text,
  `manual_evaluation` text,
  `dynamic_scores_json` text,
  `is_ai` tinyint DEFAULT '1',
  `evaluate_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_id` (`report_id`),
  KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELETE FROM `evaluation_record`;
INSERT INTO `evaluation_record` (`id`, `report_id`, `teacher_id`, `completeness_score`, `specification_score`, `knowledge_score`, `total_score`, `ai_evaluation`, `manual_evaluation`, `is_ai`, `evaluate_time`, `create_time`, `update_time`) VALUES
	(1, 1, 2, 28.50, 26.00, 35.00, 89.50, '{"completeness":"结构完整","specification":"格式规范","knowledge":"覆盖全面"}', NULL, 1, '2026-03-28 20:11:05', '2026-04-01 20:11:05', '2026-04-01 20:11:05'),
	(2, 2, 2, 27.00, 25.50, 33.00, 85.50, '{"completeness":"结构完整","specification":"格式尚可","knowledge":"覆盖较好"}', NULL, 1, '2026-03-30 20:11:05', '2026-04-01 20:11:05', '2026-04-01 20:11:05');

DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) NOT NULL,
  `config_value` varchar(500) DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELETE FROM `sys_config`;
INSERT INTO `sys_config` (`id`, `config_key`, `config_value`, `description`) VALUES
	(1, 'ai_api_url', 'https://api.deepseek.com/v1/chat/completions', '大模型API地址'),
	(2, 'ai_api_key', '', '大模型API密钥'),
	(3, 'ai_model', 'deepseek-chat', '大模型名称'),
	(4, 'completeness_weight', '30', '完整性权重(%)'),
	(5, 'specification_weight', '30', '规范性权重(%)'),
	(6, 'knowledge_weight', '40', '知识点权重(%)');

DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) DEFAULT NULL,
  `operation` varchar(200) DEFAULT NULL,
  `method` varchar(200) DEFAULT NULL,
  `params` text,
  `ip` varchar(50) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 【升级：无损融入 has_custom_criterion 开关】
DROP TABLE IF EXISTS `report_requirement`;
CREATE TABLE `report_requirement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text,
  `deadline` datetime DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `has_custom_criterion` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `type` tinyint NOT NULL DEFAULT '1',
  `title` varchar(200) NOT NULL,
  `content` text,
  `related_id` bigint DEFAULT NULL,
  `is_read` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 【升级：全新注入第11张核心指标大表】
DROP TABLE IF EXISTS `evaluation_criterion`;
CREATE TABLE `evaluation_criterion` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `requirement_id` bigint NOT NULL,
  `name` varchar(50) NOT NULL,
  `weight` int NOT NULL DEFAULT '0',
  `max_score` decimal(5,2) NOT NULL DEFAULT '100.00',
  `description` text,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_requirement_id` (`requirement_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 导出存储过程
DROP PROCEDURE IF EXISTS `sp_class_statistics`;
DELIMITER //
CREATE PROCEDURE `sp_class_statistics`(IN p_class_id BIGINT)
BEGIN
    SELECT 
        c.class_name,
        COUNT(r.id) as total_reports,
        COUNT(e.id) as evaluated_reports,
        COALESCE(AVG(e.total_score), 0) as avg_score,
        COALESCE(MAX(e.total_score), 0) as max_score,
        COALESCE(MIN(e.total_score), 0) as min_score
    FROM sys_class c
    LEFT JOIN sys_user u ON u.class_id = c.id AND u.role = 3
    LEFT JOIN training_report r ON r.student_id = u.id
    LEFT JOIN evaluation_record e ON e.report_id = r.id
    WHERE c.id = p_class_id
    GROUP BY c.id, c.class_name;
END//
DELIMITER ;

DROP PROCEDURE IF EXISTS `sp_course_statistics`;
DELIMITER //
CREATE PROCEDURE `sp_course_statistics`(IN p_course_id BIGINT)
BEGIN
    SELECT
        co.course_name,
        COUNT(r.id) as total_reports,
        COUNT(e.id) as evaluated_reports,
        COALESCE(AVG(e.total_score), 0) as avg_score,
        COALESCE(MAX(e.total_score), 0) as max_score,
        COALESCE(MIN(e.total_score), 0) as min_score
    FROM course co
    LEFT JOIN training_report r ON r.course_id = co.id
    LEFT JOIN evaluation_record e ON e.report_id = r.id
    WHERE co.id = p_course_id
    GROUP BY co.id, co.course_name;
END//
DELIMITER ;

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
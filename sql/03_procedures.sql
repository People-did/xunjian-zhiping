USE training_system;

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS sp_class_statistics(IN p_class_id BIGINT)
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
END //
DELIMITER ;

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS sp_course_statistics(IN p_course_id BIGINT)
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
END //
DELIMITER ;
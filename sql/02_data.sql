USE training_system;

INSERT INTO sys_user (id, username, password, real_name, role, status) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.ry1U0CWdpfKN2W4XaW', 'SystemAdmin', 1, 1);

INSERT INTO sys_class (id, class_name, description) VALUES
(1, 'Class1_2022', 'Software Engineering Class 1'),
(2, 'Class2_2022', 'Software Engineering Class 2'),
(3, 'Class3_2022', 'Computer Science Class 1');

INSERT INTO sys_user (id, username, password, real_name, role, email, status) VALUES
(2, 'teacher1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.ry1U0CWdpfKN2W4XaW', 'Teacher Zhang', 2, 'teacher1@edu.cn', 1),
(3, 'teacher2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.ry1U0CWdpfKN2W4XaW', 'Teacher Li', 2, 'teacher2@edu.cn', 1);

INSERT INTO sys_user (id, username, password, real_name, role, class_id, email, status) VALUES
(4, 'student1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.ry1U0CWdpfKN2W4XaW', 'Wang Xiaoming', 3, 1, 'student1@edu.cn', 1),
(5, 'student2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.ry1U0CWdpfKN2W4XaW', 'Li Xiaohong', 3, 1, 'student2@edu.cn', 1),
(6, 'student3', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.ry1U0CWdpfKN2W4XaW', 'Zhang Xiaohua', 3, 2, 'student3@edu.cn', 1),
(7, 'student4', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.ry1U0CWdpfKN2W4XaW', 'Liu Dali', 3, 2, 'student4@edu.cn', 1),
(8, 'student5', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.ry1U0CWdpfKN2W4XaW', 'Chen Jingjing', 3, 3, 'student5@edu.cn', 1);

UPDATE sys_class SET teacher_id = 2 WHERE id = 1;
UPDATE sys_class SET teacher_id = 3 WHERE id = 2;

INSERT INTO course (id, course_name, description, teacher_id, class_id) VALUES
(1, 'Java Web Training', 'Spring Boot Web Development', 2, 1),
(2, 'Database System Design', 'MySQL Design and Optimization', 2, 1),
(3, 'Frontend Framework', 'Vue.js and React Training', 3, 2),
(4, 'Mobile Application', 'Android and iOS Development', 3, 2);

INSERT INTO student_course (student_id, course_id) VALUES
(4, 1), (4, 2), (5, 1), (5, 3), (6, 3), (6, 4), (7, 2), (8, 4);

INSERT INTO training_report (id, student_id, course_id, title, content, status, create_time) VALUES
(1, 4, 1, 'Canteen Ordering System Report', 'Details about the design...', 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 5, 1, 'Online Exam System Report', 'Introduction to requirements...', 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 6, 3, 'Student Info Management Report', 'Development description...', 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, 7, 2, 'Book Management System Report', 'Functional module design...', 0, NOW());

INSERT INTO evaluation_record (report_id, teacher_id, completeness_score, specification_score, knowledge_score, total_score, ai_evaluation, is_ai, evaluate_time) VALUES
(1, 2, 28.50, 26.00, 35.00, 89.50, '{"completeness":{"score":28.50},"specification":{"score":26.00},"knowledge":{"score":35.00}}', 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2, 2, 27.00, 25.50, 33.00, 85.50, '{"completeness":{"score":27.00},"specification":{"score":25.50},"knowledge":{"score":33.00}}', 1, DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO sys_config (config_key, config_value, description) VALUES
('ai_api_url', 'https://api.deepseek.com/v1/chat/completions', 'AI API URL'),
('ai_api_key', '', 'AI API KEY'),
('ai_model', 'deepseek-chat', 'AI Model Name'),
('completeness_weight', '30', 'Completeness Weight'),
('specification_weight', '30', 'Specification Weight'),
('knowledge_weight', '40', 'Knowledge Weight');
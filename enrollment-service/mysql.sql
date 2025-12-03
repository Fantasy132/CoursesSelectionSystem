CREATE DATABASE IF NOT EXISTS enrollment_db;
CREATE USER IF NOT EXISTS 'enrollment_user'@'localhost' IDENTIFIED BY 'enrollment_pass';
GRANT ALL PRIVILEGES ON enrollment_db.* TO 'enrollment_user'@'localhost';
FLUSH PRIVILEGES;

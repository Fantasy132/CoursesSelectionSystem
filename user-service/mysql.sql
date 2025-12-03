CREATE DATABASE IF NOT EXISTS user_db;
CREATE USER IF NOT EXISTS 'user_user'@'localhost' IDENTIFIED BY 'user_pass';
GRANT ALL PRIVILEGES ON user_db.* TO 'user_user'@'localhost';
FLUSH PRIVILEGES;

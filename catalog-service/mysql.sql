CREATE DATABASE IF NOT EXISTS catalog_db;
CREATE USER IF NOT EXISTS 'catalog_user'@'localhost' IDENTIFIED BY 'catalog_pass';
GRANT ALL PRIVILEGES ON catalog_db.* TO 'catalog_user'@'localhost';
FLUSH PRIVILEGES;
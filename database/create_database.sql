-- Создание базы данных для Агрегатора новостей
-- MySQL Database Creation Script

-- Создание базы данных
CREATE DATABASE IF NOT EXISTS news_aggregator 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Использование созданной базы данных
USE news_aggregator;

-- Создание пользователя для приложения (опционально)
-- CREATE USER IF NOT EXISTS 'news_app'@'localhost' IDENTIFIED BY 'news_password';
-- GRANT ALL PRIVILEGES ON news_aggregator.* TO 'news_app'@'localhost';
-- FLUSH PRIVILEGES;

-- Таблица ролей
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(100)
);

-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_enabled (enabled)
);

-- Связующая таблица пользователи-роли (многие ко многим)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Таблица категорий новостей
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    color_code VARCHAR(7) DEFAULT '#007bff',
    
    INDEX idx_name (name)
);

-- Таблица источников новостей
CREATE TABLE IF NOT EXISTS news_sources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    rss_url VARCHAR(1000) NOT NULL UNIQUE,
    website_url VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_updated TIMESTAMP NULL,
    last_error TEXT,
    error_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_name (name),
    INDEX idx_active (active),
    INDEX idx_rss_url (rss_url(255))
);

-- Таблица статей
CREATE TABLE IF NOT EXISTS articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT,
    summary TEXT,
    source_url VARCHAR(1000) NOT NULL UNIQUE,
    image_url VARCHAR(1000),
    published_at TIMESTAMP NOT NULL,
    status ENUM('PENDING', 'PUBLISHED', 'REJECTED', 'DRAFT') NOT NULL DEFAULT 'PENDING',
    source_id BIGINT,
    category_id BIGINT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (source_id) REFERENCES news_sources(id) ON DELETE SET NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_title (title(255)),
    INDEX idx_status (status),
    INDEX idx_published_at (published_at),
    INDEX idx_source_url (source_url(255)),
    INDEX idx_source_id (source_id),
    INDEX idx_category_id (category_id),
    INDEX idx_created_by (created_by),
    
    FULLTEXT INDEX ft_title_content (title, content)
);

-- Таблица пользовательских предпочтений
CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    subscribed_categories TEXT,
    preferred_sources TEXT,
    email_notifications BOOLEAN DEFAULT FALSE,
    articles_per_page INT DEFAULT 10,
    theme VARCHAR(20) DEFAULT 'light',
    language VARCHAR(5) DEFAULT 'ru',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Таблица сохраненных статей (многие ко многим)
CREATE TABLE IF NOT EXISTS saved_articles (
    user_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, article_id),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    
    INDEX idx_saved_at (saved_at)
);

-- Вставка базовых ролей
INSERT IGNORE INTO roles (name, description) VALUES 
('ADMIN', 'Администратор системы'),
('EDITOR', 'Редактор контента'),
('READER', 'Читатель');

-- Вставка базовых категорий
INSERT IGNORE INTO categories (name, description, color_code) VALUES 
('Технологии', 'Новости из мира технологий и IT', '#007bff'),
('Политика', 'Политические новости и события', '#dc3545'),
('Экономика', 'Экономические новости и аналитика', '#28a745'),
('Спорт', 'Спортивные новости и результаты', '#fd7e14'),
('Наука', 'Научные открытия и исследования', '#6f42c1'),
('Культура', 'Культурные события и искусство', '#e83e8c'),
('Здоровье', 'Новости медицины и здравоохранения', '#20c997'),
('Образование', 'Новости образования и науки', '#6c757d'),
('Общество', 'Общественные события и социальные вопросы', '#ffc107'),
('Мир', 'Международные новости', '#17a2b8');

-- Вставка базовых источников новостей
INSERT IGNORE INTO news_sources (name, rss_url, website_url) VALUES 
('Хабр', 'https://habr.com/ru/rss/hub/programming/', 'https://habr.com'),
('TechCrunch', 'https://techcrunch.com/feed/', 'https://techcrunch.com'),
('BBC News', 'http://feeds.bbci.co.uk/news/rss.xml', 'https://www.bbc.com/news'),
('Reuters', 'https://www.reutersagency.com/feed/?best-topics=tech', 'https://www.reuters.com'),
('Ведомости', 'https://www.vedomosti.ru/rss/news', 'https://www.vedomosti.ru');

-- Создание тестовых пользователей (пароли будут зашифрованы приложением)
-- Эти пользователи будут созданы через DataInitializer в приложении

COMMIT;
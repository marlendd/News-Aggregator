-- Создание таблицы для сохраненных статей
-- Дата: 11 января 2026

CREATE TABLE saved_articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Внешние ключи
    CONSTRAINT fk_saved_articles_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_articles_article 
        FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    
    -- Уникальный индекс для предотвращения дублирования
    UNIQUE KEY unique_user_article (user_id, article_id),
    
    -- Индексы для производительности
    INDEX idx_saved_articles_user_id (user_id),
    INDEX idx_saved_articles_article_id (article_id),
    INDEX idx_saved_articles_saved_at (saved_at)
);

-- Комментарии к таблице и колонкам
ALTER TABLE saved_articles COMMENT = 'Таблица для хранения сохраненных пользователями статей';
ALTER TABLE saved_articles MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT 'Уникальный идентификатор записи';
ALTER TABLE saved_articles MODIFY COLUMN user_id BIGINT NOT NULL COMMENT 'ID пользователя, сохранившего статью';
ALTER TABLE saved_articles MODIFY COLUMN article_id BIGINT NOT NULL COMMENT 'ID сохраненной статьи';
ALTER TABLE saved_articles MODIFY COLUMN saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Время сохранения статьи';
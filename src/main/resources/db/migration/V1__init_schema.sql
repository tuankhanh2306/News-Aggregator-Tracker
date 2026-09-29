-- ========================================================
-- V1__init_schema.sql: Initial Schema for Source, Category, Article
-- ========================================================

CREATE TABLE source (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rss_url VARCHAR(500) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_fetched_at TIMESTAMP
);

CREATE TABLE category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE article (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES source(id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES category(id) ON DELETE SET NULL,
    title VARCHAR(500) NOT NULL,
    summary TEXT,
    url VARCHAR(1000) NOT NULL,
    url_hash CHAR(64) NOT NULL UNIQUE,
    image_url VARCHAR(1000),
    published_at TIMESTAMP,
    fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_article_source_id ON article(source_id);
CREATE INDEX idx_article_category_id ON article(category_id);
CREATE INDEX idx_article_published_at ON article(published_at DESC);
CREATE INDEX idx_article_url_hash ON article(url_hash);

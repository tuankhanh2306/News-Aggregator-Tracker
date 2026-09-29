-- ========================================================
-- V3__create_keyword_stat.sql: Keyword Statistics for Trending
-- ========================================================

CREATE TABLE keyword_stat (
    id BIGSERIAL PRIMARY KEY,
    keyword VARCHAR(100) NOT NULL,
    window_start TIMESTAMP NOT NULL,
    count INT NOT NULL
);

CREATE INDEX idx_keyword_stat_keyword_window ON keyword_stat (keyword, window_start);
CREATE INDEX idx_keyword_stat_window_start ON keyword_stat (window_start DESC);

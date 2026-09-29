-- ========================================================
-- V2__add_full_text_search.sql: Full-Text Search with TSVECTOR & GIN Index
-- ========================================================

-- 1. Add search_vector column to article
ALTER TABLE article ADD COLUMN search_vector tsvector;

-- 2. Create trigger function to automatically compute search_vector
-- Weight 'A' for title, Weight 'B' for summary
CREATE OR REPLACE FUNCTION article_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', coalesce(NEW.title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(NEW.summary, '')), 'B');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- 3. Create trigger on article table
CREATE TRIGGER trg_article_search_vector_update
BEFORE INSERT OR UPDATE OF title, summary ON article
FOR EACH ROW
EXECUTE FUNCTION article_search_vector_update();

-- 4. Populate search_vector for existing articles
UPDATE article SET search_vector =
    setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(summary, '')), 'B');

-- 5. Create GIN index on search_vector
CREATE INDEX idx_article_search_vector ON article USING gin(search_vector);

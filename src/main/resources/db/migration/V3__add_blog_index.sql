CREATE INDEX idx_blog_posts_archive
      ON blog_posts (published, created_at DESC, id DESC);

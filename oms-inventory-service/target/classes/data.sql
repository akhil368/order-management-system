-- Idempotent seed: INSERT IGNORE means a restart does NOT reset stock,
-- so reservations made via Kafka events persist across restarts.
INSERT IGNORE INTO inventory (product_id, quantity) VALUES
  ('PROD-1', 100),
  ('PROD-2', 50),
  ('PROD-3', 10);

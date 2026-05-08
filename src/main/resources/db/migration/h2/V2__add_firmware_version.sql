ALTER TABLE led_strip_clients ADD COLUMN firmware_version VARCHAR(50) NOT NULL DEFAULT '--';
UPDATE led_strip_clients SET firmware_version = '0.1' WHERE client_type = 'Pi';
ALTER TABLE led_strip_clients ADD COLUMN fps INT NOT NULL DEFAULT 35;
ALTER TABLE led_strip_clients ADD COLUMN fade_timeout_millis INT NOT NULL DEFAULT 15000;

ALTER TABLE led_strip_clients ADD COLUMN firmware_version VARCHAR(50) NOT NULL DEFAULT '--';
UPDATE led_strip_clients SET firmware_version = '0.1' WHERE client_type = 'Pi';

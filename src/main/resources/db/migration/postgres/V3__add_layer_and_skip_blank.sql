ALTER TABLE light_effect_settings ADD COLUMN skip_frames_if_blank BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE light_effects ADD COLUMN layer INT NOT NULL DEFAULT 0;
ALTER TABLE light_effects ADD CONSTRAINT light_effects_strip_layer_uq UNIQUE (strip_id, layer);
ALTER TABLE light_effects ADD CONSTRAINT light_effects_pool_layer_uq UNIQUE (pool_id, layer);

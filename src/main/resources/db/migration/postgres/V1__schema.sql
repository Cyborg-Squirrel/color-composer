--Force Flyway migration
--DELETE FROM flyway_schema_history;
DROP TABLE IF EXISTS light_effect_triggers;
DROP TABLE IF EXISTS light_effect_filter_junctions;
DROP TABLE IF EXISTS light_effect_filters;
DROP TABLE IF EXISTS light_effects;
DROP TABLE IF EXISTS light_effect_palettes;
DROP TABLE IF EXISTS pool_member_led_strips;
DROP TABLE IF EXISTS led_strip_pools;
DROP TABLE IF EXISTS led_strips;
DROP TABLE IF EXISTS led_strip_clients;
DROP TABLE IF EXISTS sunrise_sunset_times;
DROP TABLE IF EXISTS location_configs;

CREATE TABLE led_strip_clients
(
    id           SERIAL primary key NOT NULL,
    name         VARCHAR(255) NOT NULL,
    uuid         VARCHAR(50) NOT NULL UNIQUE,
    address      VARCHAR(255) NOT NULL,
    client_type  VARCHAR(50) NOT NULL,
    color_order  VARCHAR(4) NOT NULL,
    ws_port      INT NOT NULL,
    api_port     INT NOT NULL,
    last_seen_at BIGINT NOT NULL,
    power_limit  INT
);

CREATE TABLE led_strips
(
    id          SERIAL primary key NOT NULL,
    name        VARCHAR(255) NOT NULL,
    uuid        VARCHAR(50) NOT NULL UNIQUE,
    pin         VARCHAR(50) NOT NULL,
    length      INT NOT NULL,
    height      INT NOT NULL,
    blend_mode  VARCHAR(50) NOT NULL,
    brightness  INT NOT NULL,
    client_id   INT NOT NULL,
    FOREIGN KEY (client_id) REFERENCES led_strip_clients
);

CREATE TABLE led_strip_pools
(
    id         SERIAL primary key NOT NULL,
    name       VARCHAR(255) NOT NULL,
    uuid       VARCHAR(50) NOT NULL UNIQUE,
    pool_type  VARCHAR(50) NOT NULL,
    blend_mode VARCHAR(50) NOT NULL
);

CREATE TABLE pool_member_led_strips
(
    id           SERIAL primary key NOT NULL,
    inverted     BOOLEAN NOT NULL,
    pool_index   SMALLINT NOT NULL,
    uuid       VARCHAR(50) NOT NULL UNIQUE,
    strip_id     INT NOT NULL,
    pool_id      INT NOT NULL,
    FOREIGN KEY (strip_id) REFERENCES led_strips,
    FOREIGN KEY (pool_id) REFERENCES led_strip_pools
);

CREATE TABLE light_effect_palettes
(
    id          SERIAL PRIMARY KEY,
    uuid        VARCHAR(50) NOT NULL UNIQUE,
    settings    JSONB NOT NULL,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(255) NOT NULL
);

CREATE TABLE light_effects
(
    id         SERIAL PRIMARY KEY,
    strip_id   INT,
    pool_id    INT,
    palette_id INT,
    uuid       VARCHAR(50) NOT NULL UNIQUE,
    settings   JSONB NOT NULL,
    type       VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    status     VARCHAR(50) NOT NULL,
    FOREIGN KEY (pool_id) REFERENCES led_strip_pools,
    FOREIGN KEY (strip_id) REFERENCES led_strips,
    FOREIGN KEY (palette_id) REFERENCES light_effect_palettes
);

CREATE TABLE light_effect_triggers
(
    id          SERIAL PRIMARY KEY,
    effect_id   INT NOT NULL,
    uuid        VARCHAR(50) NOT NULL UNIQUE,
    settings    JSONB NOT NULL,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(255) NOT NULL,
    FOREIGN KEY (effect_id) REFERENCES light_effects
);

CREATE TABLE light_effect_filters
(
    id                 SERIAL PRIMARY KEY,
    uuid               VARCHAR(50) NOT NULL UNIQUE,
    settings           JSONB NOT NULL,
    name               VARCHAR(255) NOT NULL,
    type               VARCHAR(255) NOT NULL
);

CREATE TABLE light_effect_filter_junctions
(
    id        SERIAL PRIMARY KEY,
    filter_id INT NOT NULL,
    effect_id INT,
    FOREIGN KEY (filter_id) REFERENCES light_effect_filters(id),
    FOREIGN KEY (effect_id) REFERENCES light_effects(id)
);

CREATE TABLE location_configs
(
    id     SERIAL primary key NOT NULL,
    lat    VARCHAR(255) NOT NULL,
    lng    VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE sunrise_sunset_times
(
    id            SERIAL primary key NOT NULL,
    ymd           VARCHAR(10) NOT NULL,
    json          VARCHAR(500) NOT NULL,
    location_id   INT NOT NULL,
    FOREIGN KEY (location_id) REFERENCES location_configs
);

INSERT INTO location_configs VALUES(1, '44.5855', '-93.160900', TRUE)

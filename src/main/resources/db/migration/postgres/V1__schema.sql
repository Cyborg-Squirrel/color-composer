--Force Flyway migration
--DELETE FROM flyway_schema_history;
DROP TABLE IF EXISTS light_effect_triggers;
DROP TABLE IF EXISTS light_effect_palettes;
DROP TABLE IF EXISTS light_effect_palette_junctions;
DROP TABLE IF EXISTS light_effect_filter_junctions;
DROP TABLE IF EXISTS light_effect_filters;
DROP TABLE IF EXISTS light_effects;
DROP TABLE IF EXISTS group_member_led_strips;
DROP TABLE IF EXISTS led_strip_groups;
DROP TABLE IF EXISTS led_strips;
DROP TABLE IF EXISTS led_strip_clients;
DROP TABLE IF EXISTS sunrise_sunset_times;
DROP TABLE IF EXISTS location_configs;

CREATE TABLE led_strip_clients
(
    id       SERIAL primary key NOT NULL,
    name     VARCHAR(255) NOT NULL,
    uuid     VARCHAR(50) NOT NULL UNIQUE,
    address  VARCHAR(255) NOT NULL,
    ws_port  SMALLINT NOT NULL,
    api_port SMALLINT NOT NULL
);

CREATE TABLE led_strips
(
    id          SERIAL primary key NOT NULL,
    name        VARCHAR(255) NOT NULL,
    uuid        VARCHAR(50) NOT NULL UNIQUE,
    length      INT NOT NULL,
    height      INT NOT NULL,
    power_limit INT,
    blend_mode  VARCHAR(50) NOT NULL,
    client_id   INT NOT NULL,
    FOREIGN KEY (client_id) REFERENCES led_strip_clients
);

CREATE TABLE led_strip_groups
(
    id    SERIAL primary key NOT NULL,
    name  VARCHAR(255) NOT NULL,
    uuid  VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE group_member_led_strips
(
    id           SERIAL primary key NOT NULL,
    inverted     BOOLEAN NOT NULL,
    group_index  SMALLINT NOT NULL,
    strip_id     INT NOT NULL,
    group_id     INT NOT NULL,
    FOREIGN KEY (strip_id) REFERENCES led_strips,
    FOREIGN KEY (group_id) REFERENCES led_strip_groups
);

CREATE TABLE light_effects
(
    id         SERIAL PRIMARY KEY,
    strip_id   INT,
    group_id   INT,
    uuid       VARCHAR(50) NOT NULL UNIQUE,
    settings   JSONB NOT NULL,
    type       VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    status     VARCHAR(50) NOT NULL,
    FOREIGN KEY (group_id) REFERENCES led_strip_groups,
    FOREIGN KEY (strip_id) REFERENCES led_strips
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

CREATE TABLE light_effect_palettes
(
    id          SERIAL PRIMARY KEY,
    uuid        VARCHAR(50) NOT NULL UNIQUE,
    settings    JSONB NOT NULL,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(255) NOT NULL
);

CREATE TABLE light_effect_palette_junctions
(
    id         SERIAL PRIMARY KEY,
    palette_id INT NOT NULL,
    effect_id  INT,
    FOREIGN KEY (palette_id) REFERENCES light_effect_palettes(id),
    FOREIGN KEY (effect_id) REFERENCES light_effects(id)
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

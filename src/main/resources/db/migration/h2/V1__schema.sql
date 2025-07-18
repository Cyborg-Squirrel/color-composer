DROP TABLE IF EXISTS light_effect_triggers;
DROP TABLE IF EXISTS light_effect_filter_junctions;
DROP TABLE IF EXISTS light_effect_filters;
DROP TABLE IF EXISTS light_effects;
DROP TABLE IF EXISTS light_effect_palettes;
DROP TABLE IF EXISTS group_member_led_strips;
DROP TABLE IF EXISTS led_strip_groups;
DROP TABLE IF EXISTS led_strips;
DROP TABLE IF EXISTS led_strip_clients;
DROP TABLE IF EXISTS sunrise_sunset_times;
DROP TABLE IF EXISTS location_configs;

CREATE TABLE led_strip_clients
(
    id          IDENTITY PRIMARY KEY NOT NULL,
    name        VARCHAR(255) NOT NULL,
    uuid        VARCHAR(50) NOT NULL UNIQUE,
    address     VARCHAR(255) NOT NULL,
    client_type VARCHAR(50) NOT NULL,
    color_order VARCHAR(4) NOT NULL,
    ws_port     INT NOT NULL,
    api_port    INT NOT NULL
);

CREATE TABLE led_strips
(
    id          IDENTITY PRIMARY KEY NOT NULL,
    name        VARCHAR(255) NOT NULL,
    uuid        VARCHAR(50) NOT NULL UNIQUE,
    pin         VARCHAR(50) NOT NULL,
    length      INT NOT NULL,
    height      INT NOT NULL,
    power_limit INT,
    blend_mode  VARCHAR(50) NOT NULL,
    brightness  INT NOT NULL,
    client_id   INT NOT NULL,
    FOREIGN KEY (client_id) REFERENCES led_strip_clients
);

CREATE TABLE led_strip_groups
(
    id    IDENTITY PRIMARY KEY NOT NULL,
    name  VARCHAR(255) NOT NULL,
    uuid  VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE group_member_led_strips
(
    id           IDENTITY PRIMARY KEY NOT NULL,
    inverted     BOOLEAN NOT NULL,
    group_index  SMALLINT NOT NULL,
    strip_id     INT NOT NULL,
    group_id     INT NOT NULL,
    FOREIGN KEY (strip_id) REFERENCES led_strips,
    FOREIGN KEY (group_id) REFERENCES led_strip_groups
);

CREATE TABLE light_effect_palettes
(
    id          IDENTITY PRIMARY KEY NOT NULL,
    uuid        VARCHAR(50) NOT NULL UNIQUE,
    settings    JSON NOT NULL,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(255) NOT NULL
);

CREATE TABLE light_effects
(
    id         IDENTITY PRIMARY KEY NOT NULL,
    strip_id   INT,
    group_id   INT,
    palette_id INT,
    uuid       VARCHAR(50) NOT NULL UNIQUE,
    settings   JSON NOT NULL,
    type       VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    status     VARCHAR(50) NOT NULL,
    FOREIGN KEY (group_id) REFERENCES led_strip_groups,
    FOREIGN KEY (strip_id) REFERENCES led_strips,
    FOREIGN KEY (palette_id) REFERENCES light_effect_palettes
);

CREATE TABLE light_effect_triggers
(
    id          IDENTITY PRIMARY KEY NOT NULL,
    effect_id   INT NOT NULL,
    uuid        VARCHAR(50) NOT NULL UNIQUE,
    settings    JSON NOT NULL,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(255) NOT NULL,
    FOREIGN KEY (effect_id) REFERENCES light_effects
);

CREATE TABLE light_effect_filters
(
    id                 IDENTITY PRIMARY KEY NOT NULL,
    uuid               VARCHAR(50) NOT NULL UNIQUE,
    settings           JSON NOT NULL,
    name               VARCHAR(255) NOT NULL,
    type               VARCHAR(255) NOT NULL
);

CREATE TABLE light_effect_filter_junctions
(
    id        IDENTITY PRIMARY KEY NOT NULL,
    filter_id INT NOT NULL,
    effect_id INT,
    FOREIGN KEY (filter_id) REFERENCES light_effect_filters(id),
    FOREIGN KEY (effect_id) REFERENCES light_effects(id)
);

CREATE TABLE location_configs
(
    id     IDENTITY PRIMARY KEY NOT NULL,
    lat    VARCHAR(255) NOT NULL,
    lng    VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE sunrise_sunset_times
(
    id            IDENTITY PRIMARY KEY NOT NULL,
    ymd           VARCHAR(10) NOT NULL,
    json          VARCHAR(500) NOT NULL,
    location_id   INT,
    FOREIGN KEY (location_id) REFERENCES location_configs
);

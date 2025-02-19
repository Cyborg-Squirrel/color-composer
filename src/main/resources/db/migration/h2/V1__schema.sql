DROP TABLE IF EXISTS light_effect_triggers;
DROP TABLE IF EXISTS light_effects;
DROP TABLE IF EXISTS group_member_led_strips;
DROP TABLE IF EXISTS led_strip_groups;
DROP TABLE IF EXISTS led_strips;
DROP TABLE IF EXISTS led_strip_clients;
DROP TABLE IF EXISTS sunrise_sunset_times;
DROP TABLE IF EXISTS location_configs;

CREATE TABLE led_strip_clients
(
    id       IDENTITY PRIMARY KEY NOT NULL,
    name     VARCHAR(255) NOT NULL,
    address  VARCHAR(255) NOT NULL,
    ws_port  SMALLINT NOT NULL,
    api_port SMALLINT NOT NULL
);

CREATE TABLE led_strips
(
    id          IDENTITY PRIMARY KEY NOT NULL,
    name        VARCHAR(255) NOT NULL,
    uuid        VARCHAR(255) NOT NULL,
    length      INT NOT NULL,
    height      INT NOT NULL,
    power_limit INT,
    client_id   INT,
    CONSTRAINT strip_client_fk FOREIGN KEY (client_id) REFERENCES led_strip_clients
);

CREATE TABLE led_strip_groups
(
    id    IDENTITY PRIMARY KEY NOT NULL,
    name  VARCHAR(255) NOT NULL,
    uuid  VARCHAR(255) NOT NULL
);

CREATE TABLE group_member_led_strips
(
    id           IDENTITY PRIMARY KEY NOT NULL,
    inverted     BOOLEAN NOT NULL,
    group_index  SMALLINT NOT NULL,
    strip_id     INT NOT NULL,
    group_id     INT NOT NULL,
    CONSTRAINT led_strip_fk FOREIGN KEY (strip_id) REFERENCES led_strips,
    CONSTRAINT led_strip_group_fk FOREIGN KEY (group_id) REFERENCES led_strip_groups
);

CREATE TABLE light_effects
(
    id         IDENTITY PRIMARY KEY NOT NULL,
    strip_id   INT,
    group_id   INT,
    uuid       VARCHAR(255) NOT NULL,
    settings   JSON NOT NULL,
    name       VARCHAR(255) NOT NULL,
    status     VARCHAR(50) NOT NULL,
    CONSTRAINT led_strip_group_assoc_fk FOREIGN KEY (group_id) REFERENCES led_strip_groups,
    CONSTRAINT led_strip_assoc_fk FOREIGN KEY (strip_id) REFERENCES led_strips,
    CONSTRAINT strip_or_strip_group_not_null CHECK (
        (strip_id IS NOT NULL AND group_id IS NULL) OR
                (strip_id IS NULL AND group_id IS NOT NULL)
    )
);

CREATE TABLE light_effect_triggers
(
    id          IDENTITY PRIMARY KEY NOT NULL,
    effect_id   INT NOT NULL,
    uuid        VARCHAR(255) NOT NULL,
    settings    JSON NOT NULL,
    name        VARCHAR(255) NOT NULL,
    CONSTRAINT trigger_effect_fk FOREIGN KEY (effect_id) REFERENCES light_effects
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
    CONSTRAINT location_fk FOREIGN KEY (location_id) REFERENCES location_configs
);

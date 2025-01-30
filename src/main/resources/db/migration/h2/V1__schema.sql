DROP TABLE IF EXISTS group_member_led_strips;
DROP TABLE IF EXISTS led_strip_groups;
DROP TABLE IF EXISTS led_strips;
DROP TABLE IF EXISTS led_strip_clients;
DROP TABLE IF EXISTS sunrise_sunset_times;
DROP TABLE IF EXISTS location_configs;

CREATE TABLE led_strip_clients
(
    id       int auto_increment PRIMARY KEY NOT NULL,
    name     varchar(255) NOT NULL,
    address  varchar(255) NOT NULL,
    ws_port  smallint NOT NULL,
    api_port smallint NOT NULL
);

CREATE TABLE led_strips
(
    id         int auto_increment PRIMARY KEY NOT NULL,
    name       varchar(255) NOT NULL,
    uuid       varchar(255) NOT NULL,
    length     int NOT NULL,
    height     int NOT NULL,
    client_id  int,
    CONSTRAINT strip_client_fk FOREIGN KEY (client_id) REFERENCES led_strip_clients
);

CREATE TABLE led_strip_groups
(
    id    int auto_increment PRIMARY KEY NOT NULL,
    name  varchar(255) NOT NULL,
    uuid  varchar(255) NOT NULL
);

CREATE TABLE group_member_led_strips
(
    id                 int auto_increment PRIMARY KEY NOT NULL,
    inverted           boolean NOT NULL,
    group_index        smallint NOT NULL,
    uuid               varchar(255) NOT NULL,
    led_strip_id       int NOT NULL,
    led_strip_group_id int NOT NULL,
    CONSTRAINT led_strip_fk FOREIGN KEY (led_strip_id) REFERENCES led_strips,
    CONSTRAINT led_strip_group_fk FOREIGN KEY (led_strip_group_id) REFERENCES led_strip_groups
);

CREATE TABLE location_configs
(
    id     int auto_increment PRIMARY KEY NOT NULL,
    lat    varchar(255) NOT NULL,
    lng    varchar(255) NOT NULL,
    active boolean NOT NULL
);

CREATE TABLE sunrise_sunset_times
(
    id            int auto_increment PRIMARY KEY NOT NULL,
    ymd           varchar(10) NOT NULL,
    json          varchar(500) NOT NULL,
    location_id   int,
    CONSTRAINT location_fk FOREIGN KEY (location_id) REFERENCES location_configs
);

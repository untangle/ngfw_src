-- convert script for release 1.5

-- create HttpSettings.

CREATE TABLE tr_http_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    enabled bool NOT NULL,
    PRIMARY KEY (settings_id));

INSERT INTO tr_http_settings
    (SELECT nextval('hibernate_sequence'), tid, TRUE
     FROM transform_persistent_state WHERE name = 'http-casing');

-- add ACTION column to TR_HTTPBLK_EVT_BLK

ALTER TABLE tr_httpblk_evt_blk ADD COLUMN action char(1);
UPDATE tr_httpblk_evt_blk SET action = 'B';

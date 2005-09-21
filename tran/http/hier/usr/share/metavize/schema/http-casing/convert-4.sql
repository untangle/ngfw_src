-- convert script for release 3.0

ALTER TABLE tr_http_settings
    DROP CONSTRAINT fk_tr_http_settings;

ALTER TABLE tr_http_settings
    DROP COLUMN tid;
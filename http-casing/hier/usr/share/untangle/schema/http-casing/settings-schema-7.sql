-- settings schema for release-4.2

-----------
-- tables |
-----------

-- com.untangle.tran.http.HttpSettings
CREATE TABLE settings.tr_http_settings (
    settings_id int8 NOT NULL,
    enabled bool NOT NULL,
    non_http_blocked bool NOT NULL,
    max_header_length int4 NOT NULL,
    block_long_headers bool NOT NULL,
    max_uri_length int4 NOT NULL,
    block_long_uris bool NOT NULL,
    PRIMARY KEY (settings_id));

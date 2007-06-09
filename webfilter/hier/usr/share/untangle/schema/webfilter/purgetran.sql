CREATE TEMPORARY TABLE dead_string_rules (rule_id int8);

INSERT INTO dead_string_rules
    (SELECT rule_id FROM tr_httpblk_passed_urls
     WHERE setting_id
         IN (SELECT setting_id FROM tr_httpblk_settings));

INSERT INTO dead_string_rules
    (SELECT rule_id FROM tr_httpblk_extensions
     WHERE setting_id
         IN (SELECT setting_id FROM tr_httpblk_settings));

INSERT INTO dead_string_rules
    (SELECT rule_id FROM tr_httpblk_blocked_urls
     WHERE setting_id
         IN (SELECT setting_id FROM tr_httpblk_settings));

DELETE FROM string_rule WHERE rule_id IN (SELECT * FROM dead_string_rules);

CREATE TEMPORARY TABLE dead_mime_rules
    AS SELECT rule_id FROM tr_httpblk_mime_types
       WHERE setting_id
           IN (SELECT setting_id FROM tr_httpblk_settings);

DELETE FROM mimetype_rule
    WHERE rule_id IN (SELECT * FROM dead_mime_rules);

CREATE TEMPORARY TABLE dead_ipmaddr_rules
    AS SELECT rule_id FROM tr_httpblk_passed_clients
       WHERE setting_id
           IN (SELECT setting_id FROM tr_httpblk_settings);

DELETE FROM ipmaddr_rule
    WHERE rule_id IN (SELECT * FROM dead_ipmaddr_rules);

DROP TABLE settings.tr_httpblk_passed_urls;
DROP TABLE settings.tr_httpblk_extensions;
DROP TABLE settings.tr_httpblk_mime_types;
DROP TABLE settings.tr_httpblk_passed_clients;
DROP TABLE settings.tr_httpblk_blocked_urls;
DROP TABLE settings.tr_httpblk_blcat;
DROP TABLE settings.tr_httpblk_settings;
DROP TABLE settings.tr_httpblk_template;
DROP TABLE events.tr_httpblk_evt_blk;

----------------
-- constraints |
----------------


ALTER TABLE settings.tr_httpblk_blcat
    ADD CONSTRAINT fk_tr_httpblk_blcat
    FOREIGN KEY (setting_id) REFERENCES settings.tr_httpblk_settings;

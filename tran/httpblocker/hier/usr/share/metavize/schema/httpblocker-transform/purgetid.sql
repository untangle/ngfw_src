CREATE TEMPORARY TABLE dead_string_rules (rule_id int8);

INSERT INTO dead_string_rules
    (SELECT rule_id FROM tr_httpblk_passed_urls
     WHERE setting_id
         IN (SELECT setting_id FROM tr_httpblk_settings WHERE tid = :tid));

INSERT INTO dead_string_rules
    (SELECT rule_id FROM tr_httpblk_extensions
     WHERE setting_id
         IN (SELECT setting_id FROM tr_httpblk_settings WHERE tid = :tid));

INSERT INTO dead_string_rules
    (SELECT rule_id FROM tr_httpblk_blocked_urls
     WHERE setting_id
         IN (SELECT setting_id FROM tr_httpblk_settings WHERE tid = :tid));

DELETE FROM tr_httpblk_passed_urls
    WHERE rule_id IN (SELECT * FROM dead_string_rules);
DELETE FROM tr_httpblk_extensions
    WHERE rule_id IN (SELECT * FROM dead_string_rules);
DELETE FROM tr_httpblk_blocked_urls
    WHERE rule_id IN (SELECT * FROM dead_string_rules);
DELETE FROM string_rule WHERE rule_id IN (SELECT * FROM dead_string_rules);

CREATE TEMPORARY TABLE dead_mime_rules
    AS SELECT rule_id FROM tr_httpblk_mime_types
       WHERE setting_id
           IN (SELECT setting_id FROM tr_httpblk_settings WHERE tid = :tid);

DELETE FROM tr_httpblk_mime_types
    WHERE rule_id IN (SELECT * FROM dead_mime_rules);
DELETE FROM mimetype_rule
    WHERE rule_id IN (SELECT * FROM dead_mime_rules);

CREATE TEMPORARY TABLE dead_ipmaddr_rules
    AS SELECT rule_id FROM tr_httpblk_passed_clients
       WHERE setting_id
           IN (SELECT setting_id FROM tr_httpblk_settings WHERE tid = :tid);

DELETE FROM tr_httpblk_passed_clients
    WHERE rule_id IN (SELECT * FROM dead_ipmaddr_rules);
DELETE FROM ipmaddr_rule
    WHERE rule_id IN (SELECT * FROM dead_ipmaddr_rules);

DELETE FROM tr_httpblk_blcat
    WHERE setting_id
        IN (SELECT setting_id FROM tr_httpblk_settings WHERE tid = :tid);

DELETE FROM settings.tr_httpblk_settings WHERE tid = :tid;

DELETE FROM settings.tr_httpblk_template
    WHERE message_id NOT IN (SELECT template FROM tr_httpblk_settings);


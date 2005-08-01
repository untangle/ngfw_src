CREATE TEMPORARY TABLE rule_ids
    AS (SELECT rule_id
        FROM settings.tr_firewall_rules
        JOIN settings.tr_firewall_settings ON setting_id = settings_id
        WHERE tid = :tid);

DELETE FROM tr_firewall_rules WHERE rule_id IN (SELECT * FROM rule_ids);
DELETE FROM firewall_rule WHERE rule_id IN (SELECT * FROM rule_ids);
DELETE FROM tr_firewall_settings WHERE tid = :tid;
DELETE FROM tr_firewall_evt WHERE rule_id IN (SELECT * FROM rule_ids);

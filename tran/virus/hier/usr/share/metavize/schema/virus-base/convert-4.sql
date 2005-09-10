UPDATE settings.tr_virus_settings SET trickle_percent = 90;

UPDATE settings.mimetype_rule SET live = false WHERE rule_id IN
  (SELECT m.rule_id FROM tr_virus_vs_mt v, mimetype_rule m
     WHERE v.rule_id = m.rule_id AND m.live AND m.mime_type = 'application/*');

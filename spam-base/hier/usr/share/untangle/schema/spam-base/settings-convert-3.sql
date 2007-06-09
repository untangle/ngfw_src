-- settings convert for release 4.0

-- add throttle flag , false for everything except smtp inbound on spamassassin
ALTER TABLE tr_spam_smtp_config ADD COLUMN throttle BOOL;
UPDATE tr_spam_smtp_config SET throttle = false;
UPDATE tr_spam_smtp_config SET throttle = true FROM tr_spam_settings, transform_persistent_state WHERE config_id = smtp_inbound and tr_spam_settings.tid = transform_persistent_state.tid and name = 'spamassassin-transform';
ALTER TABLE tr_spam_smtp_config ALTER COLUMN throttle SET NOT NULL;

-- add second throttle
ALTER TABLE tr_spam_smtp_config ADD COLUMN throttle_sec INT4;
UPDATE tr_spam_smtp_config SET throttle_sec = 15;
ALTER TABLE tr_spam_smtp_config ALTER COLUMN throttle_sec SET NOT NULL;



-- add missing indices
CREATE INDEX tr_spam_evt_mid_idx
    ON events.tr_spam_evt (msg_id);
CREATE INDEX tr_spam_evt_smtp_mid_idx
    ON events.tr_spam_evt_smtp (msg_id);

-- "notify both" action is no longer supported;
-- convert to "notify sender" action (for both spam and clamphish)
UPDATE settings.tr_spam_smtp_config
  SET notify_action = 'S' where notify_action = 'B';
-- "notify recipient" action is no longer supported;
-- convert to "notify neither" action (for both spam and clamphish)
UPDATE settings.tr_spam_smtp_config
  SET notify_action = 'N' where notify_action = 'R';

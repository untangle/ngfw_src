-- settings convert for release 3.1

-- "notify both" action is no longer supported;
-- convert to "notify sender" action (for both spam and clamphish)
UPDATE settings.tr_spam_smtp_config
  SET notify_action = 'S' where notify_action = 'B';
-- "notify recipient" action is no longer supported;
-- convert to "notify neither" action (for both spam and clamphish)
UPDATE settings.tr_spam_smtp_config
  SET notify_action = 'N' where notify_action = 'R';

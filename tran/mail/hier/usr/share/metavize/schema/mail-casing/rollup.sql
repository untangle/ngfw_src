--due to hibernate mapping/cascade issue,
--we must manually delete orphaned child data

--delete orphaned recipients
DELETE FROM settings.tr_mail_safels_recipient
  WHERE id NOT IN
    (SELECT DISTINCT recipient FROM settings.tr_mail_safels_settings);

--delete orphaned senders
DELETE FROM settings.tr_mail_safels_sender
  WHERE id NOT IN
    (SELECT DISTINCT sender FROM settings.tr_mail_safels_settings);

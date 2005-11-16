-- XXX for  now, we have to  tie this to  spam because we do not have
-- timestamps here

DELETE FROM tr_mail_message_info
  WHERE time_stamp < (:cutoff)::timestamp;

DELETE FROM tr_mail_message_info_addr
  WHERE msg_id NOT IN (SELECT id FROM tr_mail_message_info);

DELETE FROM tr_mail_message_stats
  WHERE msg_id NOT IN (SELECT id FROM tr_mail_message_info);

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

DELETE FROM tr_mail_message_info
  WHERE time_stamp < (:cutoff)::timestamp;

DELETE FROM tr_mail_message_info_addr
  WHERE NOT EXISTS
      (SELECT 1 FROM tr_mail_message_info
          WHERE tr_mail_message_info_addr.msg_id = id);

DELETE FROM tr_mail_message_stats
  WHERE NOT EXISTS
      (SELECT 1 FROM tr_mail_message_info
          WHERE tr_mail_message_stats.msg_id = id);

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

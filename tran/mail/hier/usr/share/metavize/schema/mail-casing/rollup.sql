-- XXX for  now, we have to  tie this to  spam because we do not have
-- timestamps here

DELETE FROM tr_mail_message_info
WHERE id IN (SELECT msg_id FROM tr_spam_evt_smtp
             WHERE time_stamp < (:cutoff)::timestamp)
    OR id IN (SELECT msg_id FROM tr_spam_evt
              WHERE time_stamp < (:cutoff)::timestamp);

DELETE FROM tr_mail_message_info_addr
WHERE msg_id NOT IN (SELECT id FROM tr_mail_message_info);

DELETE FROM tr_mail_message_stats
WHERE msg_id NOT IN (SELECT id FROM tr_mail_message_info);
